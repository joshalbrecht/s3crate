package com.codexica.s3crate.filetree.history.snapshotstore.s3

import com.google.inject.{Scopes, Provides, AbstractModule}
import com.tzavellas.sse.guice.ScalaModule
import org.jets3t.service.impl.rest.httpclient.RestS3Service
import com.typesafe.config.{Config, ConfigFactory}
import org.jets3t.service.security.AWSCredentials
import org.jets3t.service.model.S3Bucket

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class S3Module extends ScalaModule {

  var config: Config = null

  def configure() {
    config = ConfigFactory.load().getConfig("aws")
    bind[S3Interface].to[S3InterfaceImpl].in(Scopes.SINGLETON)
  }

  @Provides
  def providesRestS3Service(): RestS3Service = {
    val accessKey = config.getString("access_key")
    val secretKey = config.getString("secret_key")
    val credentials = new AWSCredentials(accessKey, secretKey)
    new RestS3Service(credentials)
  }

  @Provides
  def providesS3Bucket(restS3: RestS3Service): S3Bucket = {
    val bucketName = config.getString("bucket_name")
    restS3.getOrCreateBucket(bucketName, S3Bucket.LOCATION_US_WEST)
  }
}
