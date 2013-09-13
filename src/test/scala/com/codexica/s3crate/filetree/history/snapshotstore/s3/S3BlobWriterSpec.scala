package com.codexica.s3crate.filetree.history.snapshotstore.s3

import java.io.ByteArrayInputStream
import org.scalamock.specs2.MockFactory
import org.jets3t.service.utils.ServiceUtils
import org.apache.commons.io.FileUtils
import java.util.Random
import com.codexica.common.SafeLogSpecification

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class S3BlobWriterSpec extends SafeLogSpecification {

  trait Context extends BaseContext {
    val dataLength = 34985
    val bytes = new Array[Byte](dataLength)
    new Random(1337).nextBytes(bytes)
    val fullDataHash = ServiceUtils.computeMD5Hash(bytes)
    val inputStream = new ByteArrayInputStream(bytes)
  }

  "saving a regular file" should {
    "create a list with one file that matches" in new Context {
      val (fileHashes, completeHash) = S3BlobWriter.write(inputStream, FileUtils.getTempDirectory, dataLength * 2)
      completeHash.toList must be equalTo fullDataHash.toList
      fileHashes.size must be equalTo 1
      val (file, hash) = fileHashes.head
      FileUtils.readFileToByteArray(file).toList must be equalTo bytes.toList
      hash.toList must be equalTo fullDataHash.toList
    }
  }

  "saving a large file" should {
    "create a list of multiple files with matching hashes" in new Context {
      val secondFileSize = 2111
      val firstFileSize = dataLength - secondFileSize
      val (fileHashes, completeHash) = S3BlobWriter.write(inputStream, FileUtils.getTempDirectory, firstFileSize)
      completeHash.toList must be equalTo fullDataHash.toList
      fileHashes.size must be equalTo 2

      val (firstFile, firstHash) = fileHashes.head
      FileUtils.readFileToByteArray(firstFile).toList must be equalTo bytes.toList.take(firstFileSize)
      firstHash.toList must be equalTo ServiceUtils.computeMD5Hash(bytes.toList.take(firstFileSize).toArray).toList

      val (secondFile, secondHash) = fileHashes.drop(1).head
      FileUtils.readFileToByteArray(secondFile).toList must be equalTo bytes.toList.drop(firstFileSize)
      secondHash.toList must be equalTo ServiceUtils.computeMD5Hash(bytes.toList.drop(firstFileSize).toArray).toList
    }
  }

}
