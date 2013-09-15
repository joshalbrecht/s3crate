package com.codexica.s3crate.filetree.history

import com.codexica.common.{SafeInputStream, SafeLogSpecification}
import com.codexica.encryption.CryptographicTestBase
import java.io.ByteArrayInputStream
import org.apache.commons.io.IOUtils

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class SymmetricEncryptorSpec extends SafeLogSpecification {

  trait Context extends CryptographicTestBase with BaseContext {
    val input = new SafeInputStream(new ByteArrayInputStream(data), "test data")
  }

  "passing streams through the encryptor" should {
    "do nothing if there is no key for encryption" in new Context {
      val encryptor = new SymmetricEncryptor(None, crypto)
      val (details, encryptedData) = encryptor.encrypt(input)
      IOUtils.toByteArray(encryptor.decrypt(encryptedData, details)).toList must be equalTo data.toList
    }
    "encrypt and decrypt the stream to give the exact same result" in new Context {
      val encryptor = new SymmetricEncryptor(Option(crypto.generateAsymmetricKey(keyType)), crypto)
      val (details, encryptedData) = encryptor.encrypt(input)
      IOUtils.toByteArray(encryptor.decrypt(encryptedData, details)).toList must be equalTo data.toList
    }
  }

}
