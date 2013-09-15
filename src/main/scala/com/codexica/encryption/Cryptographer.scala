package com.codexica.encryption

import org.bouncycastle.jcajce.provider.keystore.bc.BcKeyStoreSpi.BouncyCastleStore
import java.io._
import com.codexica.common.MachineUtils
import com.codexica.common.SafeInputStream
import java.security._
import java.math.BigInteger
import org.bouncycastle.asn1.pkcs.{PrivateKeyInfo, PKCSObjectIdentifiers, RSAPublicKey, RSAPrivateKey}
import java.util.{UUID, Date}
import org.bouncycastle.cert.bc.BcX509v3CertificateBuilder
import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.bouncycastle.crypto.params._
import java.security.cert.CertificateFactory
import org.bouncycastle.crypto.{KeyGenerationParameters, AsymmetricCipherKeyPair}
import org.bouncycastle.crypto.generators.{DESedeKeyGenerator, RSAKeyPairGenerator}
import org.bouncycastle.operator.{DefaultSignatureAlgorithmIdentifierFinder, DefaultDigestAlgorithmIdentifierFinder}
import org.bouncycastle.asn1.x509.{SubjectPublicKeyInfo, AlgorithmIdentifier}
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder
import org.bouncycastle.asn1.DERNull
import org.bouncycastle.jcajce.provider.asymmetric.rsa.{BCRSAPublicKey, BCRSAPrivateCrtKey, KeyFactorySpi}
import org.bouncycastle.crypto.engines.{DESedeEngine, RSAEngine}
import org.bouncycastle.crypto.util.{PublicKeyFactory, PrivateKeyFactory}
import scala.collection.mutable.ListBuffer
import org.bouncycastle.crypto.digests.{SHA256Digest, SHA1Digest, SHA512Digest}
import org.bouncycastle.util.encoders.Hex
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.slf4j.LoggerFactory
import com.jcabi.aspects.Loggable
import java.util.concurrent.TimeUnit

/**
 * Contains all encryption logic. Stores your keys in a password-protected file on the local computer.
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class Cryptographer(file: File, password: Array[Char]) {

  //Initialization
  val logger = LoggerFactory.getLogger(getClass)
  //TODO: warn above if password is insufficiently long, link to xkcd
  if (Security.getProvider("BC") == null) {
    logger.info("Adding BouncyCastle cryptography provider")
    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider())
  }
  val store = new BouncyCastleStore()
  if (file.exists()) {
    logger.info(s"Loading keys from ${file.getAbsolutePath}")
    store.engineLoad(SafeInputStream.fromFile(file), password)
  }
  val secureRandom = initializeRandom()

  /**
   * Simple internal class for representing generated key pairs.
   *
   * @param priv The private Key, usable for decryption. Can be saved to the key store.
   * @param pub The public Key, usable for encryption. Can be saved to the key store.
   * @param certs The list of certificates to validate the key. Simply a chain of one, self-signed certificate
   */
  private case class KeyPair(priv: Key, pub: Key, certs: Array[java.security.cert.Certificate])

  /**
   * Generates a private key and saves it to the keystore immediately. Callers are never allowed to access the private
   * keys directly--they may only refer to them by id. This prevents you from doing something stupid and insecure,
   * like sending it in plain text across the wire or saving it into a file or any number of things.
   *
   * @param keyType The type of key you'd like to generate
   * @return The alias associated with the key.
   */
  @Loggable(value = Loggable.DEBUG, limit = 4, unit = TimeUnit.MINUTES, prepend = true)
  def generateAsymmetricKey(keyType: KeyPairType): KeyPairReference = {
    val keyPair = keyType match {
     case RSA(length) => generateRsaKeyPair(length)
    }
    val keyRef = KeyPairReference(UUID.randomUUID)
    store.engineSetKeyEntry(keyRef.privateKeyAlias, keyPair.priv, password, keyPair.certs)
    store.engineSetKeyEntry(keyRef.publicKeyAlias, keyPair.pub, password, keyPair.certs)
    save()
    keyRef
  }

  /**
   * @param data The data to encrypt
   * @param keyId The key to use to perform encryption (will use the public half)
   * @throws MissingKeyError if this key is not contained in the key store
   * @return The encrypted data. Not guaranteed to be the same size as the input.
   */
  @Loggable(value = Loggable.TRACE, limit = 1, unit = TimeUnit.MINUTES, prepend = true)
  def publicEncrypt(data: Array[Byte], keyId: KeyPairReference): Array[Byte] = {
    val key = store.engineGetKey(keyId.publicKeyAlias, password)
    if (key == null) throw new MissingKeyError(s"No corresponding public key for $keyId", null)
    val bcKey = key.asInstanceOf[BCRSAPublicKey]
    val publicKey = PublicKeyFactory.createKey(bcKey.getEncoded)
    processBlocks(data, publicKey, shouldEncrypt = true)
  }

  /**
   * @param data The data to decrypt
   * @param keyId The key to use to perform decryption (will use the private half)
   * @throws MissingKeyError if this key is not contained in the key store
   * @return The decrypted data. Not guaranteed to be the same size as the input.
   */
  @Loggable(value = Loggable.TRACE, limit = 1, unit = TimeUnit.MINUTES, prepend = true)
  def publicDecrypt(data: Array[Byte], keyId: KeyPairReference): Array[Byte] = {
    val key = store.engineGetKey(keyId.privateKeyAlias, password)
    if (key == null) throw new MissingKeyError(s"No corresponding private key for $keyId", null)
    val bcKey = key.asInstanceOf[BCRSAPrivateCrtKey]
    val privateKey = PrivateKeyFactory.createKey(bcKey.getEncoded)
    processBlocks(data, privateKey, shouldEncrypt = false)
  }

  /**
   * Save all of the key data out to a (possibly new) file, using a (possibly new) password.
   *
   * @param outputFile The file to save all keys to. If null, defaults to the original file that was loaded.
   * @param newPassword The password under which all keys should be saved. If null, defaults to the original password
   */
  @Loggable(value = Loggable.DEBUG, limit = 1, unit = TimeUnit.MINUTES, prepend = true)
  def save(outputFile: File = null, newPassword: Array[Char] = null) {
    val outFile = if (outputFile == null) file else outputFile
    val outPassword = if (newPassword == null) password else newPassword
    store.engineStore(new FileOutputStream(outFile), outPassword)
  }

  //TODO (josh): don't use this method. Think about how keys should be stored and transferred, or if they need to be.
  @Loggable(value = Loggable.TRACE, limit = 200, unit = TimeUnit.MILLISECONDS, prepend = true)
  def deletePrivateKey(keyId: KeyPairReference) {
    store.engineDeleteEntry(keyId.privateKeyAlias)
  }

  //TODO: use AES256 or AES512 instead of DES
  /**
   * @param keyType Given this specification of the type of key to create
   * @return A randomly generated symmetric key conformting to the specification in keyType
   */
  @Loggable(value = Loggable.TRACE, limit = 300, unit = TimeUnit.MILLISECONDS, prepend = true)
  def generateSymmetricKey(keyType: SymmetricKeyType): SymmetricKey = {
    val kgp: KeyGenerationParameters = new KeyGenerationParameters(secureRandom, DESedeParameters.DES_EDE_KEY_LENGTH * 8)
    val kg: DESedeKeyGenerator = new DESedeKeyGenerator
    kg.init(kgp)
    SymmetricKey(keyType, kg.generateKey.toList)
  }

  //Only exists as an example of how symmetric key encryption and decryption works
  @deprecated
  def encrypt(inFile: String, outFile: String, key: SymmetricKey) {
    val cipher = makeCipher()
    val in = new BufferedInputStream(new FileInputStream(inFile))
    val out = new BufferedOutputStream(new FileOutputStream(outFile))

    cipher.init(true, new KeyParameter(key.byteArray))
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

    in.close()
    out.flush()
    out.close()
  }

  //Only exists as an example of how symmetric key encryption and decryption works
  @deprecated
  def decrypt(inFile: String, outFile: String, key: SymmetricKey) {
    val cipher = makeCipher()
    val in = new BufferedInputStream(new FileInputStream(inFile))
    val out = new BufferedOutputStream(new FileOutputStream(outFile))

    cipher.init(false, new KeyParameter(key.byteArray))
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

    br.close()
    out.flush()
    out.close()
  }

  protected def makeCipher(): PaddedBufferedBlockCipher = {
    new PaddedBufferedBlockCipher(new CBCBlockCipher(new DESedeEngine))
  }

  //TODO:  implement! Note that you'll almost certainly have to read from one, write to a buffer, and then read from
  //the buffer. See Compressor for an example that does the same thing, but with compression (not encryption). And see
  //the second test in CryptographerIntegrationSpec for an example that uses the deprecated methods in this class
  //to do symmetric key encryption--basically you just have to adapt that to make the input and output into streams,
  //and then convert it to use AES instead of DES
  /**
   * Return an input stream that represents an encrypted version of the original input stream.
   *
   * @param key The key to use for encrypting the stream
   * @param data The data stream to encrypt
   * @return The encrypted version of the stream
   */
  @Loggable(value = Loggable.TRACE, limit = 100, unit = TimeUnit.MILLISECONDS, prepend = true)
  def encryptStream(key: SymmetricKey, data: InputStream): InputStream = new InputStream {
    val b = new Array[Byte](1)
    def read(): Int = { read(b, 0, 1); b(0)  }
    override def read(b: Array[Byte], off: Int, len: Int): Int = {
      throw new NotImplementedError()
    }
  }

  //TODO:  implement! Note that you'll almost certainly have to read from one, write to a buffer, and then read from
  //the buffer. See Compressor for an example that does the same thing, but with compression (not encryption). And see
  //the second test in CryptographerIntegrationSpec for an example that uses the deprecated methods in this class
  //to do symmetric key encryption--basically you just have to adapt that to make the input and output into streams,
  //and then convert it to use AES instead of DES
  /**
   * Return an input stream that represents a decrypted version of the original input stream.
   *
   * @param key The key to use for decrypting the stream
   * @param data The data stream to decrypt
   * @return The decrypted version of the stream
   */
  @Loggable(value = Loggable.TRACE, limit = 100, unit = TimeUnit.MILLISECONDS, prepend = true)
  def decryptStream(key: SymmetricKey, data: InputStream): InputStream = new InputStream {
    val b = new Array[Byte](1)
    def read(): Int = { read(b, 0, 1); b(0)  }
    override def read(b: Array[Byte], off: Int, len: Int): Int = {
      throw new NotImplementedError()
    }
  }

  /**
   * @return A SecureRandom that has been initialized with as much information as we can for the seed
   */
  @Loggable(value = Loggable.DEBUG, limit = 300, unit = TimeUnit.MILLISECONDS, prepend = true)
  private def initializeRandom(): SecureRandom = {
    //TODO: hash other random stuff in here, like current time, nanos running, thread name, pid, free memory, mac address, hostname, etc to increase entropy
    //TODO (josh): also, make a wrapper around this class, and ensure that it is periodically re-seeded: http://stackoverflow.com/questions/295628/securerandom-init-once-or-every-time-it-is-needed
    //TODO (josh): Ensure that all calls to bouncycastle pass in our wrapped SecureRandom, where possible, since creating one can randomly block forever: http://www.mattnworb.com/post/14312102134/the-dangers-of-java-security-securerandom
    val hashedPasswordAndStuff = password.map(_.toByte)
    val random = SecureRandom.getInstance("SHA1PRNG")
    random.setSeed(hashedPasswordAndStuff)
    random
  }

  /**
   * Effectively the number of rounds of MillerRabin to perform for verification that the generated key is prime.
   *
   * See here: http://stackoverflow.com/questions/3087049/bouncy-castle-rsa-keypair-generation-using-lightweight-api
   *
   * @param keyLength How long the key is. The longer the key, the more certain that you want to be that the number is
   *                  prime.
   * @return A number such that the probability of the key factor being prime is > (1 - 1/2<sup>certainty</sup>)
   */
  protected def getCertaintyForKeyLength(keyLength: Int): Int = {
    keyLength match {
      case x if x < 2048 => 80
      case x if x < 4096 => 160
      case _ => 320
    }
  }

  /**
   * Note that the high timeout will warn in case of much longer keys--a 8192 bit key took about 6 minutes to generate.
   *
   * @param keyLength The number of bits for the resulting key
   * @return An RSA public and private key, along with a self-signed certificate
   */
  @Loggable(value = Loggable.DEBUG, limit = 4, unit = TimeUnit.MINUTES, prepend = true)
  private def generateRsaKeyPair(keyLength: Int): KeyPair = {
    val MIN_KEY_LENGTH = 2048
    if (keyLength < MIN_KEY_LENGTH) {
      logger.warn(
        s"I would be uncomfortable with an RSA key less than $MIN_KEY_LENGTH bits. Consider increasing the length.")
    }

    val generator = new RSAKeyPairGenerator()
    val certainty = getCertaintyForKeyLength(keyLength)
    generator.init(new RSAKeyGenerationParameters(
      new BigInteger("10001", 16),
      secureRandom,
      keyLength,
      certainty
    ))
    val pair = generator.generateKeyPair()
    val certificates = generateSelfSignedRsaCertificateChain(pair)

    //convert the private key parameters into a functional Key
    val keyAlgo = new AlgorithmIdentifier(PKCSObjectIdentifiers.rsaEncryption, DERNull.INSTANCE)
    val keyParams = pair.getPrivate.asInstanceOf[RSAPrivateCrtKeyParameters]
    val privateKeyInfo = new PrivateKeyInfo(keyAlgo, new RSAPrivateKey(
      keyParams.getModulus,
      keyParams.getPublicExponent,
      keyParams.getExponent,
      keyParams.getP,
      keyParams.getQ,
      keyParams.getDP,
      keyParams.getDQ,
      keyParams.getQInv))
    val privateKey = new KeyFactorySpi().generatePrivate(privateKeyInfo)

    //convert the public key parameters into a functional Key
    val publicKeyInfo = new SubjectPublicKeyInfo(keyAlgo, new RSAPublicKey(
      keyParams.getModulus,
      keyParams.getPublicExponent
    ))
    val publicKey = new KeyFactorySpi().generatePublic(publicKeyInfo)

    KeyPair(privateKey, publicKey, certificates)
  }

  /**
   * Create a self-signed certificate using the key pair. This is only done so that it can be saved into the key store,
   * which requires a certificate for some reason that I'm not entirely sure of.
   *
   * @param pair The private/public key pair for signing
   * @return The list of certificates
   */
  @Loggable(value = Loggable.DEBUG, limit = 10, unit = TimeUnit.SECONDS, prepend = true)
  protected def generateSelfSignedRsaCertificateChain(pair: AsymmetricCipherKeyPair): Array[java.security.cert.Certificate] = {
    val sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA512withRSA")
    val digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId)
    val sigGen = new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(pair.getPrivate)
    val x500Name = new X500NameBuilder(BCStyle.INSTANCE)
      .addRDN(BCStyle.O, "(self-signed)")
      .addRDN(BCStyle.CN, MachineUtils.getHostName)
      .build()
    val oneThousandYears = 100L * 365L * 24L * 60L * 60L * 1000L
    val notBefore = new Date(System.currentTimeMillis() - oneThousandYears)
    val notAfter = new Date(System.currentTimeMillis() + oneThousandYears)
    val serial = BigInteger.valueOf(System.currentTimeMillis())
    val certGen = new BcX509v3CertificateBuilder(x500Name, serial, notBefore, notAfter, x500Name, pair.getPublic)
    val cert = certGen.build(sigGen).toASN1Structure
    val certificateFactory = CertificateFactory.getInstance("X.509")
    val javaCert = certificateFactory.generateCertificate(new ByteArrayInputStream(cert.getEncoded))
    val certificates = List[java.security.cert.Certificate](javaCert).toArray
    certificates
  }

  /**
   * Encrypt or decrypt data one block at a time. Proper encryption requires "padding", which is a misleading term. It's
   * really more like "armoring", using special validation to check that the message has not been altered and make it
   * harder to decrypt. We use OAEP, which I believe is the recommended standard.
   *
   * @param data The data to encrypt (or decrypt)
   * @param key The key to use for encryption (or decryption)
   * @param shouldEncrypt Whether to encrypt or decrypt
   * @return The fully padded and encrypted output (for encryption) or the plain text (for decryption)
   */
  @Loggable(value = Loggable.TRACE, limit = 1, unit = TimeUnit.MINUTES, prepend = true)
  protected def processBlocks(data: Array[Byte], key: AsymmetricKeyParameter, shouldEncrypt: Boolean): Array[Byte] = {
    val rsaEngine = new RSAEngine()
    //NOTE: there is a relationship between the key size and the size of these digests.
    //if the digest is too large relative to the key, you will not have enough information left in the block to encode
    //any actual data at all. I'm open to changing or reducing the size of these digests but making it too short seems
    //bad
    val digest = key.asInstanceOf[RSAKeyParameters].getModulus.bitLength() match {
      case x if x < 2047 => new SHA1Digest()
      case x if x < 4095 => new SHA256Digest()
      case _ => new SHA512Digest()
    }
    val engine = new org.bouncycastle.crypto.encodings.OAEPEncoding(rsaEngine, digest)
    engine.init(shouldEncrypt, key)
    val blockSize = engine.getInputBlockSize
    val output = ListBuffer[Byte]()
    var chunkPosition = 0
    while (chunkPosition < data.length) {
      val chunkSize = Math.min(blockSize, data.length - chunkPosition)
      output.appendAll(engine.processBlock(data, chunkPosition, chunkSize))
      chunkPosition += blockSize
    }
    output.toArray
  }
}
