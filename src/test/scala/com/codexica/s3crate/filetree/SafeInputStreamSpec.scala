package com.codexica.s3crate.filetree

import org.specs2.mutable.Specification
import org.specs2.specification.Scope

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class SafeInputStreamSpec extends Specification {

  trait Context extends Scope {}

  "reading from a safe input stream" should {
    "return the same contents as the original stream" in new Context {
      throw new NotImplementedError()
    }
    "have all failures wrapped in S3CrateError" in new Context {
      throw new NotImplementedError()
    }
  }
}
