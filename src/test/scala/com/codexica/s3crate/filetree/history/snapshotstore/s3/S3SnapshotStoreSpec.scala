package com.codexica.s3crate.filetree.history.snapshotstore.s3

import org.scalamock.specs2.MockFactory
import scala.concurrent.{Await, ExecutionContext}
import com.codexica.encryption.{RSA}
import org.scalamock.FunctionAdapter1
import scala.concurrent.duration.Duration
import com.codexica.common.SafeLogSpecification

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class S3SnapshotStoreSpec extends SafeLogSpecification with MockFactory {

  trait Context extends BaseContext {
    val s3 = mock[S3Interface]
    val ec = ExecutionContext.Implicits.global
    //TODO: set encryption stuff
    val snapshotStore = new S3SnapshotStore(s3, "some/prefix", ec, null, null, null)
  }

  "listing all snapshots" should {
    "return an empty list if there are no snapshots" in new Context {
      (s3.listObjects _).expects(new FunctionAdapter1((prefix: String) => {
        true
      })).returning(Set()).anyNumberOfTimes()
      Await.result(snapshotStore.list(), Duration.Inf) must be equalTo Set()
    }
    "return the list of all snapshots if there are many" in new Context {

    }
    "contain an exception if listing failed" in new Context {

    }
  }

  "reading a snapshot id" should {
    "download the snapshot if it doesnt exist" in new Context {

    }
    "return the snapshot immediately if it exists in the folder" in new Context {

    }
    "download and return the snapshot if there is a corrupted snapshot in the folder" in new Context {

    }
    "return an invalid state exception if a bad id is passed in" in new Context {

    }
  }

}
