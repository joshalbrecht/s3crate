package com.codexica.s3crate.filetree.history.snapshotstore

import com.codexica.common.SafeInputStream
import com.codexica.s3crate.filetree.FilePath
import com.codexica.s3crate.filetree.history.FilePathState
import scala.concurrent.Future

/**
 * Interface for writing new snapshots to the storage system.
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
trait WritableSnapshotStore {

  /**
   * Saves binary data associated with a single snapshot to the snapshot store
   *
   * @param inputGenerator Creates streams for reading the data.
   * @return A Future that will complete with a DataBlob (description of where and how the data was saved) or throws one
   *         of the following errors: InaccessibleDataError
   */
  def saveBlob(inputGenerator: () => SafeInputStream): Future[DataBlob]

  /**
   * Saves all metadata for a single snapshot to the snapshot store
   *
   * @param path The path of the file
   * @param state The current metadata about that file
   * @param blob The already saved DataBlob information
   * @param previousVersion The id for the FileSnapshot of the most recent previous version of the file, if any
   * @return A Future that will complete with a FileSnapshot (all metadata about the file and version) or throws one
   *         of the following errors: InaccessibleDataError
   */
  def saveSnapshot(path: FilePath,
                   state: FilePathState,
                   blob: DataBlob,
                   previousVersion: Option[RemoteFileSystemTypes.SnapshotId]): Future[FileSnapshot]
}
