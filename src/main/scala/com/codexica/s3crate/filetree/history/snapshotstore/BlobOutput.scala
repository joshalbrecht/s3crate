package com.codexica.s3crate.filetree.history.snapshotstore

import scala.concurrent.Future
import java.io.InputStream

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
abstract class BlobOutput {
  def save(data: InputStream): Future[String]
}
