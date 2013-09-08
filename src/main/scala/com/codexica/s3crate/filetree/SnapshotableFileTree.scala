package com.codexica.s3crate.filetree

import scala.concurrent.Future
import java.io.InputStream

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
trait SnapshotableFileTree {
  def delta(path: FilePath, snapshot: FileSystemSnapshot, startingHash: ContentHash): Future[InputStream]
  def snapshot(): Future[FileSystemSnapshot]
  def listSnapshots(): Future[List[FileSystemSnapshot]]
  def removeSnapshot(snapshot: FileSystemSnapshot): Future[Unit]
}
