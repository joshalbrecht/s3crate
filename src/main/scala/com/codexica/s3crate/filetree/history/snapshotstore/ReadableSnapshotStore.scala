package com.codexica.s3crate.filetree.history.snapshotstore

import scala.concurrent.Future
import com.codexica.s3crate.filetree.{FilePath, WritableFileTree}

/**
 * Interface for listing and reading snapshots in the storage system.
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
trait ReadableSnapshotStore {
  def list(): Future[List[RemoteFileSystemTypes.SnapshotId]]
  def read(id: RemoteFileSystemTypes.SnapshotId): Future[FileSnapshot]
  def download(id: RemoteFileSystemTypes.SnapshotId, path: FilePath, fileSystem: WritableFileTree): Future[Unit]
}
