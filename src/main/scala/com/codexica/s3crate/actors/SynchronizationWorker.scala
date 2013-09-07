package com.codexica.s3crate.actors

import akka.actor.{ReceiveTimeout, ActorRef, Actor}
import com.codexica.s3crate.actors.messages.{TaskComplete, WorkRequest, PathTask}
import com.codexica.s3crate.filesystem.{ReadableFile, Deleted, FileSnapshot, FileSystem}
import scala.concurrent.Future
import scala.util.{Success, Failure}
import com.google.common.base.Throwables
import java.io.ByteArrayInputStream
import scala.concurrent.duration._

/**
 * Keeps a list of pending tasks. Should not grow very large, since we
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class SynchronizationWorker(synchronizer: ActorRef, sourceFileSystem: FileSystem, destFileSystem: FileSystem) extends Actor {

  private var pendingTasks = List[PathTask]()
  private var working = false

  import context.dispatcher
  context.setReceiveTimeout(30 * 1000 milliseconds)

  def receive = {
    //when nothing has happened in a while and we're not busy, start working, or request some more work if we don't
    //have any more work
    case ReceiveTimeout => {
      if (!working) {
        if (pendingTasks.size > 0) {
          handleTask()
        } else {
          synchronizer ! WorkRequest()
        }
      }
    }
    //when we're done working, begin the next task if there is one, otherwise request more work
    case TaskComplete => {
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
      val PathTask(path, sourceOpt, destOpt) = pendingTasks.head
      pendingTasks = pendingTasks.tail

      val operation = sourceOpt match {
        case Some(source) => {
          destOpt match {
            case Some(dest) => {
              sync(source, dest)
            }
            case None => {
              onDestMissing(source)
            }
          }
        }
        case None => {
          destOpt match {
            case Some(dest) => {
              onSourceMissing(dest)
            }
            case None => {
              //TODO:  think about how we could get here and what to do
              //this should probably throw an exception--it's stupid that you pass two empty paths as work...
              throw new RuntimeException("Path is not defined in either source: " + path)
            }
          }
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
            Throwables.propagate(t)
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
  private def sync(source: FileSnapshot, dest: FileSnapshot): Future[Unit] = {
    if (source == dest) {
      Future.successful()
    } else {
      writeToDest(source)
    }
  }

  /**
   * Called when the source is missing. Verify that the destination is in the deleted state
   */
  private def onSourceMissing(dest: FileSnapshot): Future[Unit] =  {
    dest.state match {
      case Deleted() => Future.successful()
      case _ => {
        destFileSystem.write(new ReadableFile(() => {new ByteArrayInputStream("".getBytes)}, 0), dest.copy(state = Deleted()))
      }
    }
  }

  /**
   * Called when the destination file is missing. Just go create it.
   */
  private def onDestMissing(source: FileSnapshot): Future[Unit] =  {
    writeToDest(source)
  }

  private def writeToDest(source: FileSnapshot): Future[Unit] = {
    source.state match {
      case Deleted() => Future.successful()
      case _ => {
        destFileSystem.write(sourceFileSystem.read(source.path), source)
      }
    }
  }
}
