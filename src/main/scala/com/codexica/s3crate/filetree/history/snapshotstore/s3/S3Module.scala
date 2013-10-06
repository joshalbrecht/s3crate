package com.codexica.s3crate.filetree.history.snapshotstore.s3

import com.google.inject.{TypeLiteral, Scopes, Provides, AbstractModule}
import com.tzavellas.sse.guice.ScalaModule
import org.jets3t.service.impl.rest.httpclient.RestS3Service
import com.typesafe.config.{Config, ConfigFactory}
import org.jets3t.service.security.AWSCredentials
import org.jets3t.service.model.S3Bucket
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.auth.BasicAWSCredentials
import com.codexica.s3crate.filetree.history.{FileTreeHistory, Compressor}
import scala.concurrent.{ExecutionContext, Future}
import java.util.concurrent.Executors
import com.codexica.common.FutureUtils

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class S3Module(remotePrefix: String) extends ScalaModule {

  trait FileTreeHistoryFuture extends Future[FileTreeHistory]

  def configure() {
    bind[S3Interface].to[S3InterfaceImpl].in(Scopes.SINGLETON)
  }

  @Provides @S3
  def providesConfig(): Config = {
    ConfigFactory.load().getConfig("aws")
  }

  @Provides @S3
  def providesContext(): ExecutionContext = {
    FutureUtils.makeExecutor("aws", 8)
  }

  @Provides
  def providesDirectAmazonS3Client(@S3 config: Config): AmazonS3Client = {
    val accessKey = config.getString("access_key")
    val secretKey = config.getString("secret_key")
    val client = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey))
    client.setEndpoint(s"s3-${S3Bucket.LOCATION_US_WEST_OREGON}.amazonaws.com")
    client
  }

  @Provides
  def providesRestS3Service(@S3 config: Config): RestS3Service = {
    val accessKey = config.getString("access_key")
    val secretKey = config.getString("secret_key")
    val credentials = new AWSCredentials(accessKey, secretKey)
    new RestS3Service(credentials)
  }

  @Provides
  def providesS3Bucket(@S3 config: Config, restS3: RestS3Service): S3Bucket = {
    val bucketName = config.getString("bucket_name")
    restS3.getOrCreateBucket(bucketName, S3Bucket.LOCATION_US_WEST_OREGON)
  }

  @Provides @S3
  def providesS3FileHistory(s3: S3Interface, @S3 context: ExecutionContext): Future[FileTreeHistory] = {
    //TODO:  create the crypto parameters
    val store = new S3SnapshotStore(s3, remotePrefix, context, new Compressor(), None, None, null)
    S3FileHistory.initialize(store, context)
  }
}
