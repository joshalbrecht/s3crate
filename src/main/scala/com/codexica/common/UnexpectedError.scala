package com.codexica.common

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class UnexpectedError(message: String, cause: Throwable) extends CodexicaError(message, cause) {

}
