package com.codexica.s3crate.common.interfaces

import java.io.InputStream
import scala.concurrent.Future
import com.codexica.s3crate.common.models._
import com.codexica.s3crate.common.models.FileSystemSnapshot
import com.codexica.s3crate.common.models.ContentHash

/**
 * An interface to a filesystem, for reading data and metadata from paths
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
trait ReadableFileTree {
  def read(path: FilePath): Future[InputStream]
  def metadata(path: FilePath): Future[FilePathState]
  def delta(path: FilePath, snapshot: FileSystemSnapshot, startingHash: ContentHash): Future[InputStream]
  def snapshot(): Future[FileSystemSnapshot]
  def listSnapshots(): Future[List[FileSystemSnapshot]]
  def removeSnapshot(snapshot: FileSystemSnapshot): Future[Unit]
}
