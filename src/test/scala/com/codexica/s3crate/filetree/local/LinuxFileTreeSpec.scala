package com.codexica.s3crate.filetree.local

import java.io.File
import org.apache.commons.io.FileUtils
import scala.concurrent.ExecutionContext
import com.codexica.s3crate.filetree._
import java.nio.file.{Paths, Files}
import com.codexica.s3crate.filetree.FileType
import com.codexica.s3crate.filetree.FolderType
import java.nio.file.LinkOption
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFileAttributeView
import com.codexica.common.SafeLogSpecification

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */

class LinuxFileTreeSpec extends SafeLogSpecification {

  trait Context extends BaseContext {
    val baseFolder = new File(FileUtils.getTempDirectory, "_LinuxFileTreeSpecTest")
    val tree = new LinuxFileTree(baseFolder, ExecutionContext.Implicits.global)
    val homeUri = Paths.get(new File(System.getProperty("user.home")).toURI)
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
      val file = new File("/tmp/linuxFileOwnerTest")
      if (!file.exists()) {
        file.createNewFile()
      }
      val path = Paths.get(file.toURI)
      val owner = Files.getOwner(homeUri)
      Files.setOwner(path, owner)
      tree.getOwner(file) must be equalTo owner.getName
    }
    "throw a FilePermissionError if the user name cannot be determined because of permissions" in new Context {
      tree.getOwner(new File("/root")) must throwA[FilePermissionError]
    }
    "throw a FileMissingError if the file is missing" in new Context {
      tree.getOwner(new File("/definitely/does/not/exist")) must throwA[FileMissingError]
    }
  }

  "getting the group" should {
    "return the group's name for regular files" in new Context {
      val file = new File("/tmp/linuxFileGroupTest")
      if (!file.exists()) {
        file.createNewFile()
      }
      val path = Paths.get(file.toURI)
      val group = Files.readAttributes(homeUri, classOf[PosixFileAttributes], LinkOption.NOFOLLOW_LINKS).group()
      val view = Files.getFileAttributeView(path,classOf[PosixFileAttributeView])
      view.setGroup(group)
      tree.getOwner(file) must be equalTo group.getName
    }
    "throw a FilePermissionError if the group name cannot be determined because of permissions" in new Context {
      tree.getGroup(new File("/root")) must throwA[FilePermissionError]
    }
    "throw a FileMissingError if the file is missing" in new Context {
      tree.getGroup(new File("/definitely/does/not/exist")) must throwA[FileMissingError]
    }
  }

  "getting the permissions" should {
    "return the correct permissions" in new Context {
      val file = new File("/tmp/linuxFilePermissionTest")
      if (!file.exists()) {
        file.createNewFile()
      }
      val path = Paths.get(file.toURI)
      val permissions = Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS)
      import collection.JavaConverters._
      tree.getPermissions(file) must be equalTo permissions.asScala.toSet
    }
    "throw a FilePermissionError if the permissions cannot be determined because of permissions" in new Context {
      tree.getPermissions(new File("/root")) must throwA[FilePermissionError]
    }
    "throw a FileMissingError if the file is missing" in new Context {
      tree.getPermissions(new File("/definitely/does/not/exist")) must throwA[FileMissingError]
    }
  }

  "getting the symlink path" should {
    "return the None if symlink target is out of baseFolder" in new Context {
      val linkToExternal = new File("/tmp/linuxFileSymlinkExternalTargetTest")
      if (linkToExternal.exists()) {
        assert(linkToExternal.delete())
      }
      val homePath = Paths.get(FileUtils.getUserDirectoryPath)
      val symlinkOutside = Files.createSymbolicLink(Paths.get(linkToExternal.getAbsolutePath), homePath)
      tree.getSymLinkPath(symlinkOutside.toFile) must be equalTo None
    }

    "return the correct symlink path" in new Context {
      val linkToInternal = new File("/tmp/linuxFileSymlinklinkInternalTargetTest")
      if (linkToInternal.exists()) {
        assert(linkToInternal.delete())
      }
      val tempPath = Paths.get(FileUtils.getTempDirectoryPath)
      val symlinkOutside = Files.createSymbolicLink(Paths.get(linkToInternal.getAbsolutePath), tempPath)
      tree.getSymLinkPath(symlinkOutside.toFile) must be equalTo None
    }
      
    "throw a FilePermissionError if the symlink path cannot be determined because of permissions" in new Context {
      tree.getSymLinkPath(new File("/root")) must throwA[FilePermissionError]
    }
    "throw a FileMissingError if the symlink is missing" in new Context {
      tree.getSymLinkPath(new File("/definitely/does/not/exist")) must throwA[FileMissingError]
    }
  }
}
