package com.codexica.s3crate.filesystem.remote

import java.util.UUID
import play.api.libs.json._

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
object RemoteFileSystemTypes {
  type SnapshotId = UUID
  type S3Path = String
  type KeyData = List[Byte]

  class SnapshotIdFormat extends Format[SnapshotId] {
    def writes(o: SnapshotId) = JsString(o.toString)
    def reads(json: JsValue) = json match {
      case JsString(value) => JsSuccess(UUID.fromString(value))
      case _ => JsError("Expected a UUID")
    }
  }

  implicit val pathFormat = new SnapshotIdFormat()
 }
