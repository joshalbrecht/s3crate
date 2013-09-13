package com.codexica.s3crate.filetree.history

import com.codexica.common.SealedTraitFormat

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
sealed trait CompressionMethod
case class NoCompression() extends CompressionMethod
case class SnappyCompression() extends CompressionMethod

object CompressionMethod {
  implicit val format = new SealedTraitFormat[CompressionMethod](NoCompression(), SnappyCompression())
}
