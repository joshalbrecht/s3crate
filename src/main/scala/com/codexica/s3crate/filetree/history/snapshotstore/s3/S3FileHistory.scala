package com.codexica.s3crate.filetree.history.snapshotstore.s3

import com.codexica.s3crate.filetree.history.{Compressor, FilePathState, FileTreeHistory}
import com.codexica.s3crate.filetree.history.snapshotstore.{RemoteFileSystemTypes, FileSnapshot}
import com.codexica.s3crate.filetree.{ReadableFileTree, FilePath, WritableFileTree}
import scala.concurrent.{ExecutionContext, Future}
import scala.Some
import com.codexica.common.FutureUtils
import com.jcabi.aspects.Loggable
import java.util.concurrent.TimeUnit

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

  @Loggable(value = Loggable.DEBUG, limit = 200, unit = TimeUnit.MILLISECONDS, prepend = true)
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
            //TODO: have to handle this eventually, but it's bad. Means that 2 things were writing at the same time. Currently thinking that the easiest way to handle this is just look at the Amazon metadata--whichever file was created earlier, use that one (in case of ties, use alphabetically first). That way things are deterministic
            //confirming that there are no cases where there are two identical previous snapshots, since that would mean that the actual correct lineage would be ambiguous
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

  @Loggable(value = Loggable.DEBUG, limit = 200, unit = TimeUnit.MILLISECONDS, prepend = true)
  def metadata(path: FilePath): Future[Option[FileSnapshot]] = Future {
    snapshotLock.synchronized {
      latestSnapshots.get(path).map(id => allSnapshots(id))
    }
  }

  @Loggable(value = Loggable.DEBUG, limit = 200, unit = TimeUnit.MILLISECONDS, prepend = true)
  def update(path: FilePath, fileTree: ReadableFileTree): Future[FileSnapshot] = {
    fileTree.metadata(path).flatMap(pathState => {
      store.saveBlob(() => {
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

  @Loggable(value = Loggable.DEBUG, limit = 200, unit = TimeUnit.MILLISECONDS, prepend = true)
  def delete(path: FilePath): Future[FileSnapshot] = throw new NotImplementedError()

  @Loggable(value = Loggable.DEBUG, limit = 200, unit = TimeUnit.MILLISECONDS, prepend = true)
  def readLatest(path: FilePath): Future[FileSnapshot] = throw new NotImplementedError()

  @Loggable(value = Loggable.DEBUG, limit = 200, unit = TimeUnit.MILLISECONDS, prepend = true)
  def download(snapshot: FileSnapshot, output: WritableFileTree): Future[FilePathState] = {
    throw new NotImplementedError()
  }

  @Loggable(value = Loggable.TRACE, limit = 200, unit = TimeUnit.MILLISECONDS, prepend = true)
  protected def previousVersion(path: FilePath): Option[FileSnapshot] = {
    snapshotLock.synchronized {
      allSnapshots(latestSnapshots(path)).previous.map(id => allSnapshots(id))
    }
  }
}

protected[s3] object S3FileHistory {

  /**
   * This is the only way to create an S3FileHistory because it takes forever to boot up, so we return a future
   * instead, and that way the object is simpler because it is guaranteed to be initialized when other calls are made.
   *
   * @param ec The context in which futures should be executed
   * @return The fully initialized file history backed by S3
   */
  def initialize(store: S3SnapshotStore, ec: ExecutionContext): Future[S3FileHistory] = {
    implicit val context = ec
    val history = new S3FileHistory(store)
    val booted = history.init()
    booted.map(_ => history)
  }
}
