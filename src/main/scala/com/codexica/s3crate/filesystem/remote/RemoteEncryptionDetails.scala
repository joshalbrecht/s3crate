package com.codexica.s3crate.filesystem.remote

import play.api.libs.json.Json
import com.codexica.s3crate.utils.KeyDataFormat

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class RemoteEncryptionDetails(
   encodedKey: RemoteFileSystemTypes.KeyData,
   method: EncryptionMethod
 )

object RemoteEncryptionDetails {
  implicit val keyFormat = new KeyDataFormat()
  implicit val format = Json.format[RemoteEncryptionDetails]
}
