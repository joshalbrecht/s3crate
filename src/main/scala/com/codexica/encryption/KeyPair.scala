package com.codexica.encryption

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class KeyPair(pub: Array[Byte], priv: Array[Byte], method: PublicEncryptionMethod)
