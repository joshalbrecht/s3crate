package com.codexica.s3crate.filetree.history

import com.codexica.common.SafeInputStream
import org.xerial.snappy.{SnappyOutputStream, Snappy, SnappyInputStream}
import java.io.{ByteArrayOutputStream, InputStream}
import org.apache.commons.io.IOUtils
import com.Ostermiller.util.CircularByteBuffer
import com.jcabi.aspects.Loggable
import java.util.concurrent.TimeUnit

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class Compressor {

  //TODO: both of these should be read from the configuration

  //ratio of ( compressed size / original size ). If less than this ratio, it's worth it to encrypt
  private val COMPRESSION_RATIO = 0.7
  //don't bother compressing unless we get at least a 30% savings in space
  //the purpose of this is to ignore compression for already encoded binaries (mp3, jpg, zips, etc)
  private val numBytesToTryCompressing = 256 * 1024

  /**
   * @param inputGenerator Given the input stream that can be generated with this function
   * @throws exceptions from reading a SafeInputStream
   * @return A pair of "whether the stream was compressed" and the actual stream (compressed or not)
   */
  @Loggable(value = Loggable.DEBUG, limit = 5, unit = TimeUnit.SECONDS, prepend = true)
  def compress(inputGenerator: () => SafeInputStream): (CompressionMethod, SafeInputStream) = {
    val input = inputGenerator()
    val ratio = compressionRatio(inputGenerator(), numBytesToTryCompressing)
    //note the extra invocation of inputGenerator so that we don't read bytes from the actual stream
    val shouldZip = ratio < COMPRESSION_RATIO
    if (shouldZip) {
      //have to write to a circular buffer with a snappy output stream, and then read from it...
      (SnappyCompression(), new SafeInputStream(new InputStream {
        val b = new Array[Byte](1)
        def read(): Int = {
          read(b, 0, 1)
          b(0)
        }
        private val cbb = new CircularByteBuffer(CircularByteBuffer.INFINITE_SIZE)
        val circInput = cbb.getInputStream
        val circOutput = cbb.getOutputStream
        val snappyOut = new SnappyOutputStream(circOutput)
        val readBuffer = new Array[Byte](8 * 1024)
        var wasClosed = false
        override def read(b: Array[Byte], off: Int, len: Int): Int = {
          val numBytesToRead = len - off
          var closed = false
          while (!closed && cbb.getAvailable < numBytesToRead) {
            val bytesRead = input.read(readBuffer, 0, readBuffer.length)
            if (bytesRead == -1) {
              if (!wasClosed) {
                snappyOut.close()
                wasClosed = true
              }
              closed = true
            } else {
              snappyOut.write(readBuffer, 0, bytesRead)
              snappyOut.flush()
            }
          }
          if (closed) {

            if (cbb.getAvailable > 0) {
              circInput.read(b, off, cbb.getAvailable)
            } else {
              -1
            }
          } else {
            circInput.read(b, off, len)
          }
        }
      }, s"zipped($input)"))
    } else {
      (NoCompression(), input)
    }
  }

  /**
   * @param input Given this stream of data
   * @param method And the way in which it was compressed, if any
   * @throws exceptions from reading a SafeInputStream
   * @return Return the unzipped version of the data
   */
  @Loggable(value = Loggable.DEBUG, limit = 200, unit = TimeUnit.MILLISECONDS, prepend = true)
  def decompress(input: SafeInputStream, method: CompressionMethod): SafeInputStream = {
    method match {
      case SnappyCompression() => new SafeInputStream(new SnappyInputStream(input), s"unzipped($input)")
      case NoCompression() => input
    }
  }

  /**
   * @param stream Bytes are read from this stream. Will definitely be closed when the function returns
   * @param numBytes Number of bytes to read
   * @throws exceptions from reading a SafeInputStream
   * @return After reading the bytes, try compressing them with snappy and return the compression ratio.
   */
  @Loggable(value = Loggable.DEBUG, limit = 5, unit = TimeUnit.SECONDS, prepend = true)
  private def compressionRatio(stream: SafeInputStream, numBytes: Int): Double = {
    val buffer = new Array[Byte](numBytes)
    val numBytesRead = try {
      stream.read(buffer, 0, numBytes)
    } finally {
      IOUtils.closeQuietly(stream)
    }
    if (numBytesRead <= 0) {
      0.0
    } else {
      val outputStream = new ByteArrayOutputStream()
      val snappy = new SnappyOutputStream(outputStream)
      snappy.write(buffer, 0, numBytesRead)
      snappy.close()
      outputStream.close()
      outputStream.size.toDouble / numBytesRead.toDouble
    }
  }
}
