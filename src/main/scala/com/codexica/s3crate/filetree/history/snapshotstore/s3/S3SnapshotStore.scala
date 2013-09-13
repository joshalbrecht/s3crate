package com.codexica.s3crate.filetree.history.snapshotstore.s3

import com.codexica.s3crate.filetree.history.snapshotstore._
import scala.concurrent.{ExecutionContext, Future}
import java.io._
import com.codexica.s3crate.filetree.{WritableFileTree, FilePath}
import org.apache.commons.io.FileUtils
import java.util.UUID
import com.codexica.s3crate.filetree.history.{AsymmetricEncryptor, Compressor, SymmetricEncryptor, FilePathState}
import play.api.libs.json.Json
import scala.util.control.NonFatal
import com.codexica.common.{SafeInputStream, FutureUtils}

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
protected[s3] class S3SnapshotStore(s3: S3Interface, remotePrefix: String, ec: ExecutionContext, compressor: Compressor,
                        metaEncryptor: AsymmetricEncryptor, blobEncryptor: SymmetricEncryptor)
  extends ReadableSnapshotStore with WritableSnapshotStore {

  implicit val context = ec

  //name of this program
  val APPLICATION_NAME = "s3crate"
  //default text encoding:
  val TEXT_ENCODING = "UTF-8"
  //the characters to use in random names
  val UUID_CHARACTER_SET = Set("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f")
  //file bigger than 128MB? Don't try to upload that as one big thing, it's going to take forever and fail.
  private val MULTIPART_CUTOFF_BYTES = 128 * 1024 * 1024

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

  override def list(): Future[Set[RemoteFileSystemTypes.SnapshotId]] = {
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
    }).toSet).map(_.flatten)
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

  override def saveBlob(path: FilePath, state: FilePathState, inputGenerator: () => SafeInputStream): Future[DataBlob] = Future {
    val (compressionMethod, compressedStream) = compressor.compress(inputGenerator)
    val (encryptionDetails, encryptedInput) = blobEncryptor.encrypt(compressedStream)
    //store blob in a random location, really doesn't matter
    val blobLocation = randomBlobLocation()
    s3.save(encryptedInput, blobLocation, uploadDir, MULTIPART_CUTOFF_BYTES)
    DataBlob(blobLocation, encryptionDetails, compressionMethod)
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
    val encryptedSnapshot = metaEncryptor.encrypt(jsonSnapshot)

    //write it out to the meta bucket
    val file = File.createTempFile(remoteSnapshot.id.toString, ".meta")
    val output = new BufferedOutputStream(new FileOutputStream(file))
    output.write(encryptedSnapshot)
    output.close()
    val location = getMetaLocation(remoteSnapshot)
    s3.save(SafeInputStream.fromFile(file), location, uploadDir, Long.MaxValue)
    FileUtils.moveFileToDirectory(file, metaDir, true)
    remoteSnapshot
  }

  def download(id: RemoteFileSystemTypes.SnapshotId, path: FilePath, fileSystem: WritableFileTree): Future[Unit] = throw new NotImplementedError()

  private def getPersistentLocalFolder(subpath: String): File = {
    val folder = new File(FileUtils.getUserDirectory, "." + APPLICATION_NAME + "/" + subpath)
    FileUtils.forceMkdir(folder)
    folder
  }

  private def getMetaLocation(snapshot: FileSnapshot): String = {
    throw new NotImplementedError()
  }

  private def readMetaFile(file: File): FileSnapshot = {
    val jsonData = new String(metaEncryptor.decrypt(FileUtils.readFileToByteArray(file)), TEXT_ENCODING)
    Json.parse(jsonData).as[FileSnapshot]
  }

  private def randomBlobLocation(): String = {
    throw new NotImplementedError()
  }
}
