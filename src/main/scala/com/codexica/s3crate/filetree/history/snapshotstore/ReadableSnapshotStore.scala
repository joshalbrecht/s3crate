package com.codexica.s3crate.filetree.history.snapshotstore

import scala.concurrent.Future
import com.codexica.s3crate.filetree.{FilePath, WritableFileTree}

/**
 * Interface for listing and reading snapshots in the storage system.
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
trait ReadableSnapshotStore {

  /**
   * @return A Future that, when complete, will contain ALL known snapshots in the snapshot store. Note that this takes
   *         a really long time, and you should cache these locally, since they are immutable anyway.
   */
  def list(): Future[Set[RemoteFileSystemTypes.SnapshotId]]

  /**
   * Read all of the meta data about a particular version from the snapshot store
   *
   * @param id The version to read
   * @return A Future that will be completed with the metadata, when downloaded/decrypted, etc
   */
  def read(id: RemoteFileSystemTypes.SnapshotId): Future[FileSnapshot]

  /**
   * Download the data from the snapshot store into the file tree
   *
   * @param id The version to download
   * @param path The location where the file should be written in the file tree.
   * @param fileSystem The file tree into which data should be written
   * @return A Future that will be complete when the download is finish
   */
  def download(id: RemoteFileSystemTypes.SnapshotId, path: FilePath, fileSystem: WritableFileTree): Future[Unit]
}
