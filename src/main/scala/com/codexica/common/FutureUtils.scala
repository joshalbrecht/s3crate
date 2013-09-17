package com.codexica.common

import scala.concurrent.{Promise, ExecutionContext, Future}
import scala.collection.generic.CanBuildFrom

/**
 * Helper functions for Futures
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
object FutureUtils {

  /**
   * Return either:
   * - The first Future to fail
   * OR
   * - A sequence of all successes (if no futures failed)
   *
   * From here:  http://stackoverflow.com/questions/16256279/wait-for-several-futures
   *
   * @param in The sequence of Futures that you'd like to wait for
   * @param cbf Description of the type transformation that is occurring?
   * @param executor The context in which we can execute
   * @tparam A The type of each element in the sequence
   * @tparam M The type of the sequence
   * @return A future that finishes as described above
   */
  def sequenceOrBailOut[A, M[_] <: TraversableOnce[_]](in: M[Future[A]] with TraversableOnce[Future[A]])
      (implicit cbf: CanBuildFrom[M[Future[A]], A, M[A]], executor: ExecutionContext): Future[M[A]] = {
    val p = Promise[M[A]]()
    in.foreach(_.onFailure{case i => p.tryFailure(i)})
    Future.sequence(in).foreach(p trySuccess)
    p.future
  }

  @annotation.tailrec
  def retry[T](n: Int)(fn: => T): util.Try[T] = {
    util.Try { fn } match {
      case x: util.Success[T] => x
      case _ if n > 1 => retry(n - 1)(fn)
      case f => f
    }
  }
}
