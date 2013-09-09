package com.codexica.s3crate.filetree.local

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import java.io.File
import org.apache.commons.io.FileUtils
import scala.concurrent.ExecutionContext
import com.codexica.s3crate.filetree._
import java.nio.file.{Paths, Files}
import com.codexica.s3crate.filetree.FileType
import com.codexica.s3crate.filetree.FolderType

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class LinuxFileTreeSpec extends Specification {

  trait Context extends Scope {
    val tree = new LinuxFileTree(new File(FileUtils.getTempDirectory, "_LinuxFileTreeSpecTest"), ExecutionContext.Implicits.global)
  }

  "getting the file type" should {
    "return FolderType() for folders" in new Context {
      tree.getFileType(FileUtils.getTempDirectory) must be equalTo FolderType()
    }
    "return FileType() for files" in new Context {
      val file = new File(FileUtils.getTempDirectory, "unqiueFileNameBecauseitsmispelld")
      FileUtils.write(file, "hello")
      tree.getFileType(file) must be equalTo FileType()
    }
    "return correctly for symlinks" in new Context {
      val file = new File("/tmp/linuxFileTestlink")
      Files.createSymbolicLink(Paths.get(file.getAbsolutePath), Paths.get(FileUtils.getUserDirectoryPath))
      tree.getFileType(file) must be equalTo SymLinkType()
    }
    "throw an InaccessibleDataError if the user does not have permission to read the file" in new Context {
      tree.getFileType(new File("/root")) must throwAn[InaccessibleDataError]
    }
    "throw a FileMissingError if the file is missing" in new Context {
      tree.getFileType(new File("/definitely/does/not/exist")) must throwA[FileMissingError]
    }
  }
}
