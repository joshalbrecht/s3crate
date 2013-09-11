package com.codexica.s3crate.filetree.history.snapshotstore.s3

import com.codexica.s3crate.filetree.history.snapshotstore.{DataBlob, BlobWriter}
import java.io.{InputStream, FileOutputStream, BufferedOutputStream, File}
import java.security.MessageDigest
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import java.util.UUID
import org.slf4j.LoggerFactory
import org.jets3t.service.utils.ServiceUtils

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class S3BlobWriter(s3: S3Interface, ec: ExecutionContext, uploadDirectory: File) extends BlobWriter {
  protected val logger = LoggerFactory.getLogger(getClass)

  implicit val context = ec

  override def save(data: InputStream, blobLocation: String, maxPartSize: Long): Future[Unit] = Future {
    logger.info("Writing out {} in chunks of maximum size = {} bytes", data: Any, maxPartSize: Any)
    val (fileHashes, completeMd5) = writeBlobToFiles(data, maxPartSize)
    if (fileHashes.size > 1) {
      s3.multipartUpload(fileHashes, blobLocation, completeMd5)
    } else {
      //have the normal uploader take care of it
      assert(completeMd5.toList == fileHashes.head._2.toList)
      s3.upload(fileHashes.head._1, blobLocation, fileHashes.head._2)
    }

    //clean up the leftover files:
    fileHashes.foreach(x => assert(x._1.delete()))
  }

  /**
   * Writes data to one or more (possibly encrypted) files (as determined by chunkSize) and calculates the MD5 for each
   * and for the entire thing
   *
   * @param data The data to write out
   * @param chunkSize The maximum amount of data to upload in one part.
   * @return (The encryption details, a map from file -> the hash of the file, and a hash of all contents that were written out
   * @throws Exception if there is no data.
   */
  private def writeBlobToFiles(data: InputStream, chunkSize: Long): (List[(File, Array[Byte])], Array[Byte]) = {
    logger.debug("Writing all data to {} in chunks of {} bytes", data, chunkSize)
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
    files.append((currentFile, digest))
    logger.debug("Wrote {} bytes to {}, resulting in digest of {}", numBytesWritten.toString,
      currentFile.getAbsolutePath, ServiceUtils.toBase64(digest))

    (files.toList, completeMd5Accumulator.digest())
  }

  private def makeUploadDirectoryFile(): File = {
    new File(uploadDirectory, UUID.randomUUID().toString)
  }
}
