package com.codexica.s3crate.filetree.history.snapshotstore

import scala.concurrent.Future
import java.io.InputStream
import com.codexica.s3crate.filetree.history.FilePathState
import com.codexica.s3crate.filetree.FilePath
import com.codexica.s3crate.filesystem.remote.RemoteFileSystemTypes

/**
 * Interface for writing new snapshots to the storage system.
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
trait WritableSnapshotStore {
  def saveBlob(path: FilePath, state: FilePathState, inputGenerator: () => InputStream): Future[DataBlob]
  def saveSnapshot(path: FilePath, state: FilePathState, blob: DataBlob, previousVersion: Option[RemoteFileSystemTypes.SnapshotId]): Future[FileSnapshot]
}
