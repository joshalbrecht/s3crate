package com.codexica.s3crate.filetree.local

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.PosixFilePermission
import scala.concurrent.ExecutionContext
import com.codexica.s3crate.S3CrateError
import com.codexica.s3crate.filetree.FilePath
import com.codexica.s3crate.filetree.FilePathType
import java.nio.file.Files
import scala.util.Left
import com.codexica.s3crate.filetree.InaccessibleDataError
import com.codexica.s3crate.filetree.InaccessibleDataError
import java.nio.file.FileSystemNotFoundException
import com.codexica.s3crate.filetree.FilePermissionError
import com.codexica.s3crate.filetree.FileMissingError
import java.nio.file.InvalidPathException
import com.codexica.s3crate.filetree.SymLinkType
import com.codexica.s3crate.filetree.FolderType
import com.codexica.s3crate.filetree.FileType
import java.nio.file.attribute.GroupPrincipal
import java.nio.file.LinkOption
import java.nio.file.attribute.PosixFileAttributes

/**
 * A hierarchy of files and folders on linux.
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class LinuxFileTree(baseFolder: File, ec: ExecutionContext) extends LocalFileTree(baseFolder, ec) {
  
  private def getPath(file: File): Path = {
    if (file == null) throw new InaccessibleDataError("file parameter cannot be null", null)
    try {
      val path = Paths.get(file.toURI())
      if (Files.notExists(path)) throw new FileMissingError("file does not exist", null)
      if (!Files.isReadable(path)) throw new FilePermissionError("file is not readable", null)
      path
    } catch {
      case ex: InvalidPathException => throw new InaccessibleDataError("file path is invalid", ex)
      case ex: IllegalArgumentException => throw new InaccessibleDataError("", ex)
      case ex: FileSystemNotFoundException => throw new FileMissingError("", ex)
      case ex: SecurityException => throw new FilePermissionError("", ex)
    }
  }

  /**
   * @param file For this file
   * @throws FilePermissionError if the file cannot be accessed for permissioning reasons
   * @throws FileMissingError if the file is missing
   * @throws InaccessibleDataError wrapping all other non fatal errors
   * @return Return the appropriate FilePathType (folder, file, or symlink)
   */
  def getFileType(file: File): FilePathType = {
    val path = getPath(file) 
    try {
      if (Files.isSymbolicLink(path)) SymLinkType()
      else if (Files.isDirectory(path)) FolderType()
      else if (Files.isRegularFile(path)) FileType()
      else throw new InaccessibleDataError("wierd error. path is not a file, a directory or a symlink", null)
    }
    catch {
      case ex => throw new InaccessibleDataError("exception trying to find file type", ex)
    }
  }


  /**
   * @param file For this file
   * @throws FilePermissionError if the file cannot be accessed for permissioning reasons
   * @throws FileMissingError if the file is missing
   * @throws InaccessibleDataError wrapping all other non fatal errors
   * @return Return the appropriate FilePathType (folder, file, or symlink)
   */
  def getOwner(file: File): String = {
    val path = getPath(file) 
    try {
      Files.getOwner(path).getName()
    } catch {
      case ex => throw new InaccessibleDataError("exception trying to find path owner", ex)
    }
  }

  /**
   * @param file For this file
   * @throws FilePermissionError if the file cannot be accessed for permissioning reasons
   * @throws FileMissingError if the file is missing
   * @throws InaccessibleDataError wrapping all other non fatal errors
   * @return Return the appropriate FilePathType (folder, file, or symlink)
   */
  def getGroup(file: File): String = {
    val path = getPath(file) 
    try {
      Files.readAttributes(path, classOf[PosixFileAttributes], LinkOption.NOFOLLOW_LINKS).group().getName()
    } catch {
      case ex => throw new InaccessibleDataError("exception trying to find path owner", ex)
    }
  }

  /**
   * @param file For this file
   * @throws FilePermissionError if the file cannot be accessed for permissioning reasons
   * @throws FileMissingError if the file is missing
   * @throws InaccessibleDataError wrapping all other non fatal errors
   * @return Return the appropriate FilePathType (folder, file, or symlink)
   */
  def getPermissions(file: File): Set[PosixFilePermission] = throw new NotImplementedError()

  /**
   * @param file For this file
   * @throws AssertionError if this is called for anything except a sym link
   * @throws FilePermissionError if the file cannot be accessed for permissioning reasons
   * @throws FileMissingError if the file is missing
   * @throws InaccessibleDataError wrapping all other non fatal errors
   * @return Return the symlink target as a FilePath (ie, relative to baseDir). The target does not need to exist OR
   *         be accessible. Return None if the symlink points outside of baseDir
   */
  def getSymLinkPath(file: File): Option[FilePath] = throw new NotImplementedError()
}
