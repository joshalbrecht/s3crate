package com.codexica.s3crate.filetree.history.snapshotstore.s3

import org.jets3t.service.impl.rest.httpclient.RestS3Service
import org.jets3t.service.model.{S3Object, S3Bucket}
import java.io._
import org.apache.commons.io.{IOUtils, FileUtils}
import org.jets3t.service.utils.ServiceUtils
import org.slf4j.LoggerFactory
import com.jcabi.aspects.Loggable
import java.util.concurrent.TimeUnit
import scala.concurrent.Future
import com.codexica.s3crate.filetree.SafeInputStream

/**
 * A wrapper around the JetS3t interface to S3 for simplicity and testing
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class S3InterfaceImpl(s3: RestS3Service, bucket: S3Bucket) extends S3Interface {
  private val logger = LoggerFactory.getLogger(getClass)

  @Loggable(value = Loggable.DEBUG, limit = 1, unit = TimeUnit.MINUTES, prepend = true)
  override def listObjects(prefix: String): List[S3Object] = {
    s3.listObjects(bucket.getName, prefix, null).toList
  }

  @Loggable(value = Loggable.DEBUG, limit = 12, unit = TimeUnit.HOURS, prepend = true)
  override def save(data: SafeInputStream, blobLocation: String, uploadDir: File, maxPartSize: Long): S3Object = {
    val (fileHashes, completeMd5) = try {
      logger.info("Writing out {} in chunks of maximum size = {} bytes", data: Any, maxPartSize: Any)
      S3BlobWriter.write(data, uploadDir, maxPartSize)
    } catch {
      case x: Throwable => {
        IOUtils.closeQuietly(data)
        throw x
      }
    }
    val s3Object = if (fileHashes.size > 1) {
      multipartUpload(fileHashes, blobLocation, completeMd5)
    } else {
      //have the normal uploader take care of it
      assert(completeMd5.toList == fileHashes.head._2.toList)
      upload(fileHashes.head._1, blobLocation, fileHashes.head._2)
    }

    //clean up the leftover files:
    fileHashes.foreach(x => assert(x._1.delete()))
    s3Object
  }

  @Loggable(value = Loggable.DEBUG, limit = 12, unit = TimeUnit.HOURS, prepend = true)
  override def download(obj: S3Object, file: File) {
    val input = new BufferedInputStream(obj.getDataInputStream)
    val output = new BufferedOutputStream(new FileOutputStream(file))
    IOUtils.copy(input, output)
    input.close()
    output.close()
  }

  @Loggable(value = Loggable.DEBUG, limit = 12, unit = TimeUnit.HOURS, prepend = true)
  override def download(path: String, file: File) {
    download(s3.getObject(bucket.getName, path), file)
  }

  /**
   * Normal method of uploading to S3.
   *
   * @param file File to upload
   * @param location Path to upload it to
   * @param md5 The hash of the file contents
   * @return The uploaded object
   */
  @Loggable(value = Loggable.DEBUG, limit = 1, unit = TimeUnit.HOURS, prepend = true)
  protected def upload(file: File, location: String, md5: Array[Byte]): S3Object = {
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
  @Loggable(value = Loggable.DEBUG, limit = 12, unit = TimeUnit.HOURS, prepend = true)
  protected def multipartUpload(fileHashes: List[(File, Array[Byte])], location: String, completeMD5: Array[Byte]): S3Object = {
    logger.info("Uploading {} files to {} (resulting hash should be {})", fileHashes.size.toString, location, ServiceUtils.toBase64(completeMD5))
    //Multipart
    //UploadCallable.uploadInParts  //<- see for real implementation, using md5
    //weirdness: when uploading in multiple parts, if LESS than 5GB, must copy onto yourself, which regenerates the ETAG to be the MD5 hash, THEN we can verify that the upload worked correctly.
    //if greater than 5GB, can't do anything right now besides hope that the resulting concatenation didn't introduce errors on the AWS side (can only verify hashes for each part)
    throw new NotImplementedError()
  }
}
