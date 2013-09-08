package com.codexica.s3crate.actors

import akka.actor.{ActorRef, Actor}
import com.codexica.s3crate.actors.messages.{PathTask, WorkRequest}
import com.codexica.s3crate.filesystem.{PathGenerator, FilePathEvent, FileSystem}
import scala.util.{Failure, Success}
import com.codexica.s3crate.utils.FutureUtils
import com.google.common.base.Throwables

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class TaskMaster(synchronizer: ActorRef, generator: PathGenerator, sourceFileSystem: FileSystem, destFileSystem: FileSystem) extends Actor {

  import context.dispatcher

  def receive = {
    case WorkRequest() => {
      //no need to reply unless there is actually some work to do
      if (generator.hasNext) {
        val event = generator.next()
        //generate metadata for each of the filesystems
        val sourceMetaFuture = sourceFileSystem.snapshot(event.path)
        val destMetaFuture = destFileSystem.snapshot(event.path)
        FutureUtils.sequenceOrBailOut(List(sourceMetaFuture, destMetaFuture)).onComplete({
          case Success(List(sourceMeta, destMeta)) => {
            //respond with a PathTask
            sender ! PathTask(event.path, sourceMeta, destMeta)
          }
          case Failure(t) => {
            //TODO:  think about what to do here--file access exceptions?  Permissions? File deleted or moved?
            Throwables.propagate(t)
          }
        })
      }
    }
  }
}
