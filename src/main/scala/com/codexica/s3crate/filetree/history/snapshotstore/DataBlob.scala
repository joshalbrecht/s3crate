package com.codexica.s3crate.filetree.history.snapshotstore

import play.api.libs.json.Json
import RemoteFileSystemTypes
import com.codexica.encryption.EncryptionDetails

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class DataBlob(
   location: RemoteFileSystemTypes.S3Path,
   encryption: EncryptionDetails,
   isZipped: Boolean
 )

object DataBlob {
  implicit val format = Json.format[DataBlob]
}
