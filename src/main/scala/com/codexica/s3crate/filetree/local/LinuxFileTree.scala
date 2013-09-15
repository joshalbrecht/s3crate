package com.codexica.s3crate.filetree.local

import java.io.File
import java.nio.file.attribute.PosixFilePermission
import scala.concurrent.ExecutionContext
import com.codexica.s3crate.filetree.FilePath
import com.codexica.s3crate.filetree.FilePathType
import java.nio.file.Files
import com.codexica.s3crate.filetree.SymLinkType
import com.codexica.s3crate.filetree.FolderType
import com.codexica.s3crate.filetree.FileType
import java.nio.file.LinkOption
import java.nio.file.attribute.PosixFileAttributes
import org.slf4j.LoggerFactory
import com.jcabi.aspects.Loggable
import java.util.concurrent.TimeUnit

/**
 * A hierarchy of files and folders on linux.
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class LinuxFileTree(baseFolder: File, ec: ExecutionContext) extends LocalFileTree(baseFolder, ec) {

  val logger = LoggerFactory.getLogger(getClass)

  @Loggable(value = Loggable.TRACE, limit = 500, unit = TimeUnit.MILLISECONDS, prepend = true)
  def getFileType(file: File): FilePathType = {
    val path = getPath(file)
    withWrappedError(file, "get the file type") {
      case (fileId) => {
        path match {
          case p if Files.isSymbolicLink(p) => SymLinkType()
          case p if Files.isDirectory(p) => FolderType()
          case p if Files.isRegularFile(p) => FileType()
        }
      }
    }
  }

  @Loggable(value = Loggable.TRACE, limit = 500, unit = TimeUnit.MILLISECONDS, prepend = true)
  def getOwner(file: File): String = {
    val path = getPath(file)
    withWrappedError(file, "get the file owner") {
      case (fileId) => {
        Files.getOwner(path).getName
      }
    }
  }

  @Loggable(value = Loggable.TRACE, limit = 500, unit = TimeUnit.MILLISECONDS, prepend = true)
  def getGroup(file: File): String = {
    val path = getPath(file)
    withWrappedError(file, "get the file group") {
      case (fileId) => {
        Files.readAttributes(path, classOf[PosixFileAttributes], LinkOption.NOFOLLOW_LINKS).group().getName
      }
    }
  }

  @Loggable(value = Loggable.TRACE, limit = 500, unit = TimeUnit.MILLISECONDS, prepend = true)
  def getPermissions(file: File): Set[PosixFilePermission] = {
    val path = getPath(file)
    withWrappedError(file, "get file permissions") {
      case (fileId) => {
        import collection.JavaConverters._
        Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS).asScala.toSet
      }
    }
  }

  @Loggable(value = Loggable.TRACE, limit = 500, unit = TimeUnit.MILLISECONDS, prepend = true)
  def getSymLinkPath(file: File): Option[FilePath] = {
    val path = getPath(file)
    withWrappedError(file, "get file permissions") {
      case (fileId) => {
        if (!Files.isSymbolicLink(path)) throw new AssertionError("file is not a symlink")
        val target = Files.readSymbolicLink(path)
        //if target is a baseFolder child - return its path in Some. else - return None
        if (target.startsWith(baseFolder.toPath)) {
          Some(FilePath(target.toString))
        } else {
          logger.warn(s"Ignoring a symbolic link because it points outside of the synchronized folder: $fileId")
          None
        }
      }
    }
  }
}