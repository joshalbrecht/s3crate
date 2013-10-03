package com.codexica.s3crate.filetree.history.synchronization

import com.codexica.common.{InaccessibleDataError, FutureUtils}
import com.codexica.s3crate.filetree.history.{FilePathState, FileTreeHistory}
import com.codexica.s3crate.filetree.{FilePath, FilePathEvent, FileTreeListener, FileTree}
import java.util.concurrent.atomic.AtomicInteger
import org.joda.time.DateTime
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import org.slf4j.LoggerFactory

/**
 * Synchronize all changes from the file tree to the history.
 * Immediately begins synchronization.
 * Triggers the FileTree to begin generating events.
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class Historian(fileTree: FileTree,
                treeHistory: FileTreeHistory,
                ec: ExecutionContext) extends FileTreeListener {

  implicit private val context = ec

  //TODO: make this configurable
  val MAX_WORKERS = 8

  protected val logger = LoggerFactory.getLogger(getClass)

  //a list of events that should be processed
  private val pendingEvents = new ListBuffer[FilePathEvent]
  //the last time that a path was successfully processed
  private val lastPathSuccess = new mutable.HashMap[FilePath, DateTime]()
  //mapping from path events to their most recent status
  private val pathProgress = new mutable.HashMap[FilePathEvent, PathSyncStatus]()
  //a counter for how many workers are currently going
  private val workerCounter = new AtomicInteger(0)
  //set when we are trying to stop
  private var isStopping = false
  //a lock for editing any of the above variables:
  private val eventLock: AnyRef = new Object()

  //register ourselves as a listener to the file tree:
  val eventGenerator = fileTree.listen(this)
  //and start listening for events:
  eventGenerator.start()

  /**
   * Call this to stop the synchronization politely. Mostly allow uploads
   * to finish, etc, so it can take a little while. Poll status to see how
   * much longer.
   *
   * @return A Future that will be complete when everything is stopped.
   */
  def stop(): Future[Unit] = Future {
    eventGenerator.stop()
    //clear the list of pending tasks as well.
    eventLock.synchronized {
      isStopping = true
      pendingEvents.clear()
    }
    //TODO: return when all current tasks have succeeded or failed
  }

  /**
   * @return Information representing the current status of the synchronization
   *         Note that this interface is not flushed out at all, and so is
   *         currently the simplest thing that makes any sense.
   */
  def status: List[String] = {
    eventLock.synchronized {
      pathProgress.map({case (event, status) => {
        s"Path ${event.path} is currently ${status.data}"
      }}).toList
    }
  }

  override def onNewFilePathEvent(event: FilePathEvent) {
    if (isInterestingEvent(event)) {
      eventLock.synchronized {
        if (!isStopping) {
          pendingEvents += event
        }
      }
      checkForWork()
    }
  }

  protected def checkForWork() {
    eventLock.synchronized {
      if (isStopping) {
        return
      }
      while (pendingEvents.size > 0 && workerCounter.get() < MAX_WORKERS) {
        val event = pendingEvents.remove(0)
        //only process an event if it has not yet succeeded
        if (!lastPathSuccess.contains(event.path)
          //or if it succeeded, but for an earlier modified time
          || lastPathSuccess(event.path).isBefore(event.lastModified)) {
          val (status, future) = handleTask(event)
          workerCounter.incrementAndGet()
          pathProgress(event) = status
          future.onComplete({
            case Success(_) => eventLock.synchronized {
              lastPathSuccess(event.path) = event.lastModified
              workerCounter.decrementAndGet()
              //TODO: update the status?
            }
            case Failure(t) => eventLock.synchronized {
              workerCounter.decrementAndGet()
              //TODO: also use the failures to update the status
              //TODO:  filter these failures. Most are going to result in putting the event back into the queue
              if (!isStopping) {
                t match {
                  case x: InaccessibleDataError => {
                    pendingEvents += event
                  }
                  case _ => {
                    logger.error("Failed in an unexpected way!", t)
                  }
                }
              }
            }
          })
          return
        }
      }
    }
  }

  //TODO: implement filters
  /**
   * @param event The event that just happened
   * @return true iff the event should cause some synchronization, else false
   */
  protected def isInterestingEvent(event: FilePathEvent): Boolean = {
    true
  }

  private def handleTask(event: FilePathEvent):
                        (PathSyncStatus, Future[Unit]) = {
    val path = event.path
    //TODO:  pass the status through the whole process so we can track it
    val status = new PathSyncStatus()
    //generate metadata for each of the filesystems
    val sourceMetaFuture = fileTree.metadata(path)
    val destMetaFuture = treeHistory.metadata(path).map({
      case Some(x) => x.fileSnapshot
      case None => FilePathState(path, false, None)
    })
    val metaFutures = List(sourceMetaFuture, destMetaFuture)
    val future = FutureUtils.sequenceOrBailOut(metaFutures)
      .flatMap({
      case List(source, dest) => {
        val operation = if (source.exists) {
          if (dest.exists) {
            sync(source, dest)
          } else {
            onDestMissing(source)
          }
        } else {
          if (dest.exists) {
            onSourceMissing(dest)
          } else {
            //TODO:  think about how we could get here and what to do
            //this should probably throw an exception--it's stupid that you pass two empty paths as work...
            throw new RuntimeException("Path is not defined in either source: " + path)
          }
        }
        operation
      }
    })
    (status, future)
  }

  /**
   * Ensure that the two snapshots are synchronized
   *
   * @param source The source snapshot
   * @param dest The destination snapshot
   */
  private def sync(source: FilePathState, dest: FilePathState): Future[Unit] = {
    if (source == dest) {
      Future.successful()
    } else {
      writeToDest(source)
    }
  }

  /**
   * Called when the source is missing. Verify that the destination is in the deleted state
   */
  private def onSourceMissing(dest: FilePathState): Future[Unit] = {
    if (dest.exists) {
      //TODO: update the status based on what is returned? (here and below)
      treeHistory.delete(dest.path).map(x => Unit)
    } else {
      Future.successful()
    }
  }

  /**
   * Called when the destination file is missing. Just go create it.
   */
  private def onDestMissing(source: FilePathState): Future[Unit] = {
    writeToDest(source)
  }

  private def writeToDest(source: FilePathState): Future[Unit] = {
    if (source.exists) {
      treeHistory.update(source.path, fileTree).map(x => Unit)
    } else {
      Future.successful()
    }
  }
}
