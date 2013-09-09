package com.codexica.s3crate.filetree.local

import java.io.File
import com.codexica.s3crate.filetree.{FilePathType, FilePath}
import java.nio.file.attribute.PosixFilePermission
import scala.concurrent.ExecutionContext

/**
 * A hierarchy of files and folders on linux.
 *
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class LinuxFileTree(baseFolder: File, ec: ExecutionContext) extends LocalFileTree(baseFolder, ec) {

  /**
   * @param file For this file
   * @throws FilePermissionError if the file cannot be accessed for permissioning reasons
   * @throws FileMissingError if the file is missing
   * @throws InaccessibleDataError wrapping all other non fatal errors
   * @return Return the appropriate FilePathType (folder, file, or symlink)
   */
  def getFileType(file: File): FilePathType = throw new NotImplementedError()

  /**
   * @param file For this file
   * @throws FilePermissionError if the file cannot be accessed for permissioning reasons
   * @throws FileMissingError if the file is missing
   * @throws InaccessibleDataError wrapping all other non fatal errors
   * @return Return the appropriate FilePathType (folder, file, or symlink)
   */
  def getOwner(file: File): String = throw new NotImplementedError()

  /**
   * @param file For this file
   * @throws FilePermissionError if the file cannot be accessed for permissioning reasons
   * @throws FileMissingError if the file is missing
   * @throws InaccessibleDataError wrapping all other non fatal errors
   * @return Return the appropriate FilePathType (folder, file, or symlink)
   */
  def getGroup(file: File): String = throw new NotImplementedError()

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
