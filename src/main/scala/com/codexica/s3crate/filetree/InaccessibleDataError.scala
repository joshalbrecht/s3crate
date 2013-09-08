package com.codexica.s3crate.filetree

import com.codexica.s3crate.S3CrateError

/**
 * This is thrown any time we try to access any data and fail, whether because of inconsistent (external) system state,
 * network/filesystem errors, anything. If this is NOT a TemporarilyInaccessibleError, the user must be informed, and
 * we must try again later, hopefully after they've fixed the problem.
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class InaccessibleDataError(message: String, cause: Throwable) extends S3CrateError(message, cause) {

}
