package com.codexica.s3crate.filetree.history.snapshotstore.s3

import com.codexica.s3crate.filetree.history.{Compressor, FilePathState, FileTreeHistory}
import com.codexica.s3crate.filetree.history.snapshotstore.{RemoteFileSystemTypes, FileSnapshot}
import com.codexica.s3crate.filetree.{ReadableFileTree, FilePath, WritableFileTree}
import scala.concurrent.{ExecutionContext, Future}
import scala.Some
import com.codexica.common.FutureUtils

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
protected[s3] class S3FileHistory private(store: S3SnapshotStore)(implicit val ec: ExecutionContext)
  extends FileTreeHistory {

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
        snapshotList.groupBy(snapshot => snapshot.fileSnapshot.path).foreach({
          case (filePath, snapshots) => {
            val snapshotList = snapshots.toList
            val linkedSnapshots = snapshotList.map(snapshot => {
              (snapshot.previous, snapshot.id)
            }).toMap
            //TODO: have to handle this eventually, but it's bad. Means that 2 things were writing at the same time
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
          }
        })
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
      store.saveBlob(path, pathState, () => {
        fileTree.read(path)
      }).flatMap(blob => {
        //get previous version
        val previous = previousVersion(path).map(_.id)
        store.saveSnapshot(path, pathState, blob, previous).map(snapshot => {
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

  def download(snapshot: FileSnapshot, output: WritableFileTree): Future[FilePathState] = {
    throw new NotImplementedError()
  }

  protected def previousVersion(path: FilePath): Option[FileSnapshot] = {
    throw new NotImplementedError()
  }
}

//TODO: make this protected again
object S3FileHistory {

  /**
   * This is the only way to create an S3FileHistory because it takes forever to boot up, so we return a future
   * instead, and that way the object is simpler because it is guaranteed to be initialized when other calls are made.
   *
   * @param ec The context in which futures should be executed
   * @return The fully initialized file history backed by S3
   */
  def initialize(ec: ExecutionContext, remotePrefix: String, s3: S3Interface): Future[S3FileHistory] = {
    implicit val context = ec
    //TODO:  create the crypto parameters
    val store = new S3FileHistory(new S3SnapshotStore(s3, remotePrefix, ec, new Compressor(), None, None, null))
    val booted = store.init()
    booted.map(_ => store)
  }
}
