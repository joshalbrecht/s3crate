package com.codexica.s3crate.filetree.history.snapshotstore.s3

import com.codexica.s3crate.filetree.history.snapshotstore.{DataBlob}
import java.io.{InputStream, FileOutputStream, BufferedOutputStream, File}
import java.security.MessageDigest
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID
import org.slf4j.LoggerFactory
import org.jets3t.service.utils.ServiceUtils
import com.jcabi.aspects.Loggable
import java.util.concurrent.TimeUnit

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
object S3BlobWriter {
  protected val logger = LoggerFactory.getLogger(getClass)

  /**
   * Writes data to one or more (possibly encrypted) files (as determined by chunkSize) and calculates the MD5 for each
   * and for the entire thing
   *
   * @param data The data to write out
   * @param chunkSize The maximum amount of data to upload in one part.
   * @return (The encryption details, a map from file -> the hash of the file, and a hash of all contents that were written out
   * @throws AssertionError if there is no data.
   */
  @Loggable(value = Loggable.DEBUG, limit = 1000, unit = TimeUnit.MILLISECONDS, prepend = true)
  def write(data: InputStream, uploadDirectory: File, chunkSize: Long): (List[(File, Array[Byte])], Array[Byte]) = {
    logger.debug("Writing all data to {} in chunks of {} bytes", data, chunkSize)

    def makeUploadDirectoryFile(): File = {
      new File(uploadDirectory, UUID.randomUUID().toString)
    }

    var finishedWritingData = false
    var currentFile = makeUploadDirectoryFile()
    var currentOutputStream = new BufferedOutputStream(new FileOutputStream(currentFile))
    var currentMd5Accumulator: MessageDigest = MessageDigest.getInstance("MD5")
    //TODO: feels kind of silly that we hash everything twice for the first file part (eg, most files), but there's no
    //convenient way around it that I see. Perhaps we could clone it before calling digest...
    val completeMd5Accumulator: MessageDigest = MessageDigest.getInstance("MD5")
    val files = new mutable.ListBuffer[(File, Array[Byte])]()
    val bytes = new Array[Byte](4096)
    var numBytesWritten = 0L
    while (!finishedWritingData) {
      val numBytesRead = data.read(bytes)
      if (numBytesRead == -1) {
        logger.debug("Finished reading {}", data)
        finishedWritingData = true
      } else {
        val numBytesToWrite: Int = if (numBytesWritten + numBytesRead > chunkSize) {
          logger.trace("Finishing off the current file because the input stream is larger than {}", chunkSize)
          (chunkSize - numBytesWritten).toInt
        } else {
          numBytesRead
        }
        logger.trace("Writing {} bytes", numBytesToWrite)
        currentOutputStream.write(bytes, 0, numBytesToWrite)
        currentMd5Accumulator.update(bytes, 0, numBytesToWrite)
        completeMd5Accumulator.update(bytes, 0, numBytesToWrite)
        numBytesWritten += numBytesToWrite

        //roll over the file and write the rest
        if (numBytesRead > numBytesToWrite) {
          currentOutputStream.close()
          val digest = currentMd5Accumulator.digest
          files.append((currentFile, digest))
          logger.debug("Wrote {} bytes to {}, resulting in digest of {}", numBytesWritten.toString,
            currentFile.getAbsolutePath, ServiceUtils.toBase64(digest))
          currentFile = makeUploadDirectoryFile()
          currentOutputStream = new BufferedOutputStream(new FileOutputStream(currentFile))
          currentMd5Accumulator = MessageDigest.getInstance("MD5")
          numBytesWritten = 0L

          val remainingBytes = numBytesRead - numBytesToWrite
          logger.trace("Writing remaining {} bytes from last read to new file", remainingBytes)
          currentOutputStream.write(bytes, numBytesToWrite, remainingBytes)
          currentMd5Accumulator.update(bytes, numBytesToWrite, remainingBytes)
          completeMd5Accumulator.update(bytes, numBytesToWrite, remainingBytes)
          numBytesWritten += remainingBytes
        }
      }
    }
    currentOutputStream.close()
    val digest = currentMd5Accumulator.digest
    if (currentFile.length() <= 0) {
      assert(currentFile.delete())
      assert(files.size > 0)
    } else {
      files.append((currentFile, digest))
    }
    logger.debug("Wrote {} bytes to {}, resulting in digest of {}", numBytesWritten.toString,
      currentFile.getAbsolutePath, ServiceUtils.toBase64(digest))

    (files.toList, completeMd5Accumulator.digest())
  }
}
