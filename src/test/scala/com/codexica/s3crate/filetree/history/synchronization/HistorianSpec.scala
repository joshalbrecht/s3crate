package com.codexica.s3crate.filetree.history.synchronization

import com.codexica.common.SafeLogSpecification
import com.codexica.s3crate.filetree.history.{NoCompression, FilePathState, FileTreeHistory}
import com.codexica.s3crate.filetree.{FileMetaData, PathGenerator, FileTreeListener, FilePathEvent, FilePath, FileTree}
import org.joda.time.DateTime
import org.specs2.mock.Mockito
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.concurrent.duration.Duration
import com.codexica.s3crate.filetree.history.snapshotstore.{DataBlob, FileSnapshot}
import java.util.UUID
import org.mockito.Matchers

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class HistorianSpec extends SafeLogSpecification {

  trait Context extends Mockito with BaseContext {
    implicit val context = ExecutionContext.Implicits.global
    val fileTree = mock[FileTree]
    fileTree.listen(any[FileTreeListener]) returns mock[PathGenerator]
    val treeHistory = mock[FileTreeHistory]
    val path = FilePath("/some/path")
    val event = FilePathEvent(path, new DateTime())
  }

  "a missing file" should {
    "cause a deletion in the history" in new Context {
      fileTree.metadata(Matchers.eq(path)) returns Future {FilePathState(path, false, None)}
      treeHistory.metadata(Matchers.eq(path)) returns Future {
        val state = FilePathState(path, true, None)
        val blob = DataBlob("sfasd", None, NoCompression())
        Option(FileSnapshot(UUID.randomUUID(), blob, state, None))
      }
      val historian = new Historian(fileTree, treeHistory, context)
      historian.onNewFilePathEvent(event)
      Await.result(historian.stop(), Duration.Inf)
      there was one(treeHistory).delete(Matchers.eq(path))
    }
  }

  "a changed file" should {
    "cause a change in the history" in new Context {

    }
  }

  "a new file" should {
    "cause a new file in the history" in new Context {

    }
  }

  "throwing an invalid data exception while synching" should {
    "cause that event to be retried" in new Context {

    }
  }

  "stopping the sync process" should {
    "finish only after all tasks have completed" in new Context {

    }
  }

}
