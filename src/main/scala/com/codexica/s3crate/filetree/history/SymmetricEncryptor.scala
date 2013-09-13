package com.codexica.s3crate.filetree.history

import com.codexica.common.SafeInputStream
import com.codexica.encryption._
import play.api.libs.json.Json

/**
 * Transparently encrypt a stream if we were configured with a key to use for the encryption, otherwise effectively
 * a no-op
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class SymmetricEncryptor(keyIdOpt: Option[KeyPairReference], crypto: Cryptographer) {

  val encryptionKeyType = AES(512)

  /**
   * @param input The stream to (possibly) encrypt
   * @return A tuple of encryption details (if the stream was encrypted) and the transformed inputstream
   */
  def encrypt(input: SafeInputStream): (Option[EncryptionDetails], SafeInputStream) = {
    keyIdOpt match {
      case None => (None, input)
      case Some(keyId) => {
        val key = crypto.generateSymmetricKey(encryptionKeyType)
        val encryptedKey = encryptKey(key, keyId)
        val details = EncryptionDetails(encryptedKey.toList, keyId)
        (Option(details), new SafeInputStream(crypto.encryptStream(key, input), s"encrypted($input)"))
      }
    }
  }

  /**
   * @param input The encrypted stream
   * @param method The method which was used to encrypt the stream, if any
   * @return
   */
  def decrypt(input: SafeInputStream, method: Option[EncryptionDetails]): (SafeInputStream) = {
    method match {
      case None => input
      case Some(EncryptionDetails(encodedKey, keyId)) => {
        val key = decryptKey(encodedKey.toArray, keyId)
        new SafeInputStream(crypto.decryptStream(key, input), s"decrypted($input)")
      }
    }
  }

  /**
   * Encode the key using the public key so that only someone with the private key can decrypt the contents
   *
   * @param blobKey the key to encode
   * @param keyRef the public key to use to encode the symmetric key
   * @return The encrypted blobkey
   */
  private def encryptKey(blobKey: SymmetricKey, keyRef: KeyPairReference): Array[Byte] = {
    val keyData = Json.stringify(Json.toJson(blobKey)).getBytes("UTF-8")
    crypto.publicEncrypt(keyData, keyRef)
  }

  /**
   * Decode the key that had previously been encoded with encryptKey
   *
   * @param encodedKey The previous output of encryptKey
   * @param keyRef The private key to use for decrypting the data
   * @return The decrypted symmetric key
   */
  private def decryptKey(encodedKey: Array[Byte], keyRef: KeyPairReference): SymmetricKey = {
    val decryptedData = crypto.publicDecrypt(encodedKey, keyRef)
    Json.parse(decryptedData).as[SymmetricKey]
  }
}
