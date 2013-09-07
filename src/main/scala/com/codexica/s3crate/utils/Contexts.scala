package com.codexica.s3crate.utils

import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
object Contexts {
  val system = ActorSystem("s3crate")
  implicit val s3Operations: ExecutionContext = system.dispatchers.lookup("contexts.s3-operations")
  implicit val fileOperations: ExecutionContext = system.dispatchers.lookup("contexts.filesystem-operations")
  implicit val cpuOperations: ExecutionContext = system.dispatchers.lookup("contexts.cpu-operations")
}
