package com.codexica.s3crate.filetree.history

import com.codexica.encryption.{Cryptographer, KeyPairReference}

/**
 * Apply a public key to encrypt/decrypt data directly. Note that you should NOTE use this for anything sizable, since
 * it's MUCH slower than symmetric encryption.
 *
 * Also note that there are three possibilities for the key that is provided in the constructor:
 * 1. None: No encryption will be performed and no decryption is possible
 * 2. Some(key id associated only with a public key): It will be possible to encrypt data, but decryption will throw an
 * Exception.
 * 3. Some(key id associated with both public and private key): Encryption and decryption should work correctly.
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class AsymmetricEncryptor(keyIdOpt: Option[KeyPairReference], crypto: Cryptographer) {

  /**
   * @param data Data to encrypt
   * @return Encrypted data
   */
  def encrypt(data: Array[Byte]): Array[Byte] = {
    keyIdOpt match {
      case None => data
      case Some(keyId) => crypto.publicEncrypt(data, keyId)
    }
  }

  /**
   * @param data the encrypted data that you want to decrypt
   * @throws MissingKeyError if there is no corresponding private key for the given key (ie, can't decrypt data)
   * @return the decrypted data
   */
  def decrypt(data: Array[Byte]): Array[Byte] = {
    keyIdOpt match {
      case None => data
      case Some(keyId) => crypto.publicDecrypt(data, keyId)
    }
  }
}
