package com.codexica.s3crate.utils

import scala.concurrent.{Promise, ExecutionContext, Future}
import scala.collection.generic.CanBuildFrom

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
object FutureUtils {

  //from here:  http://stackoverflow.com/questions/16256279/wait-for-several-futures
  def sequenceOrBailOut[A, M[_] <: TraversableOnce[_]](in: M[Future[A]] with TraversableOnce[Future[A]])(implicit cbf: CanBuildFrom[M[Future[A]], A, M[A]], executor: ExecutionContext): Future[M[A]] = {
    val p = Promise[M[A]]()

    // the first Future to fail completes the promise
    in.foreach(_.onFailure{case i => p.tryFailure(i)})

    // if the whole sequence succeeds (i.e. no failures)
    // then the promise is completed with the aggregated success
    Future.sequence(in).foreach(p trySuccess)

    p.future
  }
}
