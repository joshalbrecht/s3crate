package com.codexica.encryption

import play.api.libs.json.Json
import com.codexica.common.ByteListFormat

/**
 * Contains all data necessary for encryption and decryption with this symmetric key
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
case class SymmetricKey(keyType: SymmetricKeyType, data: List[Byte]) {
  def byteArray = data.toArray
}

object SymmetricKey {
  implicit val dataFormat = new ByteListFormat()
  implicit val format = Json.format[SymmetricKey]
}
