package com.codexica.s3crate.common.models

import com.codexica.s3crate.filesystem.ReadableFile
import play.api.libs.json.Json
import com.codexica.s3crate.filesystem.remote.{RemoteFileSystemTypes}

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class DataBlob(
   location: RemoteFileSystemTypes.S3Path,
   encryption: EncryptionDetails,
   isZipped: Boolean
 ) {

  def read(privateKey: Array[Byte]): ReadableFile = {
    //TODO: implement
    null
  }
}

object DataBlob {
  implicit val format = Json.format[DataBlob]
}
