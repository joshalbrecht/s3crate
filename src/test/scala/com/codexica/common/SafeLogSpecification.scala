package com.codexica.common

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.slf4j.LoggerFactory
import org.slf4j.helpers.SubstituteLoggerFactory
import ch.qos.logback.classic.LoggerContext

/**
 * Bleh. Race conditions from slf4j initialization being unsafe for threads and initialized in a different thread.
 * We work around it by simply waiting until we get back a real logger before proceeding with the test.
 * Without this, some log messages will be dropped and warnings are spewed.
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class SafeLogSpecification extends Specification {
  trait BaseContext extends Scope {
    LogUtils.waitForStupidLogging()
  }
}
