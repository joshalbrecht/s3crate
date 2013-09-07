package com.codexica.s3crate.filesystem.remote

import org.jets3t.service.impl.rest.httpclient.RestS3Service
import org.jets3t.service.model.{S3Object, S3Bucket}
import java.io._
import org.apache.commons.io.{IOUtils, FileUtils}

/**
 * A wrapper around the JetS3t interface to S3 for simplicity and testing
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class S3Interface(s3: RestS3Service, bucket: S3Bucket) {

  /**
   * @param prefix the path that must be a prefix of each object returned
   * @return A list of all objects at this path
   */
  def listObjects(prefix: String): List[S3Object] = {
    s3.listObjects(bucket.getName, prefix, null).toList
  }

  /**
   * Normal method of uploading to S3.
   *
   * @param file File to upload
   * @param location Path to upload it to
   * @param md5 The hash of the file contents
   * @return The uploaded object
   */
  def upload(file: File, location: String, md5: Array[Byte]): S3Object = {
    val obj = new S3Object(location)
    obj.setDataInputFile(file)
    obj.setMd5Hash(md5)
    s3.putObject(bucket, obj)
  }

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
  def multipartUpload(fileHashes: Map[File, Array[Byte]], location: String, completeMD5: Array[Byte]): S3Object = {
    //Multipart
    //UploadCallable.uploadInParts  //<- see for real implementation, using md5
    //weirdness: when uploading in multiple parts, if LESS than 5GB, must copy onto yourself, which regenerates the ETAG to be the MD5 hash, THEN we can verify that the upload worked correctly.
    //if greater than 5GB, can't do anything right now besides hope that the resulting concatenation didn't introduce errors on the AWS side (can only verify hashes for each part)
    throw new NotImplementedError()
  }

  /**
   * @param obj Download the data from this object
   * @param file And save it into this file
   */
  def download(obj: S3Object, file: File) {
    val input = new BufferedInputStream(obj.getDataInputStream)
    val output = new BufferedOutputStream(new FileOutputStream(file))
    IOUtils.copy(input, output)
    input.close()
    output.close()
  }
}
