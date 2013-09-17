package com.codexica.common

import com.jcabi.aspects.Loggable
import java.io.{BufferedOutputStream, FileOutputStream, File, IOException, OutputStream}
import java.util.concurrent.TimeUnit
import scala.util.Try
import scala.util.control.NonFatal

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class SafeOutputStream(stream: OutputStream, name: String) extends OutputStream {
  private var closeWasCalled = false

  def wasClosed = closeWasCalled

  override def close() {
    closeWasCalled = true
    Try(stream.close()).recoverWith(SafeOutputStream.handler)
  }

  def write(b: Int) {
    Try(stream.write(b)).recoverWith(SafeOutputStream.handler)
  }

  override def write(b: Array[Byte]) {
    Try(stream.write(b)).recoverWith(SafeOutputStream.handler)
  }

  override def write(b: Array[Byte], off: Int, len: Int) {
    Try(stream.write(b, off, len)).recoverWith(SafeOutputStream.handler)
  }

  override def flush() {
    Try(stream.flush()).recoverWith(SafeOutputStream.handler)
  }

  override def toString: String = {
    s"$getClass($name)"
  }

}

object SafeOutputStream {

  /**
   * Helper for building from a file (common case)
   * @param file The file to read in
   * @return A SafeOutputStream based on that file
   */
  @Loggable(value = Loggable.TRACE, limit = 300, unit = TimeUnit.MILLISECONDS, prepend = true)
  def fromFile(file: File): SafeOutputStream = {
    Try(new SafeOutputStream(new BufferedOutputStream(new FileOutputStream(file)), file.getAbsolutePath))
    .recoverWith(handler).get
  }

  def handler: PartialFunction[Throwable, Nothing] = {
    case e: IOException => throw new InaccessibleDataError("Failure writing to stream", e)
    case NonFatal(e)    => throw new UnexpectedError("Unexpected error while writing to stream", e)
  }
}
