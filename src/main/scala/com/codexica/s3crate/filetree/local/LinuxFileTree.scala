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
import scala.collection.JavaConversions
import scala.collection.JavaConverters
import com.codexica.s3crate.filetree.SymLinkType

/**
 * A hierarchy of files and folders on linux.
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class LinuxFileTree(baseFolder: File, ec: ExecutionContext) extends LocalFileTree(baseFolder, ec) {
  
  private def getPath(file: File): Path = {
    //check for null parameter
    if (file == null) throw new InaccessibleDataError("file parameter cannot be null", null)
    try {
      //get Path from File
      val path = Paths.get(file.toURI())
      //check existence
      if (Files.notExists(path)) throw new FileMissingError("file does not exist", null)
      //check for read permissions
      if (!Files.isReadable(path)) throw new FilePermissionError("file is not readable", null)
      path
    } catch {
      //catch exceptions and route to custom exceptions
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
    //get Path from File
    val path = getPath(file) 
    try {
      //check for SymLink
      if (Files.isSymbolicLink(path)) SymLinkType()
      //check for directory
      else if (Files.isDirectory(path)) FolderType()
      //check for file (we can probably skip this check and return FileType, as no other
      //option currently exists. This is a precaution against the unknown
      else if (Files.isRegularFile(path)) FileType()
      //if not check succeeded - throw our generic InaccessibleDataError
      else throw new InaccessibleDataError("wierd error. path is not a file, a directory or a symlink", null)
    }
    catch {
      //and of course, if an exception was thrown somewhere, wrap it in InaccessibleDataError and re-throw
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
    //get Path from File
    val path = getPath(file) 
    try {
      //pretty straightforward, no?
      Files.getOwner(path).getName()
    } catch {
      //and of course, if an exception was thrown somewhere, wrap it in InaccessibleDataError and re-throw
      case ex => throw new InaccessibleDataError("exception trying to find file owner", ex)
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
    //get Path from File
    val path = getPath(file) 
    try {
      //read POSIX attributes, get group and return name
      Files.readAttributes(path, classOf[PosixFileAttributes], LinkOption.NOFOLLOW_LINKS).group().getName()
    } catch {
      //and of course, if an exception was thrown somewhere, wrap it in InaccessibleDataError and re-throw
      case ex => throw new InaccessibleDataError("exception trying to find file group", ex)
    }
  }

  /**
   * @param file For this file
   * @throws FilePermissionError if the file cannot be accessed for permissioning reasons
   * @throws FileMissingError if the file is missing
   * @throws InaccessibleDataError wrapping all other non fatal errors
   * @return Return the appropriate FilePathType (folder, file, or symlink)
   */
  def getPermissions(file: File): Set[PosixFilePermission] = {
    //get Path from File
    val path = getPath(file) 
    try{
      //to convert the Java set to Scala set (using asScala)
      import collection.JavaConverters._ 
      //get the permission set, convert to Scala mutable (asScala) and then to immutable (toSet) 
      Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS).asScala.toSet
    } catch {
      //and of course, if an exception was thrown somewhere, wrap it in InaccessibleDataError and re-throw
      case ex => throw new InaccessibleDataError("exception trying to find file permissions", ex)
    }
  }

  /**
   * @param file For this file
   * @throws AssertionError if this is called for anything except a sym link
   * @throws FilePermissionError if the file cannot be accessed for permissioning reasons
   * @throws FileMissingError if the file is missing
   * @throws InaccessibleDataError wrapping all other non fatal errors
   * @return Return the symlink target as a FilePath (ie, relative to baseDir). The target does not need to exist OR
   *         be accessible. Return None if the symlink points outside of baseDir
   */
  def getSymLinkPath(file: File): Option[FilePath] = {
    //get Path from File
    val path = getPath(file) 
    try{
      //if the file parameter is not a symlink - throw an error
      if (!Files.isSymbolicLink(path)) throw new AssertionError("file is not a symlink")
      //get symlink's target
      val target = Files.readSymbolicLink(path)
      //if target is a baseFolder child - return it's path in Some. else - return None
      if (target.startsWith(baseFolder.toPath())) Some(FilePath(target.toString())) else None
    } catch {
      //and of course, if an exception was thrown somewhere, wrap it in InaccessibleDataError and re-throw
      case ex => throw new InaccessibleDataError("exception trying to find symlink path", ex)
    }
  }
}