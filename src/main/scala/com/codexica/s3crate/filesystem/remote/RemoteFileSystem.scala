package com.codexica.s3crate.filesystem.remote

import java.io.{BufferedOutputStream, FileOutputStream, File, InputStream}
import scala.concurrent.Future
import java.util.UUID
import com.codexica.s3crate.filesystem._
import scala.Some
import org.xerial.snappy.SnappyInputStream
import java.security.MessageDigest
import scala.collection.mutable
import play.api.libs.json.Json
import org.apache.commons.io.FileUtils
import org.jets3t.service.utils.ServiceUtils
import com.codexica.s3crate.common.models._
import scala.Some
import com.codexica.s3crate.filetree.{FilePath, FilePathEvent}
import com.codexica.s3crate.filetree.history.FilePathState
import com.codexica.s3crate.filetree.history.snapshotstore._
import scala.Some
import com.codexica.s3crate.filetree.FilePathEvent
import com.codexica.encryption.{Encryption, EncryptionDetails, EncryptionMethod, SimpleEncryption}
import com.codexica.s3crate.{FutureUtils, Contexts}
import com.codexica.s3crate.filetree.history.snapshotstore.s3.{S3Interface}

//TODO: would be nice for this to support resumption of previous multi-part uploads
/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class RemoteFileSystem( metaPublicKey: Array[Byte], metaPrivateKey: Array[Byte], blobPublicKey: Array[Byte], blobPrivateKey: Array[Byte]) extends FileSystem {





  private def getMetaLocation(snapshot: FileSnapshot): String = {
    throw new NotImplementedError()
  }



  private def previousVersion(path: FilePath): Option[FileSnapshot] = {
    throw new NotImplementedError()
  }






}
