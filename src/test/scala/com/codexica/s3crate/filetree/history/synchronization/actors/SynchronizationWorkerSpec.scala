package com.codexica.s3crate.filetree.history.synchronization.actors

import com.codexica.common.SafeLogSpecification
import akka.testkit.TestActorRef

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class SynchronizationWorkerSpec extends SafeLogSpecification {

  trait Context extends BaseContext {
    val actorRef = TestActorRef[SynchronizationWorker]
    val actor = actorRef.underlyingActor
  }

  "" should {
    "" in new Context {

    }
  }
}
