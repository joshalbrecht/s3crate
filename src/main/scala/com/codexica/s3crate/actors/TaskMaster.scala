package com.codexica.s3crate.actors

import akka.actor.{ActorRef, Actor}
import com.codexica.s3crate.actors.messages.{PathTask, WorkRequest}
import scala.util.{Failure, Success}
import com.codexica.s3crate.utils.FutureUtils
import com.google.common.base.Throwables
import com.codexica.s3crate.common.{PathGenerator}
import com.codexica.s3crate.common.interfaces.{ReadableFileTree, FileTreeHistory}

//TODO:  this class should be responsible for making sure that EVERYTHING gets up there, logging errors if not, deleting old snapshots, etc
/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class TaskMaster(synchronizer: ActorRef, generator: PathGenerator, fileTree: ReadableFileTree, treeHistory: FileTreeHistory) extends Actor {

  import context.dispatcher

  def receive = {
    case WorkRequest() => {
      //no need to reply unless there is actually some work to do
      if (generator.hasNext) {
        val event = generator.next()
        //generate metadata for each of the filesystems
        val sourceMetaFuture = fileTree.metadata(event.path)
        val destMetaFuture = treeHistory.metadata(event.path)
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
