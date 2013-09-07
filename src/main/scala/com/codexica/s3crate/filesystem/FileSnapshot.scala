package com.codexica.s3crate.filesystem

import play.api.libs.json.Json

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class FileSnapshot(
  path: FilePath,
  state: SnapshotState,
  meta: FileMetaData
)

object FileSnapshot {
  implicit val format = Json.format[FileSnapshot]
}
