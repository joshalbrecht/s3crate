package com.codexica.s3crate

import org.specs2.mutable.Specification
import org.apache.commons.io.FileUtils
import java.io.File
import com.codexica.s3crate.utils.Encryption

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class EncryptionSpec extends Specification {
  "Encrypted files" should {
    "decrypt to exactly the same contents" in {
      //generate a key:
      val key = Encryption.generateSymmetricKey()
      //randomly generate some data for the input file
      val content = "These are the words that I put in the file"
      val plainFile = "/tmp/plain.txt"
      FileUtils.write(new File(plainFile), content)
      //make some tmp files for the output and key
      val cipherFile = "/tmp/cipher.txt"
      //encrypt the file
      Encryption.encrypt(plainFile, cipherFile, key)
      //decrypt the file
      val outputFile = "/tmp/output.txt"
      Encryption.decrypt(cipherFile, outputFile, key)
      val decryptedContent = FileUtils.readFileToString(new File(outputFile))
      assert(content == decryptedContent)
    }
  }
}
