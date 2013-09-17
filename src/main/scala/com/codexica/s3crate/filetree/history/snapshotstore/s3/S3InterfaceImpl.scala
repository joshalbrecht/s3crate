package com.codexica.s3crate.filetree.history.snapshotstore.s3

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{AbortMultipartUploadRequest, InitiateMultipartUploadRequest, CompleteMultipartUploadRequest, UploadPartRequest}
import com.codexica.common.{SafeOutputStream, FutureUtils, InaccessibleDataError, SafeInputStream}
import com.google.inject.Inject
import com.jcabi.aspects.Loggable
import java.io._
import java.util.concurrent.TimeUnit
import org.apache.commons.io.IOUtils
import org.jets3t.service.S3ServiceException
import org.jets3t.service.impl.rest.httpclient.RestS3Service
import org.jets3t.service.model.container.ObjectKeyAndVersion
import org.jets3t.service.model.{S3Object, S3Bucket}
import org.jets3t.service.utils.ServiceUtils
import org.slf4j.LoggerFactory
import scala.collection.JavaConversions._
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Await, Future}
import scala.util.{Failure, Success}
import scala.util.control.NonFatal
import com.google.common.collect.Lists

/**
 * A wrapper around the JetS3t interface to S3 for simplicity and testing
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
protected[s3] class S3InterfaceImpl @Inject()(s3: RestS3Service,
                                              bucket: S3Bucket,
                                              directS3: AmazonS3Client,
                                              @S3ExecutionContext() ec: ExecutionContext) extends S3Interface {

  implicit val context = ec
  private val logger = LoggerFactory.getLogger(getClass)

  @Loggable(value = Loggable.DEBUG, limit = 1, unit = TimeUnit.MINUTES, prepend = true)
  override def listObjects(prefix: String): Set[S3Object] = {
    s3.listObjects(bucket.getName, prefix, null).toSet
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
  override def download(path: String, file: File) {
    try {
      val s3Object = s3.getObject(bucket.getName, path)
      download(s3Object, file)
    } catch {
      case e: S3ServiceException => {
        if (e.getS3ErrorCode == "NoSuchKey") {
          throw new IllegalArgumentException(s"Could not find $path")
        } else {
          throw new InaccessibleDataError(s"Could not access $path", e)
        }
      }
    }
  }

  //Note: this implementation may take a long time. We currently don't care because we only use it for testing...
  @Loggable(value = Loggable.DEBUG, limit = 2, unit = TimeUnit.HOURS, prepend = true)
  def delete(prefix: String) {
    val allKeys = s3.listObjects(bucket.getName, prefix, null)
    if (allKeys.size > 0) {
      val result = s3.deleteMultipleObjects(bucket.getName, allKeys.map(s3Obj => {
        new ObjectKeyAndVersion(s3Obj.getKey)
      }), true)
      if (result.hasErrors) {
        throw new InaccessibleDataError("Failed to delete: " + result.getErrorResults.toList.map(error => {
          s"${error.getKey} ${error.getVersion} ${error.getErrorCode} ${error.getMessage}"
        }).mkString("\n"), null)
      }
    }
  }

  /**
   * Internal method for downloading from S3. Very careful to make sure our streams get closed in the case of any error.
   */
  @Loggable(value = Loggable.DEBUG, limit = 12, unit = TimeUnit.HOURS, prepend = true)
  protected def download(obj: S3Object, file: File) {
    val dataInputStream = obj.getDataInputStream
    val input = new SafeInputStream(dataInputStream, s"reading aws stream from ${obj.getKey}")
    try {
      val output = SafeOutputStream.fromFile(file)
      try {
        IOUtils.copy(input, output)
        input.close()
        output.close()
      } finally {
        IOUtils.closeQuietly(output)
      }
    } finally {
      IOUtils.closeQuietly(input)
    }
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
    val result = s3.putObject(bucket, obj)
    assert(result.getETag == ServiceUtils.toHex(md5))
    result
  }

  //TODO: note that you will get exceptions if the first part is less than 5MB...
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
  protected def multipartUpload(fileHashes: List[(File, Array[Byte])],
                                location: String,
                                completeMD5: Array[Byte]): S3Object = {
    logger.info("Uploading {} files to {} (resulting hash should be {})",
      fileHashes.size.toString, location, ServiceUtils.toBase64(completeMD5))

    val bucketName = bucket.getName
    //initialize
    val initRequest = new InitiateMultipartUploadRequest(bucketName, location)
    val initResponse = directS3.initiateMultipartUpload(initRequest)

    //make a list of futures for when each is done
    val MAX_PART_RETRIES = 3
    val partFutures = fileHashes.zipWithIndex.map({ case ((file, md5), i) => Future {
      val uploadRequest = new UploadPartRequest()
        .withBucketName(bucketName)
        .withKey(location)
        .withUploadId(initResponse.getUploadId)
        .withPartNumber(i+1)
        .withMD5Digest(ServiceUtils.toBase64(md5))
        .withFileOffset(0)
        .withFile(file)
        .withPartSize(file.length())
      FutureUtils.retry(MAX_PART_RETRIES)({
        directS3.uploadPart(uploadRequest).getPartETag
      }).get
    }
    })

    val partETags = try {
      Await.result(FutureUtils.sequenceOrBailOut(partFutures), Duration.Inf)
    } catch {
      case NonFatal(t) => {
        directS3.abortMultipartUpload(new AbortMultipartUploadRequest(bucketName, location, initResponse.getUploadId))
        throw t
      }
    }

    //note: that's stupid. Calling completeMultipartUpload sorts the list that you pass in. yay side effects
    import collection.JavaConverters._
    val compRequest = new CompleteMultipartUploadRequest(bucketName, location, initResponse.getUploadId, Lists.newArrayList(partETags.asJava))
    directS3.completeMultipartUpload(compRequest)
    //some weirdness--when uploading multiple parts, the resulting ETag should be the MD5 of the concatenated MD5's of
    //the parts, - the number of parts:
    //see here: https://forums.aws.amazon.com/thread.jspa?threadID=126111
    val s3Object = s3.getObject(bucketName, location)
    s3Object.getDataInputStream.close()
    val etag = s3Object.getETag
    val numParts = fileHashes.size
    assert(etag.split("-").last == numParts.toString)
    val expectedHash = ServiceUtils.toHex(ServiceUtils.computeMD5Hash(fileHashes.flatMap(_._2).toArray))
    assert(etag.split("-").head == expectedHash)
    s3Object
  }
}
