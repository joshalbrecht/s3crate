package com.codexica.s3crate.actors

import akka.actor.{Props, Actor}
import akka.routing.RoundRobinRouter
import com.codexica.s3crate.actors.messages.{TaskComplete, InitializationMessage, PathTask, WorkRequest}
import com.codexica.s3crate.common.{PathGenerator}
import com.codexica.s3crate.common.interfaces.{ReadableFileTree, FileTreeHistory}

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class Synchronizer(generator: PathGenerator, fileTree: ReadableFileTree, treeHistory: FileTreeHistory)
  extends Actor {

  val NUM_WORKERS = 4

  //Create the task master actor
  val taskMaster = context.actorOf(Props.apply({new TaskMaster(self, generator, fileTree, treeHistory)}), "TaskMaster")

  //Create a bunch of workers:
  val workers = context.actorOf(Props.apply({new SynchronizationWorker(self, fileTree, treeHistory)})
    .withRouter(RoundRobinRouter(nrOfInstances = NUM_WORKERS)), "WorkerPool")

  /**
   * Just send events back and forth appropriately
   */
  def receive = {
    case m: InitializationMessage => {
      //start the chain off by sending a bunch of work requests to the task master (one for each worker)
      (0 until NUM_WORKERS).foreach(i => workers ! TaskComplete())
    }

    //if we get work requests, send them along to the taskmaster.  This is silly.
    case m: WorkRequest => taskMaster forward m

    //if we get back any work, check that it makes sense, and then forward along
    case m: PathTask => {
      //TODO:  filter based on includes and excludes
      workers forward m
    }
  }
}
