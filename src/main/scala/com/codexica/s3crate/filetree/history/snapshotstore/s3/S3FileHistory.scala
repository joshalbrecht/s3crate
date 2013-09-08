package com.codexica.s3crate.filetree.history.snapshotstore.s3

import com.codexica.s3crate.filetree.history.{FilePathState, FileTreeHistory}
import com.codexica.s3crate.filetree.history.snapshotstore.FileSnapshot
import com.codexica.s3crate.filetree.{ReadableFileTree, FilePath, WritableFileTree}
import scala.concurrent.{ExecutionContext, Future}
import com.codexica.s3crate.filesystem.remote.RemoteFileSystemTypes
import com.codexica.s3crate.FutureUtils
import org.xerial.snappy.SnappyInputStream
import com.codexica.encryption._
import scala.Some
import com.codexica.encryption.SimpleEncryption
import com.codexica.encryption.NoEncryption
import java.io.InputStream

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class S3FileHistory private (store: S3SnapshotStore)(implicit val ec: ExecutionContext) extends FileTreeHistory {

  //ratio of ( compressed size / original size ). If less than this ratio, it's worth it to encrypt
  val COMPRESSION_RATIO = 0.7
  //how/whether to encrypt the data at all
  val ENCRYPTION_METHOD: EncryptionMethod = SimpleEncryption()

  //lock for modifying either of the maps:
  private val snapshotLock: AnyRef = new Object()

  //mapping from FilePath to the most recent known version
  private val latestSnapshots = scala.collection.mutable.HashMap.empty[FilePath, RemoteFileSystemTypes.SnapshotId]

  //mapping from version to associated metadata
  private val allSnapshots = scala.collection.mutable.HashMap.empty[RemoteFileSystemTypes.SnapshotId, FileSnapshot]

  private def init(): Future[Unit] = {
    store.clear().flatMap(_ => {
      store.list().flatMap(snapshotIds => {
        FutureUtils.sequenceOrBailOut(snapshotIds.map(snapshotId => {
          store.read(snapshotId)
        }))
      })
    }).map(snapshotList => {
      snapshotLock.synchronized {
        snapshotList.foreach(snapshot => allSnapshots.put(snapshot.id, snapshot))
        snapshotList.groupBy(snapshot => snapshot.fileSnapshot.path).foreach({case (filePath, snapshots) => {
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
      Unit
    })
  }

  def metadata(path: FilePath): Future[Option[FileSnapshot]] = Future {
    snapshotLock.synchronized {
      latestSnapshots.get(path).map(id => allSnapshots(id))
    }
  }

  def update(path: FilePath, fileTree: ReadableFileTree): Future[FileSnapshot] = {
    fileTree.metadata(path).flatMap(pathState => {

      //get the input and output
      val output = store.saveBlob(path, pathState)
      val input = fileTree.read(path)

      //don't bother compressing unless we get at least a 30% savings in space
      //the purpose of this is to ignore compression for already encoded binaries (mp3, jpg, zips, etc)
      val numBytesToTryCompressing = 256 * 1024
      val shouldZip = compressionRatio(fileTree.read(path), numBytesToTryCompressing) < COMPRESSION_RATIO

      //if we need to zip, compress the data
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

      output.save(encryptedInput, shouldZip, encryptionDetails).flatMap(blob => {
        store.saveSnapshot(path, pathState, blob).map(snapshot => {
          //now safe to update the in-memory map
          snapshotLock.synchronized {
            allSnapshots.put(snapshot.id, snapshot)
            latestSnapshots.put(snapshot.fileSnapshot.path, snapshot.id)
          }
          snapshot
        })
      })
    })
  }

  def delete(path: FilePath): Future[FileSnapshot] = throw new NotImplementedError()

  def readLatest(path: FilePath): Future[FileSnapshot] = throw new NotImplementedError()

  def download(snapshot: FileSnapshot, output: WritableFileTree): Future[FilePathState] = throw new NotImplementedError()

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
}

object S3FileHistory {

  /**
   * This is the only way to create an S3FileHistory because it takes forever to boot up, so we return a future
   * instead, and that way the object is simpler because it is guaranteed to be initialized when other calls are made.
   *
   * @param ec The context in which futures should be executed
   * @return The fully initialized file history backed by S3
   */
  def initialize(ec: ExecutionContext): Future[S3FileHistory] = {
    implicit val context = ec
    val store = new S3FileHistory(new S3SnapshotStore())
    val booted = store.init()
    booted.map(_ => store)
  }
}
