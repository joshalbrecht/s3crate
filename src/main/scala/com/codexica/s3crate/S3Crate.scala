package com.codexica.s3crate

import scala.concurrent.Await
import scala.util.{Failure, Success}
import com.google.common.base.Throwables
import akka.actor.Props
import com.codexica.s3crate.filetree.history.synchronization.InitializationMessage
import com.codexica.s3crate.filetree.history.synchronization.actors.Synchronizer
import com.codexica.s3crate.filetree.history.snapshotstore.s3.{S3Interface, S3Module, S3FileHistory}
import com.codexica.s3crate.filetree.local.LinuxFileTree
import java.io.File
import scala.concurrent.duration.Duration
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.FileUtils
import com.google.inject.Guice

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

    val injector = Guice.createInjector(new S3Module())
    val s3 = injector.getInstance(classOf[S3Interface])

    val fileTree = new LinuxFileTree(baseFolder, Contexts.fileOperations)
    implicit val ec = Contexts.system.dispatchers.defaultGlobalDispatcher
    val booted = S3FileHistory.initialize(Contexts.s3Operations, s3Prefix, s3).map(history => {
      direction match {
        case Upload() => {
          val synchronizer = Contexts.system.actorOf(Props.apply({new Synchronizer(fileTree.listen(), fileTree, history)}), "Synchronizer")
          synchronizer ! InitializationMessage()
        }
        case Download() => throw new NotImplementedError()
      }
    })
    Await.ready(booted, Duration.Inf)
    booted.value.get match {
      case Success(results) => {
        //TODO:  figure out how to shutdown in an orderly way
        Contexts.system.awaitTermination()
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



