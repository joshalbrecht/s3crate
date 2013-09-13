package com.codexica.encryption

import org.specs2.mutable.Specification
import play.api.libs.json.Json

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class SymmetricKeyTypeSpec extends Specification {
  "Serialization" should {
    "deserialize as exactly the same value" in {
      val method = AES(512)
      Json.parse(Json.stringify(Json.toJson(method))).as[SymmetricKeyType] must be equalTo method
    }
  }
}
