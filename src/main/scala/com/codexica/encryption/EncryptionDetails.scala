package com.codexica.encryption

import play.api.libs.json.Json
import com.codexica.s3crate.filesystem.remote.RemoteFileSystemTypes

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class EncryptionDetails(
   encodedKey: RemoteFileSystemTypes.KeyData,
   method: EncryptionMethod
 )

object EncryptionDetails {
  implicit val keyFormat = new KeyDataFormat()
  implicit val format = Json.format[EncryptionDetails]
}
