package com.codexica.s3crate.filetree.history.snapshotstore.s3

import org.joda.time.{DateTimeZone, DateTime}
import com.google.inject.Guice
import org.slf4j.LoggerFactory
import org.specs2.mutable.After
import java.io.{InputStream, File, ByteArrayInputStream}
import java.util.{UUID, Random}
import org.jets3t.service.utils.ServiceUtils
import org.apache.commons.io.FileUtils
import com.codexica.common.{InaccessibleDataError, SafeLogSpecification, SafeInputStream}

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
    val workingDir = new File(FileUtils.getTempDirectory, UUID.randomUUID().toString)
    FileUtils.forceMkdir(workingDir)

    //make sure we delete everything when the tests are done
    def after = {
      val logger = LoggerFactory.getLogger(getClass)
      logger.info(s"Deleting test info from s3 ($prefix)")
      s3.delete(prefix)
      FileUtils.deleteDirectory(workingDir)
    }

    //test data:
    val dataLength = 78342
    val bytes = new Array[Byte](dataLength)
    new Random(98735).nextBytes(bytes)
    val fullDataHash = ServiceUtils.computeMD5Hash(bytes)
    val inputStream = () => {new SafeInputStream(new ByteArrayInputStream(bytes), "(local test data)")}
    val location = prefix + UUID.randomUUID().toString
  }

  "listing objects" should {
    "return nothing if there is nothing there" in new Context {
      s3.listObjects(prefix) must be equalTo Set()
    }
    "return exactly the list of objects under the prefix" in new Context {
      s3.save(inputStream(), prefix + "asdfe", workingDir, dataLength*2)
      s3.save(inputStream(), prefix + "asddd/thing/whatever", workingDir, dataLength*2)
      s3.save(inputStream(), prefix + "ffrrdd", workingDir, dataLength*2)
      s3.listObjects(prefix + "asd").map(_.getKey) must be equalTo Set(prefix + "asdfe", prefix + "asddd/thing/whatever")
    }
  }

  "uploading data" should {
    "create the correct md5 hash for single-part files" in new Context {
      s3.save(inputStream(), location, workingDir, dataLength*2)
      s3.listObjects(location).head.getMd5HashAsBase64 must be equalTo ServiceUtils.toBase64(fullDataHash)
    }
    "create the correct md5 hash for multi-part files" in new Context {
      s3.save(inputStream(), location, workingDir, dataLength / 2)
      s3.listObjects(location).head.getMd5HashAsBase64 must be equalTo ServiceUtils.toBase64(fullDataHash)
    }
    "throw the correct exception and close the stream if the underlying data stream fails" in new Context {
      def badInputStream(failure: Throwable) = {
        new SafeInputStream(new InputStream() {
          val data = List(1,2,3,4,5).toIterator
          def read(): Int = {
            if (data.hasNext) {
              data.next()
            } else {
              throw failure
            }
          }
        }, "unsafe input stream")
      }

      var stream = badInputStream(new Exception("fail"))
      s3.save(stream, location, workingDir, dataLength*2) must throwAn[InaccessibleDataError]
      stream.wasClosed must be equalTo true

      stream = badInputStream(new Throwable("fail"))
      s3.save(stream, location, workingDir, dataLength*2) must throwA[Throwable]
      stream.wasClosed must be equalTo true
    }
  }

  "downloading data" should {
    "create an identical file for single-part files" in new Context {
      s3.save(inputStream(), location, workingDir, dataLength*2)
      val file = new File(workingDir, UUID.randomUUID().toString)
      s3.download(location, file)
      FileUtils.readFileToByteArray(file).toList must be equalTo bytes.toList
    }
    "create an identical file for multi-part files" in new Context {
      s3.save(inputStream(), location, workingDir, dataLength / 2)
      val file = new File(workingDir, UUID.randomUUID().toString)
      s3.download(location, file)
      FileUtils.readFileToByteArray(file).toList must be equalTo bytes.toList
    }
    "throw a proper exception if the file is inaccessible" in new Context {
      s3.save(inputStream(), location, workingDir, dataLength*2)
      val file = new File("/path/that/does/not/exist")
      s3.download(location, file) must throwAn[InaccessibleDataError]
    }
    "throw a proper exception if the remote path is invalid" in new Context {
      val file = new File(workingDir, UUID.randomUUID().toString)
      s3.download(prefix + "/path/that/does/not/exist", file) must throwAn[InaccessibleDataError]
    }
  }

}
