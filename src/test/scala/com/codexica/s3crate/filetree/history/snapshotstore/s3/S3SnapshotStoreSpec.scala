package com.codexica.s3crate.filetree.history.snapshotstore.s3

import com.codexica.common.{InaccessibleDataError, SafeLogSpecification}
import com.codexica.s3crate.filetree.FilePath
import com.codexica.s3crate.filetree.history.snapshotstore.{DataBlob, FileSnapshot}
import com.codexica.s3crate.filetree.history.{FilePathState, NoCompression, Compressor}
import java.io.File
import java.util.UUID
import org.apache.commons.io.FileUtils
import org.jets3t.service.model.S3Object
import org.scalamock.specs2.MockFactory
import org.scalamock.{FunctionAdapter2, FunctionAdapter1}
import play.api.libs.json.Json
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
    val snapshot = FileSnapshot(UUID.randomUUID(),
      DataBlob("location", None, NoCompression()),
      FilePathState(FilePath("somewhere"), false, None),
      None)
    val jsonSnapshot = Json.stringify(Json.toJson(snapshot))
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
      (s3.listObjects _).expects(new FunctionAdapter1((prefix: String) => {
        prefix.endsWith("53")
      })).throwing(new InaccessibleDataError("test failure", null))
      (s3.listObjects _).expects(new FunctionAdapter1((prefix: String) => {
        true
      })).returning(Set()).anyNumberOfTimes()
      Await.result(snapshotStore.list(), Duration.Inf) must throwAn[InaccessibleDataError]
    }
  }

  "reading a snapshot id" should {
    "download the snapshot if it doesn't exist" in new Context {
      (s3.download(_:String, _:File)).expects(new FunctionAdapter2((path: String, file: File) => {
        path.endsWith(snapshot.id.toString) must be equalTo true
        FileUtils.write(file, jsonSnapshot)
        true
      }))
      val snapshotFuture = snapshotStore.read(snapshot.id)
      Await.result(snapshotFuture, Duration.Inf) must be equalTo snapshot
    }
    "return the snapshot immediately if it exists in the folder" in new Context {
      (s3.download(_:String, _:File)).expects(new FunctionAdapter2((path: String, file: File) => {
        FileUtils.write(file, jsonSnapshot)
        true
      }))
      Await.result(snapshotStore.read(snapshot.id), Duration.Inf) must be equalTo snapshot
      Await.result(snapshotStore.read(snapshot.id), Duration.Inf) must be equalTo snapshot
    }
    "download and return the snapshot if there is a corrupted snapshot in the folder" in new Context {
      var numCalls = 0
      (s3.download(_:String, _:File)).expects(new FunctionAdapter2((path: String, file: File) => {
        if (numCalls == 0) {
          FileUtils.write(file, "not actually json")
        } else {
          FileUtils.write(file, jsonSnapshot)
        }
        numCalls += 1
        true
      })).twice()
      Await.result(snapshotStore.read(snapshot.id), Duration.Inf) must be equalTo snapshot
    }
    "return an IllegalArgumentException if a bad id is passed in" in new Context {
      (s3.download(_:String, _:File)).expects(new FunctionAdapter2((path: String, file: File) => {
        true
      })).throwing(new IllegalArgumentException("bad path"))
      Await.result(snapshotStore.read(snapshot.id), Duration.Inf) must throwAn[IllegalArgumentException]
    }
  }

}
