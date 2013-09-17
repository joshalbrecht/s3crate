package com.codexica.s3crate.filetree.history.snapshotstore.s3

import org.jets3t.service.model.S3Object
import java.io._
import com.codexica.common.SafeInputStream

/**
 * A wrapper around the JetS3t interface to S3 for simplicity and testing
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
trait S3Interface {

  /**
   * @param prefix the path that must be a prefix of each object returned
   * @throws InaccessibleDataError if we could not connect to s3 and list the buckets properly
   * @return A list of all objects at this path
   */
  def listObjects(prefix: String): Set[S3Object]

  /**
   * Upload all of the data to the storage location, splitting as necessary
   * @param data The data to be written. This stream will definitely be closed, regardless of whether an exception is
   *             thrown or the function returns normally.
   * @param blobLocation where the data should be stored
   * @param maxPartSize The maximum amount of data to be written in a single stream to the storage provider. If multiple
   *                    parts must be uploaded, they will be merged after being uploaded.
   * @param uploadDir where to write the file(s) before uploading
   * @throws InaccessibleDataError if the data could not be read or written for any reason
   * @throws AssertionError if the checksums don't match
   * @return The object that was created
   */
  def save(data: SafeInputStream, blobLocation: String, uploadDir: File, maxPartSize: Long): S3Object

  /**
   * @param path Download the data from this path
   * @param file And save it into this file
   * @throws InaccessibleDataError if there were problems with S3 or the file
   * @throws IllegalArgumentException if the path does not exist
   */
  def download(path: String, file: File)

  /**
   * @param prefix Everything in the bucket with a matching prefix will be deleted
   * @throws InaccessibleDataError if there were problems with S3
   */
  def delete(prefix: String)
}

