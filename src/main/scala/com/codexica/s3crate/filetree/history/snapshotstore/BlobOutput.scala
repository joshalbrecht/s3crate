package com.codexica.s3crate.filetree.history.snapshotstore

import java.io.{InputStream, OutputStream}
import com.codexica.encryption.{EncryptionDetails, EncryptionMethod}
import scala.concurrent.Future

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
abstract class BlobOutput {
  def save(data: InputStream, wasZipped: Boolean, encryptionDetails: EncryptionDetails): Future[DataBlob]
}
