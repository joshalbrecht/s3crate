package com.codexica.s3crate.filetree.history

import com.codexica.common.{SafeInputStream, SafeLogSpecification}
import org.specs2.mutable.After
import java.io.{InputStream, ByteArrayInputStream, File}
import org.apache.commons.io.{IOUtils, FileUtils}
import java.util.{Random, UUID}
import org.slf4j.LoggerFactory

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class CompressorSpec extends SafeLogSpecification {

  trait Context extends BaseContext {
    val compressor = new Compressor()
    val dataLength = 32874
    val randomData = new Array[Byte](dataLength)
    new Random(27831).nextBytes(randomData)
    val incompressibleInputGenerator = () => {
      new SafeInputStream(new ByteArrayInputStream(randomData), "random data")
    }
    val simpleDataLength = 4096
    val simpleData = (0 to simpleDataLength).map(x => 0.toByte).toArray
    val compressibleInputGenerator = () => {
      new SafeInputStream(new ByteArrayInputStream(simpleData), "simple data")
    }
  }

  "Passing a stream to the compressor" should {
    "result in compression if the stream is compressible" in new Context {
      val (method, stream) = compressor.compress(compressibleInputGenerator)
      method must be equalTo SnappyCompression()
      IOUtils.toByteArray(stream).length must be lessThan simpleDataLength
      val decompressedStream = compressor.decompress(compressor.compress(compressibleInputGenerator)._2, method)
      IOUtils.toByteArray(decompressedStream).toList must be equalTo simpleData.toList
    }
    "not result in compression if the stream is random" in new Context {
      val (method, stream) = compressor.compress(incompressibleInputGenerator)
      method must be equalTo NoCompression()
      val decompressedStream = compressor.decompress(compressor.compress(incompressibleInputGenerator)._2, method)
      IOUtils.toByteArray(decompressedStream).toList must be equalTo randomData.toList
    }
    "work for all sorts of sizes of compressible input data (boundary conditions with buffer sizes)" in new Context {
      List(0, 1, 4095, 4096, 4097, 34598, 500000).foreach(dataSize => {
        val rand = new Random(39875)
        val currentData = (0 to dataSize).map(x => {
          rand.nextInt(8).toByte
        }).toArray
        val (method, stream) = compressor.compress(() => {
          new SafeInputStream(new ByteArrayInputStream(currentData), "data")
        })
        val decompressedStream = compressor.decompress(stream, method)
        IOUtils.toByteArray(decompressedStream).toList must be equalTo currentData.toList
      })
    }
  }
}
