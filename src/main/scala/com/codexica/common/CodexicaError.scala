package com.codexica.common

/**
 * The generic error from which all of our errors inherit
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class CodexicaError(message: String, cause: Throwable) extends Exception(message, cause) {

}
