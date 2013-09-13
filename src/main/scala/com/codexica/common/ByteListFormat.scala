package com.codexica.common

import play.api.data.validation.ValidationError
import org.apache.commons.codec.binary.Base64
import play.api.libs.json._

/**
 * Write and read byte arrays as base64-encoded strings in javascript.
 *
 * Note that this operates on Lists of bytes because those are properly comparable in case classes.
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class ByteListFormat extends Format[List[Byte]] {

  def writes(o: List[Byte]) = {
    val base = Base64.encodeBase64(o.toArray)
    val value = new String(base)
    JsString(value)
  }

  def reads(json: JsValue) = json match {
    case JsString(value) => JsSuccess(Base64.decodeBase64(value.getBytes).toList)
    case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsstring"))))
  }
}
