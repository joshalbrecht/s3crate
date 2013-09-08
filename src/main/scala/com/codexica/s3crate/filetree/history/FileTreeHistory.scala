package com.codexica.s3crate.filetree.history

import scala.concurrent.Future
import com.codexica.s3crate.filetree.{FilePath, WritableFileTree, ReadableFileTree}
import com.codexica.s3crate.filetree.history.snapshotstore.FileSnapshot

/**
 * Stores all data, metadata, and events related to a given file tree
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
trait FileTreeHistory {
  def metadata(path: FilePath): Future[Option[FileSnapshot]]
  def update(path: FilePath, fileTree: ReadableFileTree): Future[FileSnapshot]
  def delete(path: FilePath): Future[FileSnapshot]
  def readLatest(path: FilePath): Future[FileSnapshot]
  def download(snapshot: FileSnapshot, output: WritableFileTree): Future[FilePathState]
}
