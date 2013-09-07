package com.codexica.s3crate.filesystem

import play.api.libs.json.Json

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class FilePath(path: String)

object FilePath {
  implicit val format = Json.format[FilePath]
}
