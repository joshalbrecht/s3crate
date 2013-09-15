package com.codexica.encryption

import org.specs2.mutable.After
import java.io.File
import org.apache.commons.io.FileUtils
import java.util.{Random, UUID}
import org.slf4j.LoggerFactory

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
trait CryptographicTestBase extends After {
  val keystoreFile = new File(FileUtils.getTempDirectory, UUID.randomUUID.toString)
  val keystorePassword = "password".toCharArray
  val crypto = new Cryptographer(keystoreFile, keystorePassword)
  val dataLength = 32874
  val data = new Array[Byte](dataLength)
  new Random(27831).nextBytes(data)
  val keyType = RSA(1024)

  //make sure we delete everything when the tests are done
  def after = {
    val logger = LoggerFactory.getLogger(getClass)
    if (keystoreFile.exists()) {
      logger.info(s"Cleaning up keystore $keystoreFile")
      assert(keystoreFile.delete())
    }
  }
}
