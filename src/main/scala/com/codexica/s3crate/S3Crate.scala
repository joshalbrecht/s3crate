package com.codexica.s3crate

import scala.concurrent.{Future, Await}
import scala.util.{Failure, Success}
import com.google.common.base.Throwables
import akka.actor.{ActorSystem, Props}
import com.codexica.s3crate.filetree.history.synchronization.InitializationMessage
import com.codexica.s3crate.filetree.history.synchronization.actors.Synchronizer
import com.codexica.s3crate.filetree.history.snapshotstore.s3.{S3, S3Interface, S3Module, S3FileHistory}
import com.codexica.s3crate.filetree.local.LinuxFileTree
import java.io.File
import scala.concurrent.duration.Duration
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FileUtils
import com.google.inject.{Key, Guice}
import com.codexica.s3crate.filetree.history.FileTreeHistory

object S3Crate {

  //TODO:  Report any errors from a previous run
  def onStartup() {

  }

  def bootAndBlock() {
    onStartup()

    //TODO: parameterize and inject these
    val direction: SynchronizationDirection = Upload()
    val s3Prefix = "test/cowdata"
    val baseFolder = new File("/home/cow/data")

    val injector = Guice.createInjector(new ActorModule(), new S3Module(s3Prefix))
    val s3InitializedFuture = injector.getInstance(Key.get(classOf[Future[FileTreeHistory]], classOf[S3]))
    val actorSystem = injector.getInstance(classOf[ActorSystem])

    //TODO:  maybe make this a default context or something? Or maybe use it below?
    val cpuOperations = actorSystem.dispatchers.lookup("contexts.cpu-operations")

    val fileOperations = actorSystem.dispatchers.lookup("contexts.filesystem-operations")
    val fileTree = new LinuxFileTree(baseFolder, fileOperations)
    implicit val ec = actorSystem.dispatchers.defaultGlobalDispatcher
    val booted = s3InitializedFuture.map(history => {
      direction match {
        case Upload() => {
          val synchronizer = actorSystem.actorOf(Props.apply({new Synchronizer(fileTree.listen(), fileTree, history)}), "Synchronizer")
          synchronizer ! InitializationMessage()
        }
        case Download() => throw new NotImplementedError()
      }
    })
    Await.ready(booted, Duration.Inf)
    booted.value.get match {
      case Success(results) => {
        //TODO:  figure out how to shutdown in an orderly way
        actorSystem.awaitTermination()
      }
      case Failure(t) => {
        Throwables.propagate(t)
      }
    }
    val x = 4
  }

  def main(args: Array[String]) {
    //TODO: set this in a more general way
    //very first thing we do is set the configuration location so that config can be loaded
    System.setProperty("config.resource", new File(FileUtils.getUserDirectory, "aws.conf").getAbsolutePath)

    bootAndBlock()
  }
}



