package com.codexica.s3crate

import com.codexica.s3crate.filesystem.{PathGenerator, FilePathEvent, LocalFileSystem}
import scala.concurrent.{Await, ExecutionContext, Promise, Future}
import scala.collection.generic.CanBuildFrom
import scala.util.{Failure, Success}
import com.google.common.base.Throwables
import com.codexica.s3crate.actors.Synchronizer
import akka.actor.{Props, Actor}
import scala.sys.Prop.Creator
import scala.sys.Prop
import com.codexica.s3crate.utils.{Encryption, Contexts, FutureUtils}
import org.jets3t.service.security.AWSCredentials
import org.jets3t.service.impl.rest.httpclient.RestS3Service
import org.jets3t.service.model.S3Bucket
import com.codexica.s3crate.filesystem.remote.{S3Interface, RemoteFileSystem}
import java.io.File
import org.apache.commons.io.FileUtils
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration.Duration
import com.codexica.s3crate.actors.messages.InitializationMessage

sealed trait SynchronizationDirection
case class Upload() extends SynchronizationDirection
case class Download() extends SynchronizationDirection

object S3Crate {

  //TODO:  Report any errors from a previous run
  def onStartup() {

  }

  def bootAndBlock() {
    onStartup()

    //val injector = Guice.createInjector(new DefaultModule())

    val direction: SynchronizationDirection = Upload()

    System.setProperty("config.resource", "/dev.conf")

    val config = ConfigFactory.parseFile(new File(FileUtils.getUserDirectory, "aws.conf")).withFallback(ConfigFactory.load())
    val awsAccessKey = config.getString("aws.access_key")
    val awsSecretKey = config.getString("aws.secret_key")
    val bucketName = "joshalbrecht.test"
    val s3Prefix = "test/cowdata"
    val metaPublicKey = Encryption.generatePublicKey()
    val metaPrivateKey = Encryption.generatePublicKey()
    val blobPublicKey = Encryption.generatePublicKey()
    val blobPrivateKey = Encryption.generatePublicKey()
    val baseFolder = "/home/cow/data"

    val awsCredentials = new AWSCredentials(awsAccessKey, awsSecretKey)
    val s3Service = new RestS3Service(awsCredentials)
    val bucket = s3Service.getOrCreateBucket(bucketName, S3Bucket.LOCATION_US_WEST)

    //create the S3 filesystem representation
    val remoteFiles = new RemoteFileSystem(new S3Interface(s3Service, bucket), s3Prefix, metaPublicKey, metaPrivateKey, blobPublicKey, blobPrivateKey)
    //tell it to synchronize
    val s3InitFuture = remoteFiles.start()

    //create the local filesystem representation
    val localFiles = new LocalFileSystem(new File(baseFolder))
    //tell it to synchronize
    val localInitFuture = localFiles.start()

    //wait for everything to be loaded
    implicit val ec = Contexts.system.dispatchers.defaultGlobalDispatcher
    val bootFuture = FutureUtils.sequenceOrBailOut(List(s3InitFuture, localInitFuture))
    Await.ready(bootFuture, Duration.Inf)
    bootFuture.value.get match {
      case Success(results) => {
        //combine all the paths
        val allPaths = results.fold(Set[FilePathEvent]())((a, b) => {a ++ b})
        //decide on source and destination file system
        val (sourceFileSystem, destFileSystem) = direction match {
          case Upload() => (localFiles, remoteFiles)
          case Download() => (remoteFiles, localFiles)
        }
        //make an path generator (takes events from local)
        val generator = new PathGenerator(allPaths, sourceFileSystem)
        //make a bunch of file-inspector actors that work through the paths and respond appropriately
        //val synchronizer = Akka.system.actorOf(injector.getInstance(classOf[Prop[Synchronizer]]), "Synchronizer")

        val synchronizer = Contexts.system.actorOf(Props.apply({new Synchronizer(generator, sourceFileSystem, destFileSystem)}), "Synchronizer")
        synchronizer ! InitializationMessage()
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
    bootAndBlock()
  }
}



