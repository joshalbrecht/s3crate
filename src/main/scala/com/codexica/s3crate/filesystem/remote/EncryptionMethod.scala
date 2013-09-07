package com.codexica.s3crate.filesystem.remote

import com.codexica.s3crate.utils.SealedTraitFormat

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
sealed trait EncryptionMethod
case class NoEncryption() extends EncryptionMethod
case class SimpleEncryption() extends EncryptionMethod

object EncryptionMethod {
  implicit val format = new SealedTraitFormat[EncryptionMethod](NoEncryption(), SimpleEncryption())
}