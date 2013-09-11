package com.codexica.s3crate.filetree.history.snapshotstore.s3

import org.jets3t.service.impl.rest.httpclient.RestS3Service
import org.jets3t.service.model.{S3Object, S3Bucket}
import java.io.{FileOutputStream, BufferedOutputStream, BufferedInputStream, File}
import org.apache.commons.io.IOUtils

/**
 * A wrapper around the JetS3t interface to S3 for simplicity and testing
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
trait S3Interface {

  /**
   * @param prefix the path that must be a prefix of each object returned
   * @return A list of all objects at this path
   */
  def listObjects(prefix: String): List[S3Object]

  /**
   * Normal method of uploading to S3.
   *
   * @param file File to upload
   * @param location Path to upload it to
   * @param md5 The hash of the file contents
   * @return The uploaded object
   */
  def upload(file: File, location: String, md5: Array[Byte]): S3Object

  /**
   * Given a set of (file, md5 hash) pairs, upload all of them to S3 (in parallel?) and verify that each upload has
   * the correct MD5 hash.
   *
   * If the resulting file is LESS than 5GB, then copy the file onto itself in S3 to regenerate the ETAG, which should
   * then be equal to the complete MD5.
   *
   * @param fileHashes The set of file/hashes to upload
   * @param location Path to upload to
   * @param completeMD5 The MD5 hash for the entire object (concatenation of all files)
   * @return The uploaded object
   */
  def multipartUpload(fileHashes: List[(File, Array[Byte])], location: String, completeMD5: Array[Byte]): S3Object

  /**
   * @param obj Download the data from this object
   * @param file And save it into this file
   */
  def download(obj: S3Object, file: File)

  /**
   * @param path Download the data from this path
   * @param file And save it into this file
   */
  def download(path: String, file: File)
}

