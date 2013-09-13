package com.codexica.s3crate.filetree.history

import org.specs2.mutable.Specification
import play.api.libs.json.Json

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class CompressionMethodSpec extends Specification {
  "Serialization" should {
    "deserialize as exactly the same value" in {
      val method = NoCompression()
      Json.parse(Json.stringify(Json.toJson(method))).as[CompressionMethod] must be equalTo method
    }
  }
}
