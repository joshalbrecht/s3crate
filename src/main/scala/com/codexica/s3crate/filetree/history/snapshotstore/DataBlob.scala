package com.codexica.s3crate.filetree.history.snapshotstore

import play.api.libs.json.Json
import com.codexica.encryption.EncryptionDetails
import com.codexica.s3crate.filetree.history.CompressionMethod

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class DataBlob(
   location: RemoteFileSystemTypes.S3Path,
   encryption: Option[EncryptionDetails],
   compressionMethod: CompressionMethod
 )

object DataBlob {
  implicit val format = Json.format[DataBlob]
}
