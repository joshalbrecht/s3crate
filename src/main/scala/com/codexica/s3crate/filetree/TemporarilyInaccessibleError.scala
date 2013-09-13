package com.codexica.s3crate.filetree

import com.codexica.common.InaccessibleDataError

/**
 * Parent of all errors that are completely save to just retry later without any user intervention. If these continue
 * to happen, the user should be informed (but they should continue to be periodically retried, per the
 * InaccessibleDataError contract
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class TemporarilyInaccessibleError(message: String, cause: Throwable) extends InaccessibleDataError(message, cause) {

}
