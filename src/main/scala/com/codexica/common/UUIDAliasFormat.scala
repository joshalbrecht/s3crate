package com.codexica.common

import play.api.libs.json._
import java.util.UUID
import play.api.libs.json.JsString
import play.api.libs.json.JsSuccess

/**
 * Serialization logic strictly for aliased types that are actually UUIDs
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class UUIDAliasFormat[A <: UUID] extends Format[A] {
  def writes(o: A) = JsString(o.toString)
  def reads(json: JsValue): JsResult[A] = json match {
    case JsString(value) => JsSuccess(UUID.fromString(value).asInstanceOf[A])
    case _ => JsError("Expected a UUID")
  }
}

class UUIDFormat extends UUIDAliasFormat[UUID]
