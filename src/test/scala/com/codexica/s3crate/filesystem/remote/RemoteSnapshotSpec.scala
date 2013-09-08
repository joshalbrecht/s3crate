package com.codexica.s3crate.filesystem.remote

import play.api.libs.json.Json
import org.specs2.mutable.Specification
import java.util.UUID
import org.joda.time.DateTime
import com.codexica.s3crate.filetree.{FileType, FileMetaData, FilePath}
import com.codexica.s3crate.filetree.history.FilePathState
import com.codexica.s3crate.filetree.history.snapshotstore.{DataBlob, FileSnapshot}
import com.codexica.encryption.{EncryptionDetails, SimpleEncryption}
import java.nio.file.attribute.PosixFilePermission

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class RemoteSnapshotSpec extends Specification {
  "Serialization" should {
    "deserialize as exactly the same value" in {
      val state = FilePathState(
        FilePath("local/path"),
        true,
        Option(
          FileMetaData(
            FileType(),
            23534897L,
            new DateTime(),
            "owner",
            "group",
            Set(PosixFilePermission.GROUP_WRITE, PosixFilePermission.OWNER_READ, PosixFilePermission.OTHERS_WRITE),
            None,
            false
          )
        )
      )
      val snapshot = FileSnapshot(
        UUID.randomUUID(),
        DataBlob(
          "some/path",
          EncryptionDetails(
            "secret_key".getBytes().toList,
            SimpleEncryption()
          ),
          false
        ),
        state,
        Option(UUID.randomUUID()))
      val jsonData = Json.stringify(Json.toJson(snapshot))
      val result = Json.parse(jsonData).as[FileSnapshot]
      result must be equalTo snapshot
    }
  }
}
