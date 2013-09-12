package com.codexica.s3crate.filetree.history.snapshotstore.s3

import com.codexica.s3crate.SafeLogSpecification
import org.joda.time.{DateTimeZone, DateTime}
import com.google.inject.Guice
import org.slf4j.LoggerFactory
import org.specs2.mutable.After
import com.codexica.s3crate.filetree.SafeInputStream
import java.io.ByteArrayInputStream
import java.util.{UUID, Random}
import org.jets3t.service.utils.ServiceUtils
import org.apache.commons.io.FileUtils

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class S3InterfaceSpec extends SafeLogSpecification {
  trait Context extends After with BaseContext{
    //load the special configuration with login details
    System.setProperty("config.resource", "/integration.conf")
    val injector = Guice.createInjector(new S3Module())
    val s3 = injector.getInstance(classOf[S3Interface])

    //make a new prefix under which you should work
    val now = new DateTime(DateTimeZone.UTC)
    val prefix = s"tests/integration/$now/"

    //make sure we delete everything when the tests are done
    def after = {
      val logger = LoggerFactory.getLogger(getClass)
      logger.info("Deleting test info from s3 ($prefix)")
      s3.delete(prefix)
    }

    //test data:
    val dataLength = 78342
    val bytes = new Array[Byte](dataLength)
    new Random(98735).nextBytes(bytes)
    val fullDataHash = ServiceUtils.computeMD5Hash(bytes)
    val inputStream = () => {new SafeInputStream(new ByteArrayInputStream(bytes), "(local test data)")}
    val location = prefix + UUID.randomUUID().toString
    val uploadDir = FileUtils.getTempDirectory
  }

  "listing objects" should {
    "return nothing if there is nothing there" in new Context {
      s3.listObjects(prefix) must be equalTo Set()
    }
    "return exactly the list of objects under the prefix" in new Context {
      s3.save(inputStream(), prefix + "asdfe", uploadDir, dataLength*2)
      s3.save(inputStream(), prefix + "asddd/thing/whatever", uploadDir, dataLength*2)
      s3.save(inputStream(), prefix + "ffrrdd", uploadDir, dataLength*2)
      s3.listObjects(prefix + "asd").map(_.getKey) must be equalTo Set(prefix + "asdfe", prefix + "asddd/thing/whatever")
    }
  }

  "saving data" should {
    "create an identical file for single-part files" in new Context {

    }
    "create an identical file for multi-part files" in new Context {

    }
    "throw the correct exception and close the stream if the underlying data stream fails" in new Context {

    }
  }

  "downloading a file" should {
    "save the exact same data into the file" in new Context {

    }
    "throw a proper exception if the file is inaccessible" in new Context {

    }
    "throw a proper exception if the remote path is invalid" in new Context {

    }
  }

}
