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
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import java.nio.file.attribute.UserPrincipal
import java.nio.file.attribute.UserPrincipalLookupService
import java.nio.file.FileSystem
import java.nio.file.attribute.GroupPrincipal
import java.nio.file.LinkOption
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFileAttributeView
/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */

@RunWith(classOf[JUnitRunner])
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
      val file = new File("/tmp/linuxFileOwnerTest")
      if (!file.exists()) {
        file.createNewFile()
      }
      val path = Paths.get(file.toURI())
      val owner: UserPrincipal = Files.getOwner(Paths.get(new File(System.getProperty("user.home")).toURI()))
      Files.setOwner(path, owner)
      tree.getOwner(file) must be equalTo(owner.getName())
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
      val path = Paths.get(file.toURI())
      val group: GroupPrincipal = Files.readAttributes(Paths.get(new File(System.getProperty("user.home")).toURI()), classOf[PosixFileAttributes], LinkOption.NOFOLLOW_LINKS).group()
      val view: PosixFileAttributeView = Files.getFileAttributeView(path,classOf[PosixFileAttributeView]);
      view.setGroup(group)
      tree.getOwner(file) must be equalTo(group.getName())
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
      val path = Paths.get(file.toURI())
      val permissions = Files.getPosixFilePermissions(path, LinkOption.NOFOLLOW_LINKS)
      import collection.JavaConverters._ 

      tree.getPermissions(file) must be equalTo(permissions.asScala.toSet)
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
      val symlinkOutside = Files.createSymbolicLink(Paths.get(linkToExternal.getAbsolutePath), Paths.get(FileUtils.getUserDirectoryPath))

      tree.getSymLinkPath(symlinkOutside.toFile()) must be equalTo None
      
    }

    "return the correct symlink path" in new Context {
      val linkToInternal = new File("/tmp/linuxFileSymlinklinkInternalTargetTest")
      if (linkToInternal.exists()) {
        assert(linkToInternal.delete())
      }
      val symlinkOutside = Files.createSymbolicLink(Paths.get(linkToInternal.getAbsolutePath), Paths.get(FileUtils.getTempDirectoryPath()))

      tree.getSymLinkPath(symlinkOutside.toFile()) must be equalTo None
      
    }
      
    "throw a FilePermissionError if the symlink path cannot be determined because of permissions" in new Context {
      tree.getSymLinkPath(new File("/root")) must throwA[FilePermissionError]
    }
    "throw a FileMissingError if the symlink is missing" in new Context {
      tree.getSymLinkPath(new File("/definitely/does/not/exist")) must throwA[FileMissingError]
    }
  }
}
