package com.codexica.s3crate.filetree.local

import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import java.io.File
import org.apache.commons.io.FileUtils
import scala.concurrent.ExecutionContext
import com.codexica.s3crate.filetree.FolderType

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class LocalFileTreeSpec extends Specification {

  trait Context extends Scope {
    val tree: LocalFileTree = new LinuxFileTree(new File(FileUtils.getTempDirectory, "_LinuxFileTreeSpecTest"), ExecutionContext.Implicits.global)
  }

  "listening to the folder" should {
    "return the existing files in the folder" in new Context {
      tree.getFileType(FileUtils.getTempDirectory) must be equalTo FolderType()
    }
    "return new paths when they are added to the folder" in new Context {
      throw new NotImplementedError()
    }
    "return new paths when metadata is modified" in new Context {
      throw new NotImplementedError()
    }
    "return new paths when a file or folder is deleted" in new Context {
      throw new NotImplementedError()
    }
    "return new paths when the contents of a file are changed" in new Context {
      throw new NotImplementedError()
    }
  }

  "reading a file from the folder" should {
    "return a safe readable input stream" in new Context {
      throw new NotImplementedError()
    }
    "throw an error for folders and symlinks" in new Context {
      throw new NotImplementedError()
    }
    "throw an error for paths that do not exist" in new Context {
      throw new NotImplementedError()
    }
  }

  "reading metadata from a path" should {
    "return correct metadata for files" in new Context {
      throw new NotImplementedError()
    }
    "return correct metadata for folders" in new Context {
      throw new NotImplementedError()
    }
    "return correct metadata for symlinks" in new Context {
      throw new NotImplementedError()
    }
    "throw an error for paths that do not exist" in new Context {
      throw new NotImplementedError()
    }
  }

}
