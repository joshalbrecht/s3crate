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
    "return SymLinkType() for symlinks" in new Context {
      val file = new File("/tmp/linuxFileTestlink")
      if (file.exists()) {
        assert(file.delete())
      }
      Files.createSymbolicLink(Paths.get(file.getAbsolutePath), Paths.get(FileUtils.getUserDirectoryPath))
      tree.getFileType(file) must be equalTo SymLinkType()
    }
    "throw a FilePermissionError if the user does not have permission to read the file" in new Context {
      tree.getFileType(new File("/root")) must throwA[FilePermissionError]
    }
    "throw a FileMissingError if the file is missing" in new Context {
      tree.getFileType(new File("/definitely/does/not/exist")) must throwA[FileMissingError]
    }
  }

  "getting the owner" should {
    "return the user's name for regular files" in new Context {
      throw new NotImplementedError()
    }
    "throw a FilePermissionError if the user name cannot be determined because of permissions" in new Context {
      throw new NotImplementedError()
    }
    "throw a FileMissingError if the file is missing" in new Context {
      throw new NotImplementedError()
    }
  }

  "getting the group" should {
    "return the group's name for regular files" in new Context {
      throw new NotImplementedError()
    }
    "throw a FilePermissionError if the group name cannot be determined because of permissions" in new Context {
      throw new NotImplementedError()
    }
    "throw a FileMissingError if the file is missing" in new Context {
      throw new NotImplementedError()
    }
  }

  "getting the permissions" should {
    "return the correct permissions" in new Context {
      throw new NotImplementedError()
    }
    "throw a FilePermissionError if the permissions cannot be determined because of permissions" in new Context {
      throw new NotImplementedError()
    }
    "throw a FileMissingError if the file is missing" in new Context {
      throw new NotImplementedError()
    }
  }

  "getting the symlink path" should {
    //also have appropriate tests defined :)
  }
}
