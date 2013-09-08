package com.codexica.s3crate.common.interfaces

import com.codexica.s3crate.common.models.{FileSnapshot, FilePathState, FilePath}
import scala.concurrent.Future

/**
 * Stores all data, metadata, and events related to a given file tree
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
trait FileTreeHistory {
  def metadata(path: FilePath): Future[FileSnapshot]
  def update(path: FilePath, fileTree: ReadableFileTree): Future[FileSnapshot]
  def delete(path: FilePath): Future[FileSnapshot]
  def readLatest(path: FilePath): Future[FileSnapshot]
  def download(snapshot: FileSnapshot, output: WritableFileTree): Future[FilePathState]
}
