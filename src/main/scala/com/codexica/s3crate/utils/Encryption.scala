package com.codexica.s3crate.utils

import java.io._
import java.security.SecureRandom
import org.bouncycastle.crypto.KeyGenerationParameters
import org.bouncycastle.crypto.params.{KeyParameter, DESedeParameters}
import org.bouncycastle.crypto.generators.DESedeKeyGenerator
import org.bouncycastle.util.encoders.Hex
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.engines.DESedeEngine

//TODO: securely generate and store the keys
//TODO: change to binary
//TODO: change to AES256 or 512
//TODO: generate public keys as well

/**
 * Adapted from DESExample in BouncyCastle
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
object Encryption {
  def publicDecrypt(cipherText: Array[Byte], privateKey: Array[Byte]): Array[Byte] = {
    throw new NotImplementedError()
  }

  def publicEncrypt(content: Array[Byte], publicKey: Array[Byte]): Array[Byte] = {
    throw new NotImplementedError()
  }

  def generatePublicKey(): Array[Byte] = {
    //throw new NotImplementedError()
    "dsafadsfsd".getBytes
  }

  def generateSymmetricKey(): Array[Byte] = {
    var sr: SecureRandom = null
    try {
      sr = new SecureRandom
      sr.setSeed("www.bouncycastle.org".getBytes)
    }
    catch {
      case nsa: Exception => {
        System.err.println("Hmmm, no SHA1PRNG, you need the " + "Sun implementation")
        System.exit(1)
      }
    }
    val kgp: KeyGenerationParameters = new KeyGenerationParameters(sr, DESedeParameters.DES_EDE_KEY_LENGTH * 8)
    val kg: DESedeKeyGenerator = new DESedeKeyGenerator
    kg.init(kgp)
    kg.generateKey
  }

  def saveKey(key: Array[Byte], file: File) {
    val keystream: BufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file))
    val keyhex = Hex.encode(key)
    keystream.write(keyhex, 0, keyhex.length)
    keystream.flush
    keystream.close
  }

  def loadKey(file: File): Array[Byte] = {
    val keystream: BufferedInputStream = new BufferedInputStream(new FileInputStream(file))
    val len: Int = keystream.available
    val keyhex: Array[Byte] = new Array[Byte](len)
    keystream.read(keyhex, 0, len)
    Hex.decode(keyhex)
  }

  def encrypt(inFile: String, outFile: String, key: Array[Byte]) {
    val cipher = makeCipher()
    val in = new BufferedInputStream(new FileInputStream(inFile))
    val out = new BufferedOutputStream(new FileOutputStream(outFile))

    cipher.init(true, new KeyParameter(key))
    val inBlockSize: Int = 47
    val outBlockSize: Int = cipher.getOutputSize(inBlockSize)
    val inblock: Array[Byte] = new Array[Byte](inBlockSize)
    val outblock: Array[Byte] = new Array[Byte](outBlockSize)

    var inL = 0
    var outL = 0
    var rv: Array[Byte] = null
    while (({inL = in.read(inblock, 0, inBlockSize); inL}) > 0) {
      outL = cipher.processBytes(inblock, 0, inL, outblock, 0)
      if (outL > 0) {
        rv = Hex.encode(outblock, 0, outL)
        out.write(rv, 0, rv.length)
        out.write('\n')
      }
    }

    outL = cipher.doFinal(outblock, 0)
    if (outL > 0) {
      rv = Hex.encode(outblock, 0, outL)
      out.write(rv, 0, rv.length)
      out.write('\n')
    }

    in.close
    out.flush
    out.close
  }

  def decrypt(inFile: String, outFile: String, key: Array[Byte]) {
    val cipher = makeCipher()
    val in = new BufferedInputStream(new FileInputStream(inFile))
    val out = new BufferedOutputStream(new FileOutputStream(outFile))

    cipher.init(false, new KeyParameter(key))
    val br: BufferedReader = new BufferedReader(new InputStreamReader(in))

    var outL: Int = 0
    var inblock: Array[Byte] = null
    var outblock: Array[Byte] = null
    var rv: String = null
    while (({rv = br.readLine; rv}) != null) {
      inblock = Hex.decode(rv)
      outblock = new Array[Byte](cipher.getOutputSize(inblock.length))
      outL = cipher.processBytes(inblock, 0, inblock.length, outblock, 0)
      if (outL > 0) {
        out.write(outblock, 0, outL)
      }
    }

    outL = cipher.doFinal(outblock, 0)
    if (outL > 0) {
      out.write(outblock, 0, outL)
    }

    br.close
    out.flush
    out.close
  }

  def encryptStream(key: Array[Byte], data: InputStream): InputStream = new InputStream {
    def read(): Int = {
      throw new NotImplementedError()
    }
  }

  protected def makeCipher(): PaddedBufferedBlockCipher = {
    new PaddedBufferedBlockCipher(new CBCBlockCipher(new DESedeEngine))
  }
}
