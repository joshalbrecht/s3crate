package com.codexica.s3crate.filetree.history.snapshotstore.s3

import com.codexica.s3crate.filetree.history.snapshotstore._
import scala.concurrent.{ExecutionContext, Future}
import java.io.{FileOutputStream, BufferedOutputStream, File, InputStream}
import com.codexica.s3crate.filesystem.remote.RemoteFileSystemTypes
import com.codexica.s3crate.filetree.{WritableFileTree, FilePath}
import com.codexica.encryption.{Encryption, SimpleEncryption, EncryptionMethod}
import com.codexica.s3crate.{FutureUtils, Contexts}
import org.apache.commons.io.FileUtils
import java.util.UUID
import com.codexica.s3crate.filetree.history.FilePathState
import play.api.libs.json.Json
import org.jets3t.service.utils.ServiceUtils
import com.codexica.encryption.SimpleEncryption

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class S3SnapshotStore(s3: S3Interface, remotePrefix: String, ec: ExecutionContext) extends ReadableSnapshotStore with WritableSnapshotStore {
  implicit val context = ec

  //name of this program
  val APPLICATION_NAME = "s3crate"

  //default text encoding:
  val TEXT_ENCODING = "UTF-8"
  //the characters to use in random names
  val UUID_CHARACTER_SET = Set("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f")

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

  //TODO: these files should obviously be cached in that folder
  override def read(id: RemoteFileSystemTypes.SnapshotId): Future[FileSnapshot] = Future {
    val file = new File(metaDir, id.toString)
    s3.download(obj, file)
    val jsonData = new String(Encryption.publicDecrypt(FileUtils.readFileToByteArray(file), metaPrivateKey), TEXT_ENCODING)
    Json.parse(jsonData).as[FileSnapshot]
  }

  override def saveBlob(path: FilePath, state: FilePathState): BlobOutput = {
    new S3BlobOutput()
  }

  override def saveSnapshot(path: FilePath, state: FilePathState, blob: DataBlob): Future[FileSnapshot] = {
    //get previous version
    val previous = previousVersion(snapshot.path)

    //write out the data to the blob bucket if appropriate, otherwise use the existing blob data
    val blobData = snapshot.state match {
      //TODO: eventually handle the Changed() case separately, and calculate a diff to upload instead
      case Created() | Changed() => writeBlob(data)
      case Restored() | Deleted() => previous.get.blob
    }

    //create the remote snapshot meta data
    val remoteSnapshot = FileSnapshot(
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
  }

  def download(id: RemoteFileSystemTypes.SnapshotId, path: FilePath, fileSystem: WritableFileTree): Future[Unit] = throw new NotImplementedError()

  private def getPersistentLocalFolder(subpath: String): File = {
    val folder = new File(FileUtils.getUserDirectory, "." + APPLICATION_NAME + "/" + subpath)
    FileUtils.forceMkdir(folder)
    folder
  }
}
