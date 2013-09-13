package com.codexica.common

/**
 * This is thrown any time we try to access any data and fail, whether because of inconsistent (external) system state,
 * network/filesystem errors, anything. If this is NOT a TemporarilyInaccessibleError, the user must be informed, and
 * we must try again later, hopefully after they've fixed the problem.
 *
 * Note that this should ONLY wrap errors that are DEFINITELY caused by an inconsistent state. Any exceptional
 * conditions that could be solved by changing our code should NOT be wrapped (ex: wrapping a FileSystemNotFound error
 * is fine, but wrapping Exception, or NullPointerError, or anything generic like that that may be caused by an actual
 * programming error on our part, should not be wrapped)
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class InaccessibleDataError(message: String, cause: Throwable) extends CodexicaError(message, cause) {

}
