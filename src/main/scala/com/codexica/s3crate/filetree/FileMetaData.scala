package com.codexica.s3crate.filetree

import org.joda.time.DateTime
import play.api.libs.json.Json
import java.nio.file.attribute.PosixFilePermission

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class FileMetaData(
  fileType: FilePathType,
  length: Long,
  modifiedAt: DateTime,
  owner: String,
  group: String,
  permissions: Set[PosixFilePermission],
  symLinkPath: Option[FilePath],
  isHidden: Boolean
)

object FileMetaData {
  implicit val format = Json.format[FileMetaData]
}