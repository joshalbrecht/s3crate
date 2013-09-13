package com.codexica.encryption

import play.api.libs.json._

/**
 * All supported symmetric key types. Just one for now. Can add more in the future if someone really wants.
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
sealed trait SymmetricKeyType {
  def length: Int
}
case class AES(length: Int) extends SymmetricKeyType

object SymmetricKeyType {
  class SymmetricKeyTypeFormat extends Format[SymmetricKeyType] {
    def reads(json: JsValue): JsResult[SymmetricKeyType] = {
      json match {
        case x: JsObject => JsSuccess(AES(x.value("length").as[Int]))
        case _ => JsError(s"Cannot convert $json to SymmetricKeyType")
      }
    }
    def writes(o: SymmetricKeyType): JsValue = {
      JsObject(List("keyType" -> JsString("AES"), "length" -> JsNumber(o.length)))
    }
  }
  implicit val format = new SymmetricKeyTypeFormat()
}
