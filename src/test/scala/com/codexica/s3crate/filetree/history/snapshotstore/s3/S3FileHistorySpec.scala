package com.codexica.s3crate.filetree.history.snapshotstore.s3

import com.codexica.common.{SafeInputStream, SafeLogSpecification}
import com.codexica.s3crate.filetree.history.snapshotstore.{DataBlob, FileSnapshot}
import com.codexica.s3crate.filetree.history.{NoCompression, FilePathState, Compressor}
import com.codexica.s3crate.filetree.{FileType, FileMetaData, ReadableFileTree, FilePath}
import java.util.UUID
import org.joda.time.DateTime
import org.mockito.Matchers
import org.specs2.mock.Mockito
import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Await, ExecutionContext}
import java.io.IOException

//See here for docs about using mockito: https://code.google.com/p/specs/wiki/UsingMockito#Using_Mockito_with_ScalaTest

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class S3FileHistorySpec extends SafeLogSpecification {

  trait Context extends Mockito with BaseContext {
    implicit val context = ExecutionContext.Implicits.global

    class ExtendedS3SnapshotStore extends S3SnapshotStore(null,
      "path/fds",
      context,
      new Compressor(),
      None,
      None,
      null)

    val store = mock[ExtendedS3SnapshotStore]
    store.clear() returns Future {}
    val id1 = UUID.randomUUID()
    val id2 = UUID.randomUUID()
    store.list() returns Future {
      Set(id1, id2)
    }
    val blob = DataBlob("remote/path", None, NoCompression())
    val filePath = FilePath("local/path1")
    store.read(id1) returns Future {
      FileSnapshot(id1, blob, FilePathState(filePath, true, None), None)
    }
    store.read(id2) returns Future {
      FileSnapshot(id2, blob, FilePathState(FilePath("local/path2"), true, None), None)
    }
    val fileHistory = S3FileHistory.initialize(store, context)
    val fileTree = mock[ReadableFileTree]
  }

  "initializing the file history" should {
    "generally succeed" in new Context {
      Await.result(fileHistory, Duration.Inf) must beAnInstanceOf[S3FileHistory]
    }
    "propagate appropriate failures" in new Context {
      val failStore = mock[ExtendedS3SnapshotStore]
      failStore.clear() returns Future {
        throw new IOException("fail")
      }
      val history = S3FileHistory.initialize(failStore, context)
      Await.result(history, Duration.Inf) must throwAn[IOException]
    }
  }

  "reading metadata" should {
    "return the correct snapshot data" in new Context {
      fileHistory.flatMap(history => {
        history.metadata(FilePath("local/path1"))
      })
      Await.result(fileHistory, Duration.Inf)
    }
  }

  //TODO: make a test of a filepath that doesn't exist yet

  //TODO: test when there are previous versions that things work out correctly (and updates most recent version?)

  "calling update" should {
    "save the new file snapshot" in new Context {
      val pathState = FilePathState(filePath,
        true,
        Option(FileMetaData(FileType(),
          34523,
          new DateTime(),
          "owner",
          "group",
          Set(),
          None,
          false)))
      val snapshot = FileSnapshot(UUID.randomUUID(), blob, pathState, None)
      fileTree.metadata(Matchers.eq(filePath)) returns Future {
        pathState
      }
      store.saveBlob(any[() => SafeInputStream]) returns Future { blob }
      store.saveSnapshot(filePath, pathState, blob, None) returns Future { snapshot }
      Await.result(fileHistory, Duration.Inf).update(filePath, fileTree)
      there was one(store).saveSnapshot(filePath, pathState, blob, None)
    }
  }

}
