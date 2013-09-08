package com.codexica.s3crate.filetree.history

import play.api.libs.json.Json
import com.codexica.s3crate.filetree.{FileMetaData, FilePath}

/**
 * Represents the states that can happen to data at a particular path. The life of a file is simply a sequence of these
 * objects over time, paired with whatever data was in the file at that particular time
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class FilePathState(
  path: FilePath,
  exists: Boolean,
  meta: Option[FileMetaData]
)

object FilePathState {
  implicit val format = Json.format[FilePathState]
}
