package com.codexica.s3crate.actors

import com.codexica.s3crate.filesystem.{PathGenerator, FileSystem, FilePathEvent}
import akka.actor.{Props, Actor}
import akka.routing.RoundRobinRouter
import com.codexica.s3crate.actors.messages.{PathTask, WorkRequest}

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class Synchronizer(generator: PathGenerator, sourceFileSystem: FileSystem, destFileSystem: FileSystem)
  extends Actor {

  val NUM_WORKERS = 4

  //Create the task master actor
  val taskMaster = context.actorOf(Props.apply({new TaskMaster(self, generator, sourceFileSystem, destFileSystem)}))

  //Create a bunch of workers:
  val workers = context.actorOf(Props.apply({new SynchronizationWorker(self, sourceFileSystem, destFileSystem)})
    .withRouter(RoundRobinRouter(nrOfInstances = NUM_WORKERS)))

  //start the chain off by sending a bunch of work requests to the task master (one for each worker)
  (0 until NUM_WORKERS).foreach(i => workers ! WorkRequest())

  /**
   * Just send events back and forth appropriately
   */
  def receive = {
    //if we get work requests, send them along to the taskmaster.  This is silly.
    case m: WorkRequest => taskMaster forward m

    //if we get back any work, check that it makes sense, and then forward along
    case m: PathTask => {
      //TODO:  filter based on includes and excludes
      workers forward m
    }
  }
}
