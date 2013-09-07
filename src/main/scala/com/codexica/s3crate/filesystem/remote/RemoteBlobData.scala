package com.codexica.s3crate.filesystem.remote

import com.codexica.s3crate.filesystem.ReadableFile
import play.api.libs.json.Json

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class RemoteBlobData(
   location: RemoteFileSystemTypes.S3Path,
   encryption: RemoteEncryptionDetails,
   isZipped: Boolean
 ) {

  def read(privateKey: Array[Byte]): ReadableFile = {
    //TODO: implement
    null
  }
}

object RemoteBlobData {
  implicit val format = Json.format[RemoteBlobData]
}
