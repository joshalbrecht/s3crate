package com.codexica.s3crate.filetree

/**
 * Throw if file data or meta data cannot be accessed because of improper permissions
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class FilePermissionError(message: String, cause: Throwable) extends InaccessibleDataError(message, cause) {

}
