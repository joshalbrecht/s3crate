package com.codexica.s3crate.filetree.history

import com.codexica.common.SafeLogSpecification
import com.codexica.encryption.{CryptographicTestBase, MissingKeyError, RSA, Cryptographer}
import java.io.File
import org.apache.commons.io.FileUtils
import java.util.{Random, UUID}

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class AsymmetricEncryptorSpec extends SafeLogSpecification {

  trait Context extends CryptographicTestBase with BaseContext {

  }

  "Encryption and decryption" should {
    "return the same data if no key is provided at all" in new Context {
      val encryptor = new AsymmetricEncryptor(None, crypto)
      encryptor.decrypt(encryptor.encrypt(data)) must be equalTo data
    }
    "return the same data for a valid private/public key pair" in new Context {
      val encryptor = new AsymmetricEncryptor(Option(crypto.generateAsymmetricKey(keyType)), crypto)
      encryptor.decrypt(encryptor.encrypt(data)) must be equalTo data
    }
    "throw a MissingKeyError when there is no private key" in new Context {
      val keyId = crypto.generateAsymmetricKey(keyType)
      val encryptor = new AsymmetricEncryptor(Option(keyId), crypto)
      crypto.deletePrivateKey(keyId)
      val encryptedData = encryptor.encrypt(data)
      encryptor.decrypt(encryptedData) must throwA[MissingKeyError]
    }
  }
}
