package com.codexica.encryption

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
sealed trait PublicEncryptionMethod
case class RSA() extends PublicEncryptionMethod
