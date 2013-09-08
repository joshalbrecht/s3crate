package com.codexica.s3crate.filesystem

import scala.concurrent.Future
import java.io.InputStream
import com.codexica.s3crate.common.models.{FilePathState, FilePathEvent, FilePath}

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
trait FileSystem {

  /**
   * Begin initialization. Future will be ready when everything is initialized, and will return all paths that we
   * encountered while starting up. If initialization failed, will return the failure and the program should shut down.
   */
  def start(): Future[Set[FilePathEvent]]

  //TODO:  what to do about errors here?
  /**
   * @param event Given this path
   * @return Return a snapshot if it exists and can be read.
   */
  def snapshot(event: FilePath): Future[Option[FilePathState]]

  /**
   * @param data read from this stream and write it to the filesystem
   * @param snapshot to this corresponding snapshot location
   * @return only when finished trying as hard as possible to write
   * @throws exceptions if retries have been exhausted and this just absolutely cannot be written, or if file path
   *                    is invalid, deleted, or you don't have permission to read or write
   */
  def write(data: ReadableFile, snapshot: FilePathState): Future[Unit]

  /**
   * Note that this function should NOT throw any exceptions or errors as you try to read the data!  It should return
   * a simple wrapper around something that is not guaranteed to exist or be accessible. The reasoning for this is that
   * no matter what, you have to handle those exceptions during access, so there's no need to handle them twice.
   *
   * @param path The location to read
   * @return A wrapper that will allow you to figure out the length or read the actual data.
   */
  def read(path: FilePath): ReadableFile
}
