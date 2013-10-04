package com.codexica.s3crate.filetree.history.synchronization

import com.codexica.common.{InaccessibleDataError, SafeLogSpecification}
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
    def makeSnapshot(remoteFileExists: Boolean) = {
      val state = FilePathState(path, remoteFileExists, None)
      val blob = DataBlob("sfasd", None, NoCompression())
      FileSnapshot(UUID.randomUUID(), blob, state, None)
    }

    def singleFileCheck(localFileExists: Boolean, remoteFileExists: Boolean) {
      fileTree.metadata(Matchers.eq(path)) returns Future {
        FilePathState(path, localFileExists, None)
      }
      treeHistory.metadata(Matchers.eq(path)) returns Future {
        Option(makeSnapshot(remoteFileExists))
      }
      val historian = new Historian(fileTree, treeHistory, context)
      historian.onNewFilePathEvent(event)
      Await.result(historian.stop(), Duration.Inf)
    }
  }

  "a missing file" should {
    "cause a deletion in the history" in new Context {
      singleFileCheck(false, true)
      there was one(treeHistory).delete(Matchers.eq(path))
    }
  }

  "a changed file" should {
    "cause a change in the history" in new Context {
      singleFileCheck(true, true)
      there was one(treeHistory).update(Matchers.eq(path), Matchers.eq(fileTree))
    }
  }

  "a new file" should {
    "cause a new file in the history" in new Context {
      singleFileCheck(true, false)
      there was one(treeHistory).update(Matchers.eq(path), Matchers.eq(fileTree))
    }
  }

  "throwing an invalid data exception while synching" should {
    "cause that event to be retried" in new Context {
      var wasCalled = false
      treeHistory.update(Matchers.eq(path), Matchers.eq(fileTree)).answers(
        (p, t) => {
          if (wasCalled) {
            Future {
              makeSnapshot(true)
            }
          } else {
            wasCalled = true
            throw new InaccessibleDataError("Whatever", null)
          }
        }
      )
      singleFileCheck(true, true)
      there were two(treeHistory).update(Matchers.eq(path), Matchers.eq(fileTree))
    }
  }
}
