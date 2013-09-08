package com.codexica.s3crate.filetree.local

import java.io.File
import com.codexica.s3crate.filetree.{FilePathType, FilePath}
import java.nio.file.attribute.PosixFilePermission
import scala.concurrent.ExecutionContext

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class LinuxFileTree(baseFolder: File, ec: ExecutionContext) extends LocalFileTree(baseFolder, ec) {

  def getFileType(file: File): FilePathType = throw new NotImplementedError()

  def getOwner(file: File): String = throw new NotImplementedError()

  def getGroup(file: File): String = throw new NotImplementedError()

  def getPermissions(file: File): Set[PosixFilePermission] = throw new NotImplementedError()

  def getSymLinkPath(file: File): Option[FilePath] = throw new NotImplementedError()
}
