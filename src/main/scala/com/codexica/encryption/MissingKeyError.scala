package com.codexica.encryption

import com.codexica.common.CodexicaError

/**
 * Thrown if you try to use or get a key that doesn't exist (or the private part of a public key pair that is missing)
 *
 * Where possible, try to detect and throw this error instead of a generic decryption failure
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class MissingKeyError(message: String, cause: Throwable) extends CodexicaError(message, cause) {

}
