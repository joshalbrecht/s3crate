package com.codexica.s3crate.filesystem

import com.codexica.s3crate.utils.SealedTraitFormat

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
sealed trait SnapshotState
case class Deleted() extends SnapshotState
case class Restored() extends SnapshotState
case class Created() extends SnapshotState
case class Changed() extends SnapshotState

object SnapshotState {
  implicit val format = new SealedTraitFormat[SnapshotState](Created(), Changed(), Deleted(), Restored())
}