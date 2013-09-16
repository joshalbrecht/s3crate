package com.codexica.s3crate.filetree.history.snapshotstore.s3

import com.codexica.common.SafeLogSpecification
import com.codexica.s3crate.filetree.history.Compressor
import java.util.UUID
import org.jets3t.service.model.S3Object
import org.scalamock.FunctionAdapter1
import org.scalamock.specs2.MockFactory
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class S3SnapshotStoreSpec extends SafeLogSpecification with MockFactory {

  trait Context extends BaseContext {
    val s3 = mock[S3Interface]
    val ev = ExecutionContext.Implicits.global
    val remotePath = "some/path"
    val snapshotStore = new S3SnapshotStore(s3, remotePath, ev, new Compressor(), None, None, null)
  }

  "listing all snapshots" should {
    "return an empty list if there are no snapshots" in new Context {
      (s3.listObjects _).expects(new FunctionAdapter1((prefix: String) => {
        true
      })).returning(Set()).anyNumberOfTimes()
      Await.result(snapshotStore.list(), Duration.Inf) must be equalTo Set()
    }
    "return the list of all snapshots if there are many" in new Context {
      (s3.listObjects _).expects(new FunctionAdapter1((prefix: String) => {
        prefix.endsWith("0a")
      })).returning(Set(
        new S3Object("meta/0a756ed0-1ef1-11e3-8224-0800200c9a66"),
        new S3Object("meta/0a111ed0-0ef0-00e3-0000-111111111111")))
      (s3.listObjects _).expects(new FunctionAdapter1((prefix: String) => {
        prefix.endsWith("53")
      })).returning(Set(new S3Object("meta/53974120-1ef1-11e3-8224-0800200c9a66")))
      (s3.listObjects _).expects(new FunctionAdapter1((prefix: String) => {
        true
      })).returning(Set()).anyNumberOfTimes()
      Await.result(snapshotStore.list(), Duration.Inf) must be equalTo Set(
        UUID.fromString("0a756ed0-1ef1-11e3-8224-0800200c9a66"),
        UUID.fromString("0a111ed0-0ef0-00e3-0000-111111111111"),
        UUID.fromString("53974120-1ef1-11e3-8224-0800200c9a66")
      )
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
