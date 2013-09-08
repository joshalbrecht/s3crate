package com.codexica.s3crate.filesystem.remote

import play.api.libs.json.Json
import org.specs2.mutable.Specification
import com.codexica.encryption.{SimpleEncryption, EncryptionMethod}

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class EncryptionMethodSpec extends Specification {
  "Serialization" should {
    "deserialize as exactly the same value" in {
      val method = SimpleEncryption()
      Json.parse(Json.stringify(Json.toJson(method))).as[EncryptionMethod] must be equalTo method
    }
  }
}
