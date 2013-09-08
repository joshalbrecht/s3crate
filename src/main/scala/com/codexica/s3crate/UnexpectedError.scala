package com.codexica.s3crate

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class UnexpectedError(message: String, cause: Throwable) extends S3CrateError(message, cause) {

}
