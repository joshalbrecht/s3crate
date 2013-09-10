package com.codexica.s3crate.filetree.history.snapshotstore.s3

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.apache.commons.lang3.RandomStringUtils
import java.io.{File, ByteArrayInputStream}
import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration.Duration
import org.scalamock.specs2.MockFactory
import org.jets3t.service.model.S3Object
import org.scalamock.FunctionAdapter3
import org.jets3t.service.utils.ServiceUtils
import org.apache.commons.io.FileUtils

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class S3BlobWriterSpec extends Specification with MockFactory {

  trait Context extends Scope {
    val dataLength = 34985
    val data = RandomStringUtils.random(dataLength)
    val bytes = data.getBytes("UTF-8")
    val fullDataHash = ServiceUtils.computeMD5Hash(bytes)
    val inputStream = new ByteArrayInputStream(bytes)
    val s3 = mock[S3Interface]
    val ec = ExecutionContext.Implicits.global
  }

  "saving a regular file" should {
    "call upload with the correct values" in new Context {
      val blob = new S3BlobWriter(s3, ec, FileUtils.getTempDirectory)
      val path = "some/place/to/put/data"
      (s3.upload _).expects(new FunctionAdapter3((file: File, location: String, md5: Array[Byte]) => {
        md5.toList must be equalTo fullDataHash.toList
        location must be equalTo path
        FileUtils.readFileToByteArray(file).toList must be equalTo bytes.toList
        true
      }))
      Await.result(blob.save(inputStream, path, dataLength*2), Duration.Inf) must be equalTo Unit
    }
  }

  "saving a large file" should {
    "call upload with the correct values" in new Context {

    }
  }

}
