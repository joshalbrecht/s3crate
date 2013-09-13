package com.codexica.common

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import scala.concurrent.{ExecutionContext, Await, Future}
import scala.concurrent.duration.Duration

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class FutureUtilsSpec extends Specification {

  trait Context extends Scope {
    implicit val ec = ExecutionContext.Implicits.global
  }

  "Failures" should {
    "immediately trigger the result" in new Context {
      val f1 = Future { Thread.sleep(2000) ; 5 / 0 }
      val f2 = Future { 5 }
      val f3 = Future { None.get }

      val future = FutureUtils.sequenceOrBailOut(List(f1, f2, f3))
      Await.ready(future, Duration.Inf)
      future.onFailure {
        case i => i must haveClass[java.util.NoSuchElementException]
      }
    }
  }

  "All futures finishing successfully" should {
    "return a correct list" in new Context {
      val f1 = Future { 1 }
      val f2 = Future { Thread.sleep(100); 2 }
      val f3 = Future { 3 }

      val future = FutureUtils.sequenceOrBailOut(List(f1, f2, f3))
      Await.ready(future, Duration.Inf)
      future.onSuccess {
        case i => i must be equalTo List(1, 2, 3)
      }
    }
  }
}
