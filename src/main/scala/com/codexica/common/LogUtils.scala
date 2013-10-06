package com.codexica.common

import org.slf4j.LoggerFactory
import org.slf4j.helpers.SubstituteLoggerFactory
import ch.qos.logback.classic.LoggerContext

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
object LogUtils {
  def waitForStupidLogging() {
    var isLogInitializationDone = false
    while (!isLogInitializationDone) {
      LoggerFactory.getILoggerFactory match {
        case logger: SubstituteLoggerFactory => Thread.sleep(100)
        case logger: LoggerContext => {
          //Note: if you want to debug where the logging configuration is coming from, call: StatusPrinter.print(logger)
          isLogInitializationDone = true
        }
      }
    }
  }
}
