package com.codexica.s3crate.filetree.history.snapshotstore.s3

import com.codexica.common.SafeLogSpecification
import org.scalamock.specs2.MockFactory
import scala.concurrent.{Future, Await, ExecutionContext}
import com.codexica.s3crate.filetree.history.{NoCompression, FilePathState, Compressor}
import scala.concurrent.duration.Duration
import org.specs2.mock.Mockito
import com.codexica.s3crate.filetree.history.snapshotstore.{DataBlob, FileSnapshot, RemoteFileSystemTypes}
import java.util.UUID
import com.codexica.s3crate.filetree.FilePath

//See here for docs about using mockito: https://code.google.com/p/specs/wiki/UsingMockito#Using_Mockito_with_ScalaTest

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class S3FileHistorySpec extends SafeLogSpecification {

  trait Context extends Mockito with BaseContext {
    implicit val context = ExecutionContext.Implicits.global
    class ExtendedS3SnapshotStore extends S3SnapshotStore(null, "path/fds", context, new Compressor(), None, None, null)
    val store = mock[ExtendedS3SnapshotStore]
    store.clear() returns Future {}
    val id1 = UUID.randomUUID()
    val id2 = UUID.randomUUID()
    store.list() returns Future { Set(id1, id2) }
    val blob = DataBlob("remote/path", None, NoCompression())
    store.read(id1) returns Future { FileSnapshot(id1, blob, FilePathState(FilePath("local/path1"), true, None), None) }
    store.read(id2) returns Future { FileSnapshot(id2, blob, FilePathState(FilePath("local/path2"), true, None), None) }
    val fileHistory = S3FileHistory.initialize(store, context)
  }

  "initializing the file history" should {
    "generally succeed" in new Context {
      Await.result(fileHistory, Duration.Inf) must beAnInstanceOf[S3FileHistory]
    }
    "propagate appropriate failures" in new BaseContext {

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

  "calling update" should {
    "save the new file snapshot" in new Context {

    }
  }

}
