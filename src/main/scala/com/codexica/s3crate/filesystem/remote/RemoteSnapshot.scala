package com.codexica.s3crate.filesystem.remote

import play.api.libs.json._
import play.api.libs.functional.syntax._
import com.codexica.s3crate.filesystem.FileSnapshot

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class RemoteSnapshot(
   id: RemoteFileSystemTypes.SnapshotId,
   blob: RemoteBlobData,
   fileSnapshot: FileSnapshot,
   previous: Option[RemoteFileSystemTypes.SnapshotId],
   version: Int = 1
 )

object RemoteSnapshot {
  import RemoteFileSystemTypes._
  implicit val format = Json.format[RemoteSnapshot]
}
