package com.codexica.s3crate.filetree.history.snapshotstore.s3

import org.jets3t.service.impl.rest.httpclient.RestS3Service
import org.jets3t.service.model.{S3Object, S3Bucket}
import java.io._
import org.apache.commons.io.{IOUtils, FileUtils}
import org.jets3t.service.utils.ServiceUtils
import org.slf4j.LoggerFactory

/**
 * A wrapper around the JetS3t interface to S3 for simplicity and testing
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class S3InterfaceImpl(s3: RestS3Service, bucket: S3Bucket) extends S3Interface {
  protected val logger = LoggerFactory.getLogger(getClass)

  override def listObjects(prefix: String): List[S3Object] = {
    s3.listObjects(bucket.getName, prefix, null).toList
  }

  override def upload(file: File, location: String, md5: Array[Byte]): S3Object = {
    val obj = new S3Object(location)
    obj.setDataInputFile(file)
    obj.setMd5Hash(md5)
    s3.putObject(bucket, obj)
  }

  override def multipartUpload(fileHashes: List[(File, Array[Byte])], location: String, completeMD5: Array[Byte]): S3Object = {
    logger.info("Uploading {} files to {} (resulting hash should be {})", fileHashes.size.toString, location, ServiceUtils.toBase64(completeMD5))
    //Multipart
    //UploadCallable.uploadInParts  //<- see for real implementation, using md5
    //weirdness: when uploading in multiple parts, if LESS than 5GB, must copy onto yourself, which regenerates the ETAG to be the MD5 hash, THEN we can verify that the upload worked correctly.
    //if greater than 5GB, can't do anything right now besides hope that the resulting concatenation didn't introduce errors on the AWS side (can only verify hashes for each part)
    throw new NotImplementedError()
  }

  override def download(obj: S3Object, file: File) {
    val input = new BufferedInputStream(obj.getDataInputStream)
    val output = new BufferedOutputStream(new FileOutputStream(file))
    IOUtils.copy(input, output)
    input.close()
    output.close()
  }

  override def download(path: String, file: File) {
    download(s3.getObject(bucket.getName, path), file)
  }
}
