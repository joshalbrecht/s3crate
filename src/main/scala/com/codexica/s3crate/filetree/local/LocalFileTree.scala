package com.codexica.s3crate.filetree.local

import com.codexica.s3crate.filetree._
import scala.concurrent.{ExecutionContext, Future}
import java.io._
import com.codexica.s3crate.filetree.history.FilePathState
import java.nio.file.{LinkOption, Files, Paths}
import java.nio.file.attribute.{PosixFilePermission, FileTime}
import org.joda.time.{DateTimeZone, DateTime}
import org.apache.commons.io.{IOUtils, FileUtils}
import org.apache.commons.io.filefilter.TrueFileFilter
import scala.collection.JavaConversions._
import scala.util.control.NonFatal
import com.codexica.s3crate.{UnexpectedError, S3CrateError}
import com.codexica.s3crate.filetree.SymLinkType
import com.codexica.s3crate.filetree.FileType
import com.codexica.s3crate.filetree.FilePathEvent
import com.codexica.s3crate.filetree.FolderType

/**
 * A locally mounted file system. Should be accessibly by working with java.io.File's
 *
 * Does most of the work for the implementing classes, which need only define a few metadata accessors
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
abstract class LocalFileTree(val baseFolder: File, implicit val ec: ExecutionContext) extends ListenableFileTree with ReadableFileTree with WritableFileTree {

  def getFileType(file: File): FilePathType
  def getOwner(file: File): String
  def getGroup(file: File): String
  def getPermissions(file: File): Set[PosixFilePermission]
  def getSymLinkPath(file: File): Option[FilePath]

  //TODO: eventually use the fancier "file watcher" mechanism
  override def listen(): PathGenerator = {
    //list all of the files
    val initialFiles = FileUtils.listFilesAndDirs(baseFolder, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE).par.map(file => {
      FilePathEvent(getFilePath(file), getLastModified(file))
    }).seq.toSet
    new PathGenerator(initialFiles, this)
  }

  override def read(path: FilePath): SafeInputStream = {
    SafeInputStream.fromFile(getFile(path))
  }

  override def metadata(path: FilePath): Future[FilePathState] = Future {
    try {
      val file = getFile(path)
      val exists = file.exists()
      val fileType = getFileType(file)
      val symLinkPath = fileType match {
        case SymLinkType() => {
          getSymLinkPath(file)
        }
        case FolderType() | FileType() => None
      }
      val metadata = if (exists) {
        Option(FileMetaData(
          fileType,
          file.length(),
          getLastModified(file),
          getOwner(file),
          getGroup(file),
          getPermissions(file),
          symLinkPath,
          file.isHidden
        ))
      } else {
        None
      }
      FilePathState(path, exists, metadata)
    } catch {
      case e: S3CrateError => throw e
      //TODO:  improve the list of things that can be thrown here so that all appropriate errors are wrapped
      case e: IOException => throw new InaccessibleDataError("Failure fetching metadata from file tree", e)
      case NonFatal(e) => throw new UnexpectedError("Unexpected error while fetching metadata from file tree", e)
    }
  }

  protected[this] def getLastModified(file: File): DateTime = {
    val attributes = Files.readAttributes(Paths.get(file.getAbsolutePath), "lastModifiedTime", LinkOption.NOFOLLOW_LINKS)
    attributes.get("lastModifiedTime") match {
      case lastModified: FileTime => new DateTime(lastModified.toMillis, DateTimeZone.UTC)
    }
  }

  protected[this] def getFilePath(file: File): FilePath = {
    FilePath(Paths.get(baseFolder.getAbsolutePath).relativize(Paths.get(file.getAbsolutePath)).toString)
  }

  protected[this] def getFile(path: FilePath): File = {
    new File(baseFolder, path.path)
  }
}
