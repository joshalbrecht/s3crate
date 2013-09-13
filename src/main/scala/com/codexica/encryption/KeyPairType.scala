package com.codexica.encryption

import play.api.libs.json._

/**
 * All supported asymmetric key types. Just one for now. Can add more in the future if someone really wants.
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
sealed trait KeyPairType {
  def length: Int
}
case class RSA(length: Int) extends KeyPairType

object KeyPairType {
  class KeyPairTypeFormat extends Format[KeyPairType] {
    def reads(json: JsValue): JsResult[KeyPairType] = {
      json match {
        case x: JsObject => JsSuccess(RSA(x.value("length").as[Int]))
        case _ => JsError(s"Cannot convert $json to SymmetricKeyType")
      }
    }
    def writes(o: KeyPairType): JsValue = {
      JsObject(List("keyType" -> JsString("RSA"), "length" -> JsNumber(o.length)))
    }
  }
  implicit val format = new KeyPairTypeFormat()
}