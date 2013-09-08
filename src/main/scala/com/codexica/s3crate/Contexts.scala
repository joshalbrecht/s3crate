package com.codexica.s3crate

import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
object Contexts {
  //TODO:  use injection and make this not static
  val system = ActorSystem("s3crate", ConfigFactory.load().getConfig("com.codexica.s3crate"))
  implicit val s3Operations: ExecutionContext = system.dispatchers.lookup("contexts.s3-operations")
  implicit val fileOperations: ExecutionContext = system.dispatchers.lookup("contexts.filesystem-operations")
  implicit val cpuOperations: ExecutionContext = system.dispatchers.lookup("contexts.cpu-operations")
}
