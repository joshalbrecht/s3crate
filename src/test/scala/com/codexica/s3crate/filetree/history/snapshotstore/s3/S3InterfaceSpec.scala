package com.codexica.s3crate.filetree.history.snapshotstore.s3

import com.codexica.common.{InaccessibleDataError, SafeLogSpecification}
import java.io.{InputStream, File}
import org.jets3t.service.impl.rest.httpclient.RestS3Service
import org.jets3t.service.model.{S3Object, S3Bucket}
import org.scalamock.FunctionAdapter2
import org.scalamock.specs2.MockFactory
import org.jets3t.service.ServiceException

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class S3InterfaceSpec extends SafeLogSpecification with MockFactory {

  //TODO: finish this test someday :(
//  trait Context extends BaseContext {
//    val restService = mock[RestS3Service]
//    val bucket = mock[S3Bucket]
//    val s3 = new S3InterfaceImpl(restService, bucket)
//  }
//
//  "downloading data" should {
//    "throw a proper exception if the remote path is temporarily inaccessible" in new Context {
//      val mockedS3Object = new S3Object() {
//        override def getDataInputStream: InputStream = {
//          new InputStream() {
//            def read(): Int = {
//              throw new ServiceException("failure during download")
//            }
//          }
//        }
//      }
//      (restService.getObject(_: String, _: String)).expects(new FunctionAdapter2((path: String, bucketName: String) => {
//        true
//      })).returning(mockedS3Object)
//
//      s3.download("somewhere", new File("useless/path")) must throwAn[InaccessibleDataError]
//    }
//  }
}
