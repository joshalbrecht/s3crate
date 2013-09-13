package com.codexica.encryption

import org.specs2.mutable.Specification
import play.api.libs.json.Json

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class KeyPairTypeSpec extends Specification {
  "Serialization" should {
    "deserialize as exactly the same value" in {
      val method = RSA(512)
      Json.parse(Json.stringify(Json.toJson(method))).as[KeyPairType] must be equalTo method
    }
  }
}
