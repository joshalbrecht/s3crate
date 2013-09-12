import de.johoop.jacoco4sbt._

import JacocoPlugin._

jacoco.settings

parallelExecution in jacoco.Config := false

name := "s3crate"

organization := "codexica"

version := "0.0.1-SNAPSHOT"

scalaVersion := "2.10.2"

libraryDependencies += "net.java.dev.jets3t" % "jets3t" % "0.9.0"

libraryDependencies += "com.amazonaws" % "aws-java-sdk" % "1.3.33"

libraryDependencies += "org.ow2.asm" % "asm" % "4.1"

libraryDependencies += "commons-codec" % "commons-codec" % "1.8"

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.1"

libraryDependencies += "commons-io" % "commons-io" % "2.4"

libraryDependencies += "commons-net" % "commons-net" % "3.2"

libraryDependencies += "com.google.inject" % "guice" % "3.0"

libraryDependencies += "org.sonatype.sisu.inject" % "cglib" % "3.0"

libraryDependencies += "com.google.inject.extensions" % "guice-assistedinject" % "3.0"

libraryDependencies += "com.google.inject.extensions" % "guice-multibindings" % "3.0"

libraryDependencies += "com.tzavellas" % "sse-guice" % "0.7.1"

libraryDependencies += "org.bouncycastle" % "bcprov-ext-jdk15on" % "1.49"

libraryDependencies += "org.xerial.snappy" % "snappy-java" % "1.1.0-M4"

libraryDependencies += "com.google.guava" % "guava" % "14.0.1"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.2-M1"

libraryDependencies += "com.typesafe.akka" %% "akka-actor-tests" % "2.2-M1"

libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.2-M1"

libraryDependencies += "joda-time" % "joda-time" % "2.3"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.9"

resolvers += "Mandubian repository snapshots" at "https://github.com/mandubian/mandubian-mvn/raw/master/snapshots/"

resolvers += "Mandubian repository releases" at "https://github.com/mandubian/mandubian-mvn/raw/master/releases/"

libraryDependencies += "play" %% "play-json" % "2.2-SNAPSHOT"

libraryDependencies += "org.specs2" %% "specs2" % "1.13" % "test"

libraryDependencies += "junit" % "junit" % "4.8" % "test"

libraryDependencies += "org.scalamock" %% "scalamock-specs2-support" % "3.0.1" % "test"

libraryDependencies += "org.aspectj" % "aspectjweaver" % "1.7.2"

libraryDependencies += "org.aspectj" % "aspectjrt"     % "1.7.2"

libraryDependencies += "com.jcabi"   % "jcabi-aspects" % "0.8"

libraryDependencies += "com.jcabi"   % "jcabi-log" % "0.8"

