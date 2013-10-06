package com.codexica.s3crate

import com.codexica.s3crate.filetree.history.FileTreeHistory
import com.codexica.s3crate.filetree.history.snapshotstore.s3.{S3, S3Module}
import com.codexica.s3crate.filetree.history.synchronization.Historian
import com.codexica.s3crate.filetree.local.LinuxFileTree
import com.google.inject.{TypeLiteral, Key, Guice}
import java.io.File
import org.apache.commons.io.FileUtils
import scala.concurrent.{Await, ExecutionContext, Future}
import java.util.concurrent.Executors
import com.codexica.common.{FutureUtils, LogUtils}
import org.slf4j.LoggerFactory
import scala.concurrent.duration.Duration

object S3Crate {

  //TODO:  Report any errors from a previous run
  def onStartup() {

  }

  def bootAndBlock() {
    LogUtils.waitForStupidLogging()
    val logger = LoggerFactory.getLogger(getClass)

    try {
      onStartup()

      //TODO: parameterize and inject these
      val direction: SynchronizationDirection = Upload()
      val s3Prefix = "test/cowdata"
      val baseFolder = new File("/home/cow/data")
      //TODO: before this point, validate that the folder exists, is a folder, etc? Perhaps the constructor should check that, and s3 should check that it can connect too?

      val injector = Guice.createInjector(new S3Module(s3Prefix))
      val s3InitializedFuture = injector
        .getInstance(Key.get(new TypeLiteral[Future[FileTreeHistory]](){}, classOf[S3]))

      implicit val ec = FutureUtils.makeExecutor("bootAndBlock", 1)
      val fileOperations = FutureUtils.makeExecutor("filesystem", 16)
      val historianContext = FutureUtils.makeExecutor("historian", 8)
      val fileTree = new LinuxFileTree(baseFolder, fileOperations)
      val finalResult = s3InitializedFuture.map(history => {
        direction match {
          case Upload() => {
            val historian = new Historian(fileTree, history, historianContext)
            while (true) {
              println(historian.status)
              Thread.sleep(5 * 1000)
            }
          }
          case Download() => throw new NotImplementedError()
        }
      })
      Await.result(finalResult, Duration.Inf)
    } catch {
      case t: Throwable => {
        logger.error("S3Crate failed!", t)
      }
    }
  }

  def main(args: Array[String]) {
    //TODO: set this in a more general way
    //very first thing we do is set the configuration location so that config can be loaded
    val configPath = new File(FileUtils.getUserDirectory, "aws.conf").getAbsolutePath
    System.setProperty("config.file", configPath)

    bootAndBlock()
  }
}



