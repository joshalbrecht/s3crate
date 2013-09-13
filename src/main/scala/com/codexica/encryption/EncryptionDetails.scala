package com.codexica.encryption

import play.api.libs.json.Json
import com.codexica.s3crate.filetree.history.snapshotstore.RemoteFileSystemTypes
import com.codexica.common.ByteListFormat

/**
 * Represents the data used to encrypt a blob of binary data. Encrypting the blob requires a symmetric key, but that
 * is pointless if the symmetric key is stored unencrypted, so it is encrypted using the public key identified by
 * encodingKey, then the encrypted version is stored as encodedKey
 *
 * @param encodedKey The serialized SymmetricKey
 * @param encodingKey The id of the public key used to encrypt encodedKey
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class EncryptionDetails(
   encodedKey: List[Byte],
   encodingKey: KeyPairReference
 )

object EncryptionDetails {
  implicit val keyFormat = new ByteListFormat()
  implicit val format = Json.format[EncryptionDetails]
}
