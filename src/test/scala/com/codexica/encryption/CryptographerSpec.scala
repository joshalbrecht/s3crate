package com.codexica.encryption

import com.codexica.common.SafeLogSpecification
import java.io.File
import org.apache.commons.io.FileUtils
import java.util.{Random, UUID}
import org.slf4j.LoggerFactory
import org.specs2.mutable.After

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class CryptographerSpec extends SafeLogSpecification {

  trait Context extends CryptographicTestBase with BaseContext {

  }

  "asymmetric keys" should {
    "be generated correctly" in new Context {
      crypto.generateAsymmetricKey(keyType)
    }
    "enable encryption and decryption of messages" in new Context {
      val keyId = crypto.generateAsymmetricKey(keyType)
      val encryptedData = crypto.publicEncrypt(data, keyId)
      encryptedData.toList mustNotEqual data.toList
      val decryptedData = crypto.publicDecrypt(encryptedData, keyId)
      decryptedData.toList must be equalTo data.toList
    }
    "get generated and saved in a way that makes encryption and decryption work" in new Context {
      val keyId = crypto.generateAsymmetricKey(keyType)
      val encryptedData = crypto.publicEncrypt(data, keyId)
      val loadedCrypto = new Cryptographer(keystoreFile, keystorePassword)
      val decryptedData = loadedCrypto.publicDecrypt(encryptedData, keyId)
      decryptedData.toList must be equalTo data.toList
    }
    "fail correctly when there are no unlimited strength java crypto extensions installed" in new Context {

    }
    "fail correctly when the key is not present" in new Context {

    }
  }

  "symmetric keys" should {
    "be easy to generate for various strengtha" in new Context {

    }
    "properly encrypt and decrypt streams of data" in new Context {

    }
  }

}
