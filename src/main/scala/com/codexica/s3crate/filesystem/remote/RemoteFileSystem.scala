package com.codexica.s3crate.filesystem.remote

import java.io.{BufferedOutputStream, FileOutputStream, File, InputStream}
import scala.concurrent.Future
import java.util.UUID
import com.codexica.s3crate.utils.{FutureUtils, Encryption, Contexts}
import com.codexica.s3crate.filesystem._
import com.codexica.s3crate.filesystem.FilePath
import com.codexica.s3crate.filesystem.FileSnapshot
import scala.Some
import com.codexica.s3crate.filesystem.FilePathEvent
import org.xerial.snappy.SnappyInputStream
import java.security.MessageDigest
import scala.collection.mutable
import play.api.libs.json.Json
import org.apache.commons.io.FileUtils
import org.jets3t.service.utils.ServiceUtils

//TODO: would be nice for this to support resumption of previous multi-part uploads
/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class RemoteFileSystem(s3: S3Interface, remotePrefix: String, metaPublicKey: Array[Byte], metaPrivateKey: Array[Byte], blobPublicKey: Array[Byte], blobPrivateKey: Array[Byte]) extends FileSystem {

  //name of this program
  val APPLICATION_NAME = "s3crate"
  //file bigger than 128MB? Don't try to upload that as one big thing, it's going to take forever and fail.
  val MULTIPART_CUTOFF_BYTES = 128 * 1024 * 1024
  //ratio of ( compressed size / original size ). If less than this ratio, it's worth it to encrypt
  val COMPRESSION_RATIO = 0.7
  //how/whether to encrypt the data at all
  val ENCRYPTION_METHOD: EncryptionMethod = SimpleEncryption()
  //default text encoding:
  val TEXT_ENCODING = "UTF-8"
  //the characters to use in random names
  //TODO:  set this to the proper set of characters
  val RANDOM_CHARACTER_SET = Set("a", "b", "c")

  implicit val context = Contexts.s3Operations

  private val metaFolder = (remotePrefix.split("/").filter(_ != "").toList ::: List("meta")).mkString("/")
  private val blobFolder = (remotePrefix.split("/").filter(_ != "").toList ::: List("blob")).mkString("/")
  private val metaDir = getPersistentLocalFolder(metaFolder)
  private val uploadDir = getPersistentLocalFolder(blobFolder)

  //lock for modifying either of the maps:
  private val snapshotLock: AnyRef = new Object()

  //mapping from FilePath to the most recent known version
  private val latestSnapshots = scala.collection.mutable.HashMap.empty[FilePath, RemoteFileSystemTypes.SnapshotId]

  //mapping from version to associated metadata
  private val allSnapshots = scala.collection.mutable.HashMap.empty[RemoteFileSystemTypes.SnapshotId, RemoteSnapshot]

  /**
   * load everything from S3 into one big map of S3Snapshots (which contain FileSnapshots, among many other things)
   */
  override def start(): Future[Set[FilePathEvent]] = {
    //ensure that all of the older files were deleted
    uploadDir.listFiles().foreach(file => assert(file.delete()))

    //generate a bunch of prefixes, and list each of those in parallel. The reason for this is that it makes listing
    //large buckets parallel, and thus much faster.
    val prefixes = RANDOM_CHARACTER_SET.flatMap(char1 => {
      RANDOM_CHARACTER_SET.map(char2 => char1 + char2)
    })

    //load all data
    val listingTasks = prefixes.map(prefix => {
      Future {
        s3.listObjects(metaFolder + prefix)
      }.flatMap(objects => {
        FutureUtils.sequenceOrBailOut(objects.map(obj => Future {
          val file = new File(metaDir, obj.getKey.split("/").last)
          s3.download(obj, file)
          file
        }))
      })
    }).toList

    FutureUtils.sequenceOrBailOut(listingTasks).map(listOfFileLists => {
      val events = listOfFileLists.flatten.toSet[File].par.map(file => {
        val jsonData = new String(Encryption.publicDecrypt(FileUtils.readFileToByteArray(file), metaPrivateKey), TEXT_ENCODING)
        val snapshot = Json.parse(jsonData).as[RemoteSnapshot]
        snapshotLock.synchronized {
          allSnapshots.put(snapshot.id, snapshot)
        }
        FilePathEvent(snapshot.fileSnapshot.path, snapshot.fileSnapshot.meta.modifiedAt)
      }).seq
      snapshotLock.synchronized {
        allSnapshots.values.groupBy(snapshot => snapshot.fileSnapshot.path).foreach({case (filePath, snapshots) => {
          val snapshotList = snapshots.toList
          val linkedSnapshots = snapshotList.map(snapshot => {
            (snapshot.previous, snapshot.id)
          }).toMap
          //TODO: we'll have to handle this eventually, but it's bad. Means that two things were writing at the same time
          //confirming that there are no cases where there are two identical previous snapshots, would be ambiguous
          assert(snapshotList.size == linkedSnapshots.size)
          var curSnapshotId = linkedSnapshots(None)
          var iterationNum = 0
          var isDone = false
          while (!isDone) {
            linkedSnapshots.get(Option(curSnapshotId)) match {
              case None => isDone = true
              case Some(x) => curSnapshotId = x
            }
            iterationNum += 1
            if (iterationNum > snapshotList.size) {
              throw new RuntimeException("Cycle detected :( " + linkedSnapshots)
            }
          }
          latestSnapshots.put(filePath, curSnapshotId)
        }})
      }
      events
    })
  }

  /**
   * Just pull it out of the map. Nearly instantaneous
   */
  override def snapshot(path: FilePath): Future[Option[FileSnapshot]] = Future {
    snapshotLock.synchronized {
      latestSnapshots.get(path) match {
        case None => Option.empty[FileSnapshot]
        case Some(id) => allSnapshots.get(id).map(_.fileSnapshot)
      }
    }
  }

  override def write(data: ReadableFile, snapshot: FileSnapshot): Future[Unit] = Future {

    //get previous version
    val previous = previousVersion(snapshot.path)

    //write out the data to the blob bucket if appropriate, otherwise use the existing blob data
    val blobData = snapshot.state match {
      //TODO: eventually handle the Changed() case separately, and calculate a diff to upload instead
      case Created() | Changed() => writeBlob(data)
      case Restored() | Deleted() => previous.get.blob
    }

    //create the remote snapshot meta data
    val remoteSnapshot = RemoteSnapshot(
      UUID.randomUUID(),
      blobData,
      snapshot,
      previous.map(_.id)
    )

    //serialize the snapshot
    val jsonSnapshot = Json.stringify(Json.toJson(remoteSnapshot)).getBytes(TEXT_ENCODING)

    //encrypt the snapshot
    val encryptedSnapshot = ENCRYPTION_METHOD match {
      case NoEncryption() => jsonSnapshot
      case SimpleEncryption() => Encryption.publicEncrypt(jsonSnapshot, metaPublicKey)
    }

    //write it out to the meta bucket
    val file = makeUploadDirectoryFile()
    val output = new BufferedOutputStream(new FileOutputStream(file))
    output.write(encryptedSnapshot)
    output.close()
    val hash = ServiceUtils.computeMD5Hash(encryptedSnapshot)
    val location = getMetaLocation(remoteSnapshot)
    s3.upload(file, location, hash)
    FileUtils.moveFileToDirectory(file, metaDir, true)

    //now safe to update the in-memory map
    snapshotLock.synchronized {
      allSnapshots.put(remoteSnapshot.id, remoteSnapshot)
      latestSnapshots.put(snapshot.path, remoteSnapshot.id)
    }

    Unit
  }

  override def read(path: FilePath): ReadableFile = {
    //must have provided the read key as an extra argument (otherwise write only)
    val snapshot = snapshotLock.synchronized {
      allSnapshots(latestSnapshots(path))
    }
    snapshot.blob.read(blobPrivateKey)
  }

  /**
   * Writes data to one or more (possibly encrypted) files (as determined by chunkSize) and calculates the MD5 for each
   * and for the entire thing
   *
   * @param data The data to write out
   * @param encryption The encryption method. This function will generate any necessary keys.
   * @param chunkSize The maximum amount of data to upload in one part.
   * @return (The encryption details, a map from file -> the hash of the file, and a hash of all contents that were written out
   * @throws Exception if there is no data.
   */
  private def writeBlobToFiles(data: ReadableFile, encryption: EncryptionMethod, chunkSize: Long): (RemoteEncryptionDetails, Map[File, Array[Byte]], Array[Byte]) = {
    assert(data.length > 0)

    val key = encryption match {
      case NoEncryption() => new Array[Byte](0).toList
      case SimpleEncryption() => randomBlobKey().toList
    }

    val encryptedInput = encryption match {
      case NoEncryption() => data.data()
      case SimpleEncryption() => Encryption.encryptStream(key.toArray, data.data())
    }

    var finishedWritingData = false
    var currentFile = makeUploadDirectoryFile()
    var currentOutputStream = new BufferedOutputStream(new FileOutputStream(currentFile))
    var currentMd5Accumulator: MessageDigest = MessageDigest.getInstance("MD5")
    //TODO: feels kind of silly that we hash everything twice for the first file part (eg, most files), but there's no
    //convenient way around it that I see. Perhaps we could clone it before calling digest...
    val completeMd5Accumulator: MessageDigest = MessageDigest.getInstance("MD5")
    val files = new mutable.HashMap[File, Array[Byte]]()
    val bytes = new Array[Byte](4096)
    var numBytesWritten = 0L
    while (!finishedWritingData) {
      val numBytesRead = encryptedInput.read(bytes)
      if (numBytesRead == -1) {
        finishedWritingData = true
      } else {
        val numBytesToWrite: Int = if (numBytesWritten + numBytesRead > chunkSize) {
          (chunkSize - numBytesWritten).toInt
        } else {
          numBytesRead
        }
        currentOutputStream.write(bytes, 0, numBytesToWrite)
        currentMd5Accumulator.update(bytes, 0, numBytesToWrite)
        completeMd5Accumulator.update(bytes, 0, numBytesToWrite)
        numBytesWritten += numBytesToWrite

        //roll over the file and write the rest
        if (numBytesRead > numBytesToWrite) {
          currentOutputStream.close()
          files += (currentFile -> currentMd5Accumulator.digest)
          currentFile = makeUploadDirectoryFile()
          currentOutputStream = new BufferedOutputStream(new FileOutputStream(currentFile))
          currentMd5Accumulator = MessageDigest.getInstance("MD5")
          numBytesWritten = 0L

          val remainingBytes = numBytesRead - numBytesToWrite
          currentOutputStream.write(bytes, numBytesToWrite, remainingBytes)
          currentMd5Accumulator.update(bytes, numBytesToWrite, remainingBytes)
          completeMd5Accumulator.update(bytes, numBytesToWrite, remainingBytes)
          numBytesWritten += remainingBytes
        }
      }
    }
    currentOutputStream.close()
    files += (currentFile -> currentMd5Accumulator.digest)

    //encrypt the key
    val encryptedKey = encryptKey(key.toArray).toList

    //ServiceUtils.toBase64
    (RemoteEncryptionDetails(encryptedKey, encryption), files.toMap, completeMd5Accumulator.digest())
  }

  /**
   * Write all data out to disk, possibly zipping and then possibly encrypting, then upload the entire thing, and create
   * the metadata that represents this file data.
   *
   * @param data the data to write out
   * @throws a bunch of exceptions from interacting with ReadableFile, which may be concurrently modified by the user
   * @return the associated metadata
   */
  private def writeBlob(data: ReadableFile): RemoteBlobData = {
    //don't bother compressing unless we get at least a 30% savings in space
    //the purpose of this is to ignore compression for already encoded binaries (mp3, jpg, zips, etc)
    val numBytesToTryCompressing = 256 * 1024
    val shouldZip = compressionRatio(data.data(), numBytesToTryCompressing) < COMPRESSION_RATIO

    //store blob in a random location, really doesn't matter
    val blobLocation = randomBlobLocation()

    //if we need to zip, compress the data
    val compressedStream = shouldZip match {
      case true => new SnappyInputStream(data.data())
      case false => data.data()
    }

    //write out all of the data for upload and calculate the md5 for each piece
    val (encryptionDetails, fileHashes, completeMd5) = writeBlobToFiles(data, ENCRYPTION_METHOD, MULTIPART_CUTOFF_BYTES)
    //if this file is huge:
    if (fileHashes.size > 1) {
      //let the multi-part uploader take care of the rest
      s3.multipartUpload(fileHashes, blobLocation, completeMd5)
    //otherwise it should be pretty easy. Just upload directly
    } else {
      //have the normal uploader take care of it
      assert(completeMd5 == fileHashes.values.head)
      s3.upload(fileHashes.keys.head, blobLocation, fileHashes.values.head)
    }

    //clean up the leftover files:
    fileHashes.keys.foreach((file: File) => assert(file.delete()))

    RemoteBlobData(blobLocation, encryptionDetails, shouldZip)
  }

  private def compressionRatio(stream: InputStream, numBytes: Int): Double = {
//    val bytes = new Array[Byte](numBytes)
//    val numBytesRead = stream.read(bytes)
//    //TODO:  use snappy to compress and compare with the number of bytes read
//    0.0
    throw new NotImplementedError()
  }

  private def getMetaLocation(snapshot: RemoteSnapshot): String = {
    throw new NotImplementedError()
  }

  private def randomBlobLocation(): String = {
    throw new NotImplementedError()
  }

  private def previousVersion(path: FilePath): Option[RemoteSnapshot] = {
    throw new NotImplementedError()
  }

  private def randomBlobKey(): Array[Byte] = {
    throw new NotImplementedError()
  }

  //encode the key using the public key so that only someone with the private key can decrypt the contents
  private def encryptKey(blobKey: Array[Byte]): Array[Byte] = {
    throw new NotImplementedError()
  }

  private def makeUploadDirectoryFile(): File = {
    throw new NotImplementedError()
  }

  private def getPersistentLocalFolder(subpath: String): File = {
    val folder = new File(FileUtils.getUserDirectory, "." + APPLICATION_NAME + "/" + subpath)
    FileUtils.forceMkdir(folder)
    folder
  }
}
