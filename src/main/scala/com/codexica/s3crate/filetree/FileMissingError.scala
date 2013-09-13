package com.codexica.s3crate.filetree

import com.codexica.common.InaccessibleDataError

/**
 * Thrown iff the file is missing when we attempt to access it
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class FileMissingError(message: String, cause: Throwable) extends InaccessibleDataError(message, cause) {

}
