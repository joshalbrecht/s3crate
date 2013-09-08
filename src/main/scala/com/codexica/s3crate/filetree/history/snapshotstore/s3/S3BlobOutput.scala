package com.codexica.s3crate.filetree.history.snapshotstore.s3

import com.codexica.s3crate.filetree.history.snapshotstore.{DataBlob, BlobOutput}
import com.codexica.encryption.EncryptionDetails
import java.io.{InputStream, FileOutputStream, BufferedOutputStream, File}
import java.security.MessageDigest
import scala.collection.mutable
import scala.concurrent.Future

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class S3BlobOutput(s3: S3Interface) extends BlobOutput {

  //file bigger than 128MB? Don't try to upload that as one big thing, it's going to take forever and fail.
  private val MULTIPART_CUTOFF_BYTES = 128 * 1024 * 1024

  override def save(data: InputStream, wasZipped: Boolean, encryptionDetails: EncryptionDetails): Future[DataBlob] = Future {

    //store blob in a random location, really doesn't matter
    val blobLocation = randomBlobLocation()

    //write out all of the data for upload and calculate the md5 for each piece
    val (fileHashes, completeMd5) = writeBlobToFiles(data, MULTIPART_CUTOFF_BYTES)
    //if this file is huge:
    if (fileHashes.size > 1) {
      //let the multi-part uploader take care of the rest
      s3.multipartUpload(fileHashes, blobLocation, completeMd5)
      //otherwise it should be pretty easy. Just upload directly
    } else {
      //have the normal uploader take care of it
      assert(completeMd5 == fileHashes.values.head)
      s3.upload(fileHashes.keys.head, blobLocation, fileHashes.values.head)
    }

    //clean up the leftover files:
    fileHashes.keys.foreach((file: File) => assert(file.delete()))

    DataBlob(blobLocation, encryptionDetails, wasZipped)
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
  private def writeBlobToFiles(data: InputStream, chunkSize: Long): (Map[File, Array[Byte]], Array[Byte]) = {
    var finishedWritingData = false
    var currentFile = makeUploadDirectoryFile()
    var currentOutputStream = new BufferedOutputStream(new FileOutputStream(currentFile))
    var currentMd5Accumulator: MessageDigest = MessageDigest.getInstance("MD5")
    //TODO: feels kind of silly that we hash everything twice for the first file part (eg, most files), but there's no
    //convenient way around it that I see. Perhaps we could clone it before calling digest...
    val completeMd5Accumulator: MessageDigest = MessageDigest.getInstance("MD5")
    val files = new mutable.HashMap[File, Array[Byte]]()
    val bytes = new Array[Byte](4096)
    var numBytesWritten = 0L
    while (!finishedWritingData) {
      val numBytesRead = data.read(bytes)
      if (numBytesRead == -1) {
        finishedWritingData = true
      } else {
        val numBytesToWrite: Int = if (numBytesWritten + numBytesRead > chunkSize) {
          (chunkSize - numBytesWritten).toInt
        } else {
          numBytesRead
        }
        currentOutputStream.write(bytes, 0, numBytesToWrite)
        currentMd5Accumulator.update(bytes, 0, numBytesToWrite)
        completeMd5Accumulator.update(bytes, 0, numBytesToWrite)
        numBytesWritten += numBytesToWrite

        //roll over the file and write the rest
        if (numBytesRead > numBytesToWrite) {
          currentOutputStream.close()
          files += (currentFile -> currentMd5Accumulator.digest)
          currentFile = makeUploadDirectoryFile()
          currentOutputStream = new BufferedOutputStream(new FileOutputStream(currentFile))
          currentMd5Accumulator = MessageDigest.getInstance("MD5")
          numBytesWritten = 0L

          val remainingBytes = numBytesRead - numBytesToWrite
          currentOutputStream.write(bytes, numBytesToWrite, remainingBytes)
          currentMd5Accumulator.update(bytes, numBytesToWrite, remainingBytes)
          completeMd5Accumulator.update(bytes, numBytesToWrite, remainingBytes)
          numBytesWritten += remainingBytes
        }
      }
    }
    currentOutputStream.close()
    files += (currentFile -> currentMd5Accumulator.digest)

    (files.toMap, completeMd5Accumulator.digest())
  }

  private def randomBlobLocation(): String = {
    throw new NotImplementedError()
  }

  private def makeUploadDirectoryFile(): File = {
    throw new NotImplementedError()
  }
}
