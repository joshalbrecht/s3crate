package com.codexica.s3crate.utils

import play.api.libs.json._
import org.apache.commons.codec.binary.Base64
import play.api.data.validation.ValidationError
import com.codexica.s3crate.filesystem.remote.RemoteFileSystemTypes

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class KeyDataFormat extends Format[RemoteFileSystemTypes.KeyData] {

  def writes(o: RemoteFileSystemTypes.KeyData) = {
    val base = Base64.encodeBase64(o.toArray)
    val value = new String(base)
    JsString(value)
  }

  def reads(json: JsValue) = json match {
    case JsString(value) => JsSuccess(Base64.decodeBase64(value.getBytes).toList)
    case _ => JsError(Seq(JsPath() -> Seq(ValidationError("validate.error.expected.jsstring"))))
  }
}
