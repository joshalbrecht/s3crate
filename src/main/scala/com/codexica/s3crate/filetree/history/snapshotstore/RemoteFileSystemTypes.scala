package com.codexica.s3crate.filetree.history.snapshotstore

import java.util.UUID
import play.api.libs.json._
import com.codexica.common.UUIDAliasFormat

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
object RemoteFileSystemTypes {
  type SnapshotId = UUID
  type S3Path = String

  implicit val pathFormat = new UUIDAliasFormat[SnapshotId]()
 }
