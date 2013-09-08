package com.codexica.s3crate.filetree

import org.joda.time.DateTime
import play.api.libs.json._
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
  class PosixPermissionFormat extends Format[PosixFilePermission] {
    def writes(o: PosixFilePermission) = JsString(o.toString)
    def reads(json: JsValue) = json match {
      case JsString(value) => JsSuccess(PosixFilePermission.valueOf(value))
      case _ => JsError("Expected a PosixFilePermission")
    }
  }
  implicit val permissionFormat = new PosixPermissionFormat()
  implicit val format = Json.format[FileMetaData]
}