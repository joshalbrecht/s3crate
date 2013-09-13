package com.codexica.common

import play.api.libs.json._

/**
 * The best I could do for convenient serialization and deserialization of sealed traits
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class SealedTraitFormat[A](cases: A*) extends Format[A] {

  private val toJs: Map[A, String] = cases.map((c: A) => {
    (c, c.toString)
  }).toMap

  private val fromJs: Map[String, A] = cases.map((c: A) => {
    (c.toString, c)
  }).toMap

  def reads(json: JsValue): JsResult[A] = {
    json match {
      case x: JsString => {
        JsSuccess(fromJs(x.value))
      }
      case _ => {
        JsError("Cannot convert %s, choices are %s".format(json, toJs.values.toList))
      }
    }
  }

  def writes(o: A): JsValue = {
    JsString(toJs(o))
  }
}
