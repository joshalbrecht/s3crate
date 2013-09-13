package com.codexica.encryption

import play.api.libs.json.Json
import java.util.UUID
import com.codexica.common.UUIDFormat

/**
 * Refers to a public or private key in the local key store. The id is a UUID because that seems reasonable.
 *
 * Note that this does NOT contain any actual data about the key. Transferring this to another person, printing it out,
 * etc, would not make it possible for anyone to decrypt your information. The reference MUST be paired with a key
 * store that contains that keys to be useful for encryption or decryption.
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class KeyPairReference(id: UUID) {
  def privateKeyAlias = s"${id.toString}_private"
  def publicKeyAlias = s"${id.toString}_public"
}

object KeyPairReference {
  implicit val uuidFormat = new UUIDFormat()
  implicit val format = Json.format[KeyPairReference]
}