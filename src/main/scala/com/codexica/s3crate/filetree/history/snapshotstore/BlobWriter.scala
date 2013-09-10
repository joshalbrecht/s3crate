package com.codexica.s3crate.filetree.history.snapshotstore

import scala.concurrent.Future
import java.io.InputStream

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
abstract class BlobWriter {

  /**
   * Upload all of the data to the storage location, splitting as necessary
   * @param data The data to be written.
   * @param blobLocation where the data should be stored
   * @param maxPartSize The maximum amount of data to be written in a single stream to the storage provider. If multiple
   *                    parts must be uploaded, they will be merged after being uploaded.
   * @return The future may contain one of the following
   *         errors:  InaccessibleDataError
   */
  def save(data: InputStream, blobLocation: String, maxPartSize: Long): Future[Unit]
}
