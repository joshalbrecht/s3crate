package com.codexica.s3crate.filetree

import java.io.OutputStream
import scala.concurrent.Future
import com.codexica.s3crate.filetree.history.FilePathState
import com.codexica.common.SafeInputStream

/**
 * An interface to a filesystem, for reading data and metadata from paths
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
trait ReadableFileTree {

  /**
   * @param path Read the data from this path
   * @return An input stream that will generate sensibly wrapped exceptions. Will block while reading.
   */
  def read(path: FilePath): SafeInputStream

  /**
   * @param path Read the metadata associated with this path
   * @return A future that will be complete when all metadata has been read.
   *         The called must check the future for errors or ensure that it will be checked.
   *         Possible errors include:  InaccessibleDataError
   *                                   UnexpectedError: All other non-fatal errors
   */
  def metadata(path: FilePath): Future[FilePathState]
}
