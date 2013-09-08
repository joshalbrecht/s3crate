package com.codexica.s3crate.filetree.history.synchronization.actors

import akka.actor.{ReceiveTimeout, ActorRef, Actor}
import scala.concurrent.Future
import scala.util.{Success, Failure}
import scala.concurrent.duration._
import com.codexica.s3crate.filetree.ReadableFileTree
import com.codexica.s3crate.filetree.history.{FilePathState, FileTreeHistory}
import com.codexica.s3crate.filetree.history.synchronization.{WorkRequest, TaskComplete, PathTask}

/**
 * Keeps a list of pending tasks. Should not grow very large, since we
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class SynchronizationWorker(synchronizer: ActorRef, fileTree: ReadableFileTree, treeHistory: FileTreeHistory) extends Actor {

  private var pendingTasks = List[PathTask]()
  private var working = false

  import context.dispatcher
  context.setReceiveTimeout(30 * 1000 milliseconds)

  def receive = {
    //when nothing has happened in a while and we're not busy, start working, or request some more work if we don't
    //have any more work
    case m: ReceiveTimeout => {
      if (!working) {
        if (pendingTasks.size > 0) {
          handleTask()
        } else {
          synchronizer ! WorkRequest()
        }
      }
    }
    //when we're done working, begin the next task if there is one, otherwise request more work
    case m: TaskComplete => {
      working = false
      handleTask()
      if (!working) {
        synchronizer ! WorkRequest()
      }
    }
    //if we got notified of some new work, add it to the list and start working if we're not already
    case m: PathTask => {
      pendingTasks = pendingTasks ::: List(m)
      if (!working) {
        handleTask()
      }
    }
  }

  private def handleTask() {
    if (pendingTasks.size > 0) {
      val PathTask(path, source, dest) = pendingTasks.head
      pendingTasks = pendingTasks.tail

      val operation = if (source.exists) {
        if (dest.exists) {
          sync(source, dest)
        } else {
          onDestMissing(source)
        }
      } else {
        if (dest.exists) {
          onSourceMissing(dest)
        } else {
          //TODO:  think about how we could get here and what to do
          //this should probably throw an exception--it's stupid that you pass two empty paths as work...
          throw new RuntimeException("Path is not defined in either source: " + path)
        }
      }
      working = true
      operation.onComplete(result => {
        //send ourselves a message when we're done so that we update properly
        self ! TaskComplete()
        result match {
          case Success(_) => {}
          case Failure(t) => {
            //TODO:  what should we do if the task failed?  retry? send it back to the dispatcher?
            throw t
          }
        }
      })
    }
  }

  /**
   * Ensure that the two snapshots are synchronized
   *
   * @param source The source snapshot
   * @param dest The destination snapshot
   */
  private def sync(source: FilePathState, dest: FilePathState): Future[Unit] = {
    if (source == dest) {
      Future.successful()
    } else {
      writeToDest(source)
    }
  }

  /**
   * Called when the source is missing. Verify that the destination is in the deleted state
   */
  private def onSourceMissing(dest: FilePathState): Future[Unit] =  {
    if (dest.exists) {
      //TODO: stop mapping to Unit and actually send the result back to the task master so that it can mark this as
      //completed (here and below)
      treeHistory.delete(dest.path).map(x => Unit)
    } else {
      Future.successful()
    }
  }

  /**
   * Called when the destination file is missing. Just go create it.
   */
  private def onDestMissing(source: FilePathState): Future[Unit] =  {
    writeToDest(source)
  }

  private def writeToDest(source: FilePathState): Future[Unit] = {
    if (source.exists) {
      treeHistory.update(source.path, fileTree).map(x => Unit)
    } else {
      Future.successful()
    }
  }
}
