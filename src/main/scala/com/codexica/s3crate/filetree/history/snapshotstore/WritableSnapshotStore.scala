package com.codexica.s3crate.filetree.history.snapshotstore

import scala.concurrent.Future
import java.io.InputStream
import com.codexica.s3crate.filetree.history.snapshotstore.{DataBlob, FileSnapshot}
import com.codexica.s3crate.filetree.history.FilePathState
import com.codexica.s3crate.filetree.FilePath

/**
 * Interface for writing new snapshots to the storage system.
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
trait WritableSnapshotStore {
  def saveBlob(path: FilePath, state: FilePathState): BlobOutput
  def saveSnapshot(path: FilePath, state: FilePathState, blob: DataBlob): Future[FileSnapshot]
}
