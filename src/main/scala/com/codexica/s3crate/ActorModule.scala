package com.codexica.s3crate

import com.tzavellas.sse.guice.ScalaModule
import com.google.inject.Provides
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class ActorModule extends ScalaModule {
  def configure() {

  }

  @Provides
  def providesActorSystem(): ActorSystem = {
    ActorSystem("s3crate", ConfigFactory.load().getConfig("com.codexica.s3crate"))
  }
}
