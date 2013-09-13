package com.codexica.common

import java.io.{FileInputStream, File, IOException, InputStream}
import scala.util.Try
import scala.util.control.NonFatal
import com.jcabi.aspects.Loggable
import java.util.concurrent.TimeUnit

/**
 * Proxy to InputStream that wraps all errors with the correct exception, depending on how they should be handled
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class SafeInputStream(stream: InputStream, name: String) extends InputStream {
  private var closeWasCalled = false

  def wasClosed = closeWasCalled

  def read(): Int = Try(stream.read()).recoverWith(handler).get

  override def read(b: Array[Byte]): Int = Try(stream.read(b)).recoverWith(handler).get

  override def read(b: Array[Byte], off: Int, len: Int): Int = Try(stream.read(b, off, len)).recoverWith(handler).get

  override def skip(n: Long): Long = Try(stream.skip(n)).recoverWith(handler).get

  override def available(): Int = Try(stream.available()).recoverWith(handler).get

  override def close() {
    closeWasCalled = true
    Try(stream.close()).recoverWith(handler)
  }

  override def mark(readlimit: Int) {
    Try(stream.mark(readlimit)).recoverWith(handler)
  }

  override def reset() {
    Try(stream.reset()).recoverWith(handler)
  }

  override def markSupported(): Boolean = Try(stream.markSupported()).recoverWith(handler).get

  protected def handler: PartialFunction[Throwable, Nothing] = {
    case e: IOException => throw new InaccessibleDataError("Failure reading from file tree", e)
    case e: CodexicaError => throw e
    case NonFatal(e) => throw new UnexpectedError("Unexpected error while reading from file tree", e)
  }

  override def toString: String = {
    s"$getClass($name)"
  }
}

object SafeInputStream {

  /**
   * Helper for building from a file (common case)
   * @param file The file to read in
   * @return A SafeInputStream based on that file
   */
  @Loggable(value = Loggable.TRACE, limit = 300, unit = TimeUnit.MILLISECONDS, prepend = true)
  def fromFile(file: File): SafeInputStream = {
    new SafeInputStream(new FileInputStream(file), file.getAbsolutePath)
  }
}
