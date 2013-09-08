package com.codexica.s3crate.filetree.history.snapshotstore

import play.api.libs.json._
import play.api.libs.functional.syntax._
import com.codexica.s3crate.filesystem.remote.RemoteFileSystemTypes
import com.codexica.s3crate.filetree.history.FilePathState
import com.codexica.s3crate.filetree.history.snapshotstore.DataBlob

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class FileSnapshot(
   id: RemoteFileSystemTypes.SnapshotId,
   blob: DataBlob,
   fileSnapshot: FilePathState,
   previous: Option[RemoteFileSystemTypes.SnapshotId],
   version: Int = 1
 )

object FileSnapshot {
  import RemoteFileSystemTypes._
  implicit val format = Json.format[FileSnapshot]
}
