package com.codexica.s3crate.filetree.history.snapshotstore.s3

import com.amazonaws.services.s3.AmazonS3Client
import com.codexica.common.{InaccessibleDataError, SafeLogSpecification}
import java.io.{InputStream, File}
import org.jets3t.service.ServiceException
import org.jets3t.service.impl.rest.httpclient.RestS3Service
import org.jets3t.service.model.{S3Object, S3Bucket}
import org.specs2.mock.Mockito
import scala.concurrent.ExecutionContext

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class S3InterfaceSpec extends SafeLogSpecification {

  trait Context extends Mockito with BaseContext {
    val restService = mock[RestS3Service]
    val bucket = mock[S3Bucket]
    val client = mock[AmazonS3Client]
    implicit val context = ExecutionContext.Implicits.global
    val s3 = new S3InterfaceImpl(restService, bucket, client, context)
  }

  "downloading data" should {
    "throw a proper exception if the remote path is temporarily inaccessible" in new Context {
      val mockedS3Object = new S3Object() {
        override def getDataInputStream: InputStream = {
          new InputStream() {
            def read(): Int = {
              throw new ServiceException("failure during download")
            }
          }
        }
      }
      restService.getObject(anyString, anyString) returns mockedS3Object
      s3.download("somewhere", new File("useless/path")) must throwAn[InaccessibleDataError]
    }
  }
}
