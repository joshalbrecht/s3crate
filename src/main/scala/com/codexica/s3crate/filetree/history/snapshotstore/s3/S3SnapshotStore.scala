package com.codexica.s3crate.filetree.history.snapshotstore.s3

import com.codexica.s3crate.filetree.history.snapshotstore._
import scala.concurrent.{ExecutionContext, Future}
import java.io.{FileOutputStream, BufferedOutputStream, File, InputStream}
import com.codexica.s3crate.filetree.{WritableFileTree, FilePath}
import com.codexica.encryption._
import com.codexica.s3crate.{FutureUtils, Contexts}
import org.apache.commons.io.FileUtils
import java.util.UUID
import com.codexica.s3crate.filetree.history.FilePathState
import play.api.libs.json.Json
import org.jets3t.service.utils.ServiceUtils
import org.xerial.snappy.SnappyInputStream
import com.codexica.encryption.SimpleEncryption
import com.codexica.encryption.NoEncryption
import scala.util.control.NonFatal

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class S3SnapshotStore(s3: S3Interface, remotePrefix: String, ec: ExecutionContext, metaKeys: KeyPair, blobKeys: KeyPair) extends ReadableSnapshotStore with WritableSnapshotStore {
  implicit val context = ec

  //name of this program
  val APPLICATION_NAME = "s3crate"
  //default text encoding:
  val TEXT_ENCODING = "UTF-8"
  //the characters to use in random names
  val UUID_CHARACTER_SET = Set("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f")
  //ratio of ( compressed size / original size ). If less than this ratio, it's worth it to encrypt
  val COMPRESSION_RATIO = 0.7
  //how/whether to encrypt the data at all
  val ENCRYPTION_METHOD: EncryptionMethod = SimpleEncryption()

  private val metaFolder = (remotePrefix.split("/").filter(_ != "").toList ::: List("meta")).mkString("/")
  private val blobFolder = (remotePrefix.split("/").filter(_ != "").toList ::: List("blob")).mkString("/")
  private val metaDir = getPersistentLocalFolder(metaFolder)
  private val uploadDir = getPersistentLocalFolder(blobFolder)

  /**
   * @return Future will be complete when all of the older files were deleted
   */
  def clear(): Future[Unit] = Future {
    uploadDir.listFiles().foreach(file => assert(file.delete()))
  }

  override def list(): Future[List[RemoteFileSystemTypes.SnapshotId]] = {
    //generate a bunch of prefixes, and list each of those in parallel. The reason for this is that it makes listing
    //large buckets parallel, and thus much faster.
    val prefixes = UUID_CHARACTER_SET.flatMap(char1 => {
      UUID_CHARACTER_SET.map(char2 => char1 + char2)
    })

    //load all data
    Future.sequence(prefixes.map(prefix => {
      Future {
        s3.listObjects(metaFolder + prefix)
      }.flatMap(objects => {
        FutureUtils.sequenceOrBailOut(objects.map(obj => Future {
          val id: RemoteFileSystemTypes.SnapshotId = UUID.fromString(obj.getKey.split("/").last)
          id
        }))
      })
    }).toList).map(_.flatten)
  }

  /**
   * Files are cached locally because they are immutable once written to S3. If we ever fail to deserialize one, we
   * assume that it was not properly downloaded, delete it, and try again.
   */
  override def read(id: RemoteFileSystemTypes.SnapshotId): Future[FileSnapshot] = Future {
    val remotePath = metaFolder + "/" + id.toString
    val file = new File(metaDir, id.toString)
    if (file.exists()) {
      try {
        readMetaFile(file)
      } catch {
        case NonFatal(e) => {
          if (file.exists()) {
            assert(file.delete())
          }
          s3.download(remotePath, file)
          readMetaFile(file)
        }
      }
    } else {
      s3.download(remotePath, file)
      readMetaFile(file)
    }
  }

  override def saveBlob(path: FilePath, state: FilePathState, inputGenerator: () => InputStream): Future[DataBlob] = {

    //don't bother compressing unless we get at least a 30% savings in space
    //the purpose of this is to ignore compression for already encoded binaries (mp3, jpg, zips, etc)
    val numBytesToTryCompressing = 256 * 1024
    val shouldZip = compressionRatio(inputGenerator(), numBytesToTryCompressing) < COMPRESSION_RATIO

    //if we need to zip, compress the data
    val input = inputGenerator()
    val compressedStream = shouldZip match {
      case true => new SnappyInputStream(input)
      case false => input
    }

    //check our current encryption policy and encrypt as necessary:
    val encryption = ENCRYPTION_METHOD

    val key = encryption match {
      case NoEncryption() => new Array[Byte](0)
      case SimpleEncryption() => randomBlobKey()
    }

    val encryptedInput = encryption match {
      case NoEncryption() => compressedStream
      case SimpleEncryption() => Encryption.encryptStream(key, compressedStream)
    }

    //encrypt the key
    val encryptedKey = encryptKey(key).toList

    //ServiceUtils.toBase64
    val encryptionDetails = EncryptionDetails(encryptedKey, encryption)

    new S3BlobOutput(s3, ec).save(encryptedInput).map(blobLocation => {
      DataBlob(blobLocation, encryptionDetails, shouldZip)
    })
  }

  override def saveSnapshot(path: FilePath, state: FilePathState, blob: DataBlob, previous: Option[RemoteFileSystemTypes.SnapshotId]): Future[FileSnapshot] = Future {

    //create the remote snapshot meta data
    val remoteSnapshot = FileSnapshot(
      UUID.randomUUID(),
      blob,
      state,
      previous
    )

    //serialize the snapshot
    val jsonSnapshot = Json.stringify(Json.toJson(remoteSnapshot)).getBytes(TEXT_ENCODING)

    //encrypt the snapshot
    val encryptedSnapshot = ENCRYPTION_METHOD match {
      case NoEncryption() => jsonSnapshot
      case SimpleEncryption() => Encryption.publicEncrypt(jsonSnapshot, metaKeys.pub)
    }

    //write it out to the meta bucket
    val file = File.createTempFile(remoteSnapshot.id.toString, ".meta")
    val output = new BufferedOutputStream(new FileOutputStream(file))
    output.write(encryptedSnapshot)
    output.close()
    val hash = ServiceUtils.computeMD5Hash(encryptedSnapshot)
    val location = getMetaLocation(remoteSnapshot)
    s3.upload(file, location, hash)
    FileUtils.moveFileToDirectory(file, metaDir, true)
    remoteSnapshot
  }

  def download(id: RemoteFileSystemTypes.SnapshotId, path: FilePath, fileSystem: WritableFileTree): Future[Unit] = throw new NotImplementedError()

  private def getPersistentLocalFolder(subpath: String): File = {
    val folder = new File(FileUtils.getUserDirectory, "." + APPLICATION_NAME + "/" + subpath)
    FileUtils.forceMkdir(folder)
    folder
  }

  //TODO: be sure to close the stream
  private def compressionRatio(stream: InputStream, numBytes: Int): Double = {
    //    val bytes = new Array[Byte](numBytes)
    //    val numBytesRead = stream.read(bytes)
    //    //TODO:  use snappy to compress and compare with the number of bytes read
    //    0.0
    throw new NotImplementedError()
  }

  private def randomBlobKey(): Array[Byte] = {
    throw new NotImplementedError()
  }

  //encode the key using the public key so that only someone with the private key can decrypt the contents
  private def encryptKey(blobKey: Array[Byte]): Array[Byte] = {
    throw new NotImplementedError()
  }

  private def getMetaLocation(snapshot: FileSnapshot): String = {
    throw new NotImplementedError()
  }

  private def readMetaFile(file: File): FileSnapshot = {
    val jsonData = new String(Encryption.publicDecrypt(FileUtils.readFileToByteArray(file), metaKeys.priv), TEXT_ENCODING)
    Json.parse(jsonData).as[FileSnapshot]
  }
}
