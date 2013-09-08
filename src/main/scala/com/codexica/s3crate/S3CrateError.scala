package com.codexica.s3crate

/**
 * The generic error from which all of our errors inherit
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class S3CrateError(message: String, cause: Throwable) extends Exception(message, cause) {

}
