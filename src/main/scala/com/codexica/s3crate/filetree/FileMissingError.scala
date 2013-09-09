package com.codexica.s3crate.filetree

/**
 * Thrown iff the file is missing when we attempt to access it
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class FileMissingError(message: String, cause: Throwable) extends InaccessibleDataError(message, cause) {

}
