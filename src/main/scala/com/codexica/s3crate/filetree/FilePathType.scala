package com.codexica.s3crate.filetree

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
sealed trait FilePathType
case class FolderType()
case class FileType()
case class SymLinkType()
