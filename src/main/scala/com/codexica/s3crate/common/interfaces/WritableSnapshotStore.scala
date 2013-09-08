package com.codexica.s3crate.common.interfaces

import scala.concurrent.Future
import java.io.InputStream
import com.codexica.s3crate.common.models.{FileSnapshot, DataBlob}

/**
 * Interface for writing new snapshots to the storage system.
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
trait WritableSnapshotStore {
  def save(blob: DataBlob, data: InputStream, length: Long): Future[Unit]
  def save(snapshot: FileSnapshot): Future[Unit]
}
