package com.codexica.s3crate.filesystem

import org.joda.time.DateTime
import play.api.libs.json.Json

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class FileMetaData(
  modifiedAt: DateTime,
  createdAt: DateTime,
  length: Long,
  osType: String,
  properties: Map[String, String]
)

object FileMetaData {
  implicit val format = Json.format[FileMetaData]
}