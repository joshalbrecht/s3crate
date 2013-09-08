package com.codexica.s3crate.common.models

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class ContentHash(value: Array[Byte], hashType: HashType)
sealed trait HashType
case class MD5()
