package com.codexica.s3crate.filetree

import java.io.{FileInputStream, File, IOException, InputStream}
import scala.util.Try
import com.codexica.s3crate.{S3CrateError, UnexpectedError}
import scala.util.control.NonFatal

object SafeInputStream {
  def fromFile(file: File): SafeInputStream = {
    new SafeInputStream(new FileInputStream(file), file.getAbsolutePath)
  }
}

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
    case e: S3CrateError => throw e
    case NonFatal(e) => throw new UnexpectedError("Unexpected error while reading from file tree", e)
  }

  override def toString: String = {
    s"$getClass($name)"
  }
}
