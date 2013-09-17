package com.codexica.s3crate.filetree.history.snapshotstore

import com.codexica.common.SafeLogSpecification
import org.specs2.specification.Scope
import com.codexica.s3crate.filetree.history.{SnappyCompression, FilePathState}
import com.codexica.s3crate.filetree.{FileType, FileMetaData, FilePath}
import org.joda.time.DateTime
import java.nio.file.attribute.PosixFilePermission
import java.util.UUID
import com.codexica.encryption.{KeyPairReference, EncryptionDetails}
import scala.util.Random
import play.api.libs.json.Json
import org.xerial.snappy.Snappy

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class SnapshotStoreSizeExperiment extends SafeLogSpecification {
  trait Context extends BaseContext {
    val random = new Random(1337)
    def generateSnapshot(): FileSnapshot = {
      val state = FilePathState(
        FilePath(random.nextString(32)),
        exists = true,
        Option(
          FileMetaData(
            FileType(),
            random.nextLong(),
            new DateTime(random.nextLong()),
            "owner",
            "group",
            Set(PosixFilePermission.GROUP_WRITE, PosixFilePermission.OWNER_READ, PosixFilePermission.OTHERS_WRITE),
            None,
            isHidden = false
          )
        )
      )
      FileSnapshot(
        UUID.randomUUID(),
        DataBlob(
          random.nextString(32),
          Option(EncryptionDetails(
            random.nextString(256).getBytes.toList,
            KeyPairReference(UUID.randomUUID())
          )),
          SnappyCompression()
        ),
        state,
        Option(UUID.randomUUID()))
    }
  }

  "A ridiculous number of snapshots" should {
    "not take up too much room" in new Context {
      //This is too big :(
      val snapshotData = (0 to 10000).map(i => Json.stringify(Json.toJson(generateSnapshot()))).mkString("\n")
      val finalLength = Snappy.compress(snapshotData).size
      val x = 4
      //new ideas to reduce metadata size down to something more reasonable without sacrificing encryption:
      // - stop storing the keys. Just derive them from the UUID and the private key. Then we don't need two keys either.
      // - stop storing the UUIDs in the object as it is serialized--that's just the name of the file, no need to duplicate information
      // - write in a more efficient format and be careful about what we store. Maybe make a json <-> thrift converter someday
      //     maybe even best to make the in-memory version the thrift one too, probably much smaller
      // - can probably fit the whole thing into an AES256 chunk
      // - eventually can make consolidated snapshot files, lists of snapshots that happened during a certain period, to consolidate the number of files in S3
    }
  }
}
