package com.codexica.encryption

import com.codexica.common.SafeLogSpecification
import org.specs2.mutable.After
import java.io.File
import org.apache.commons.io.FileUtils
import java.util.{Random, UUID}
import org.slf4j.LoggerFactory

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class CryptographerIntegrationSpec extends SafeLogSpecification {

  trait Context extends After with BaseContext {
    val keystoreFile = new File(FileUtils.getTempDirectory, UUID.randomUUID.toString)
    val keystorePassword = "password".toCharArray
    val crypto = new Cryptographer(keystoreFile, keystorePassword)
    val dataLength = 32874
    val data = new Array[Byte](dataLength)
    new Random(27831).nextBytes(data)

    //make sure we delete everything when the tests are done
    def after = {
      val logger = LoggerFactory.getLogger(getClass)
      if (keystoreFile.exists()) {
        logger.info(s"Cleaning up keystore $keystoreFile")
        assert(keystoreFile.delete())
      }
    }
  }

  "asymmetric keys" should {
    "work for various key strengths" in new Context {
      List(1024, 2048, 4096, 8192).foreach(keyLen => {
        val keyId = crypto.generateAsymmetricKey(RSA(keyLen))
        crypto.publicDecrypt(crypto.publicEncrypt(data, keyId), keyId).toList must be equalTo data.toList
      })
    }
  }

  "symmetric keys" should {
    "properly encrypt and decrypt files" in new Context {
      //generate a key:
      val key = crypto.generateSymmetricKey(AES(256))
      //randomly generate some data for the input file
      val content = "These are the words that I put in the file"
      val plainFile = "/tmp/plain.txt"
      FileUtils.write(new File(plainFile), content)
      //make some tmp files for the output and key
      val cipherFile = "/tmp/cipher.txt"
      //encrypt the file
      crypto.encrypt(plainFile, cipherFile, key)
      //decrypt the file
      val outputFile = "/tmp/output.txt"
      crypto.decrypt(cipherFile, outputFile, key)
      val decryptedContent = FileUtils.readFileToString(new File(outputFile))
      assert(content == decryptedContent)
    }
  }

}
