package com.codexica.s3crate.filetree

import com.codexica.common.SealedTraitFormat

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
sealed trait FilePathType
case class FolderType() extends FilePathType
case class FileType() extends FilePathType
case class SymLinkType() extends FilePathType

object FilePathType {
  implicit val format = new SealedTraitFormat[FilePathType](FolderType(), FileType(), SymLinkType())
}
