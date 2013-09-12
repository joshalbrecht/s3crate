package com.codexica.encryption

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class KeyPair(pub: Array[Byte], priv: Array[Byte], method: PublicEncryptionMethod)

object KeyPair {
  def generate(method: PublicEncryptionMethod): KeyPair = {
    val publicKey = Encryption.generatePublicKey()
    val privateKey = Encryption.generatePublicKey()
    KeyPair(publicKey, privateKey, method)
  }
}
