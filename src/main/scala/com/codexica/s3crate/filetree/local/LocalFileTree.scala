package com.codexica.s3crate.filetree.local

import com.codexica.s3crate.filetree._
import scala.concurrent.{ExecutionContext, Future}
import java.io._
import com.codexica.s3crate.filetree.history.FilePathState
import java.nio.file._
import java.nio.file.attribute.{PosixFilePermission, FileTime}
import org.joda.time.{DateTimeZone, DateTime}
import org.apache.commons.io.{IOUtils, FileUtils}
import org.apache.commons.io.filefilter.TrueFileFilter
import scala.collection.JavaConversions._
import scala.util.control.NonFatal
import com.codexica.s3crate.filetree.SymLinkType
import com.codexica.s3crate.filetree.FileType
import com.codexica.s3crate.filetree.FilePathEvent
import com.codexica.s3crate.filetree.FolderType
import com.codexica.common.{InaccessibleDataError, SafeInputStream, UnexpectedError, CodexicaError}
import com.codexica.s3crate.filetree.SymLinkType
import com.codexica.s3crate.filetree.FileType
import com.codexica.s3crate.filetree.FilePathEvent
import com.codexica.s3crate.filetree.FolderType
import com.codexica.s3crate.filetree.SymLinkType
import com.codexica.s3crate.filetree.FileType
import com.codexica.s3crate.filetree.FilePathEvent
import com.codexica.s3crate.filetree.FolderType
import com.jcabi.aspects.Loggable
import java.util.concurrent.TimeUnit

/**
 * A locally mounted file system. Should be accessibly by working with java.io.File's
 *
 * Does most of the work for the implementing classes, which need only define a few metadata accessors
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
abstract class LocalFileTree(val baseFolder: File, implicit val ec: ExecutionContext) extends FileTree {

  /**
   * @param file For this file
   * @throws InaccessibleDataError as described in withWrappedError
   * @return Return the appropriate FilePathType (folder, file, or symlink)
   */
  def getFileType(file: File): FilePathType

  /**
   * @param file For this file
   * @throws InaccessibleDataError as described in withWrappedError
   * @return Return the appropriate FilePathType (folder, file, or symlink)
   */
  def getOwner(file: File): String

  /**
   * @param file For this file
   * @throws InaccessibleDataError as described in withWrappedError
   * @return Return the appropriate FilePathType (folder, file, or symlink)
   */
  def getGroup(file: File): String

  /**
   * @param file For this file
   * @throws InaccessibleDataError as described in withWrappedError
   * @return Return the appropriate FilePathType (folder, file, or symlink)
   */
  def getPermissions(file: File): Set[PosixFilePermission]

  /**
   * @param file For this file
   * @throws AssertionError if this is called for anything except a sym link
   * @throws InaccessibleDataError as described in withWrappedError
   * @return Return the symlink target as a FilePath (ie, relative to baseDir). The target does not need to exist OR
   *         be accessible. Return None if the symlink points outside of baseDir
   */
  def getSymLinkPath(file: File): Option[FilePath]

  //TODO: eventually use the fancier "file watcher" mechanism
  @Loggable(value = Loggable.TRACE, limit = 1, unit = TimeUnit.MINUTES, prepend = true)
  override def listen(listener: FileTreeListener): PathGenerator = {
    //list all of the files
    val initialFiles = FileUtils.listFilesAndDirs(baseFolder, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE).par.map(file => {
      FilePathEvent(getFilePath(file), getLastModified(file))
    }).seq.toSet
    new PathGenerator(initialFiles, this, listener)
  }

  @Loggable(value = Loggable.TRACE, limit = 500, unit = TimeUnit.MILLISECONDS, prepend = true)
  override def read(path: FilePath): SafeInputStream = {
    SafeInputStream.fromFile(getFile(path))
  }

  @Loggable(value = Loggable.TRACE, limit = 500, unit = TimeUnit.MILLISECONDS, prepend = true)
  override def metadata(path: FilePath): Future[FilePathState] = Future {
    val file = getFile(path)
    withWrappedError(file, "get metadata") {case (fileId) => {
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
    }}
  }

  /**
   * @param file For this File
   * @return return the last time that it was modified
   */
  @Loggable(value = Loggable.TRACE, limit = 500, unit = TimeUnit.MILLISECONDS, prepend = true)
  protected[this] def getLastModified(file: File): DateTime = {
    val attributes = Files.readAttributes(Paths.get(file.getAbsolutePath), "lastModifiedTime", LinkOption.NOFOLLOW_LINKS)
    attributes.get("lastModifiedTime") match {
      case lastModified: FileTime => new DateTime(lastModified.toMillis, DateTimeZone.UTC)
    }
  }

  /**
   * @param file Converts this File
   * @return into a FilePath
   */
  protected[this] def getFilePath(file: File): FilePath = {
    FilePath(Paths.get(baseFolder.getAbsolutePath).relativize(Paths.get(file.getAbsolutePath)).toString)
  }

  /**
   * @param path Converts this FilePath
   * @return into a File
   */
  protected[this] def getFile(path: FilePath): File = {
    new File(baseFolder, path.path)
  }

  /**
   * @param file convert this File
   * @return into a Path
   */
  protected[this] def getPath(file: File): Path = {
    withWrappedError(file, "get the file path") {case (fileId) => {
      val path = Paths.get(file.toURI)
      if (Files.notExists(path)) throw new FileMissingError(s"does not exist: $fileId", null)
      if (!Files.isReadable(path)) throw new FilePermissionError(s"not readable: $fileId", null)
      path
    }}
  }

  /**
   * Consolidated error handling for access to the file-system. Turns the errors into something with semantics about
   * how it should be handled.
   *
   * @param file The file to operate on
   * @param action A description of what you're trying to do
   * @param func The actual operation to perform on the file. Passed fileId for easier/safer/more consistent logging
   * @tparam A The return value
   * @throws FilePermissionError if the file cannot be accessed for permissioning reasons
   * @throws FileMissingError if the file is missing
   * @throws InaccessibleDataError wrapping all other non fatal errors
   * @return The value from func
   */
  protected[this] def withWrappedError[A](file: File, action: String)(func: (String) => A): A = {
    val fileId = try {
      file.getAbsolutePath
    } catch {
      case e: SecurityException => throw new FilePermissionError(s"Failed to $action because the file is unavailable", e)
    }
    val failureString = s"Failed to $action on $fileId because "
    try {
      func(fileId)
    } catch {
      case e: FileSystemNotFoundException => throw new FileMissingError(s"the filesystem was not found because of ${e.getMessage}", e)
      case e: SecurityException => throw new FilePermissionError(s"there is no permission because of ${e.getMessage}", e)
      case e: IOException => throw new InaccessibleDataError(failureString + s"an error happened while accessing ${e.getMessage}", e)
    }
  }
}
