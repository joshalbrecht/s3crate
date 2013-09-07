package com.codexica.s3crate.filesystem

import java.io.{FileOutputStream, FileInputStream, InputStream, File}
import scala.concurrent.Future
import com.codexica.s3crate.utils.{FutureUtils, Contexts}
import org.apache.commons.io.{IOUtils, FileUtils}
import org.apache.commons.io.filefilter.TrueFileFilter
import scala.collection.JavaConversions._
import com.google.common.base.Throwables
import scala.util.{Success, Failure}
import java.nio.file.{Paths, Path, LinkOption, Files}
import org.joda.time.{DateTimeZone, DateTime}

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class LocalFileSystem(baseFolder: File) extends FileSystem {

  assert(baseFolder.isDirectory)

  implicit val context = Contexts.fileOperations

  override def start(): Future[Set[FilePathEvent]] = Future {
    val files = FileUtils.listFilesAndDirs(baseFolder, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)
    val futures = files.toList.map((file: File) => getFilePath(file)).map((filePath: FilePath) => Future {
      val attributes = Files.readAttributes(Paths.get(getFile(filePath).getAbsolutePath), "lastModifiedTime", LinkOption.NOFOLLOW_LINKS)
      val lastModifiedUtcMillis = attributes.get("lastModifiedTime").asInstanceOf[Long]
      FilePathEvent(filePath, new DateTime(lastModifiedUtcMillis, DateTimeZone.UTC))
    })
    var pathEvents: Set[FilePathEvent] = Set()
    FutureUtils.sequenceOrBailOut(futures).onComplete({
      case Success(snapshotList) => {
        pathEvents = snapshotList.toSet
      }
      case Failure(t) => Throwables.propagate(t)
    })
    pathEvents
  }

  override def snapshot(path: FilePath): Future[Option[FileSnapshot]] = Future {
    //TODO: implement
//    val file = getFile(path)
//    if (file.exists()) {
//      val attributes = Files.readAttributes(Paths.get(file.getAbsolutePath), "*", LinkOption.NOFOLLOW_LINKS)
//      //RESUME:  blah blah, set the rest of the attributes.  Maybe get lazy and do it as a map...
//    } else {
//      Option.empty[FileSnapshot]
//    }
    null
  }

  //TODO:  set all meta data based on the data from FileSnapshot
  override def write(data: ReadableFile, snapshot: FileSnapshot): Future[Unit] = Future {
    val output = new FileOutputStream(getFile(snapshot.path))
    val input = data.data()
    IOUtils.copy(input, output)
    output.close()
    input.close()
    Unit
  }

  //TODO: it might be a good idea to copy this somewhere first, so that it is not concurrently modified while reading...
  override def read(path: FilePath): ReadableFile = {
    val file = getFile(path)
    new ReadableFile(() => {
      new FileInputStream(file)
    }, file.length())
  }

  private def getFilePath(file: File): FilePath = {
    FilePath(Paths.get(baseFolder.getAbsolutePath).relativize(Paths.get(file.getAbsolutePath)).toString)
  }

  private def getFile(path: FilePath): File = {
    new File(baseFolder, path.path)
  }
}

