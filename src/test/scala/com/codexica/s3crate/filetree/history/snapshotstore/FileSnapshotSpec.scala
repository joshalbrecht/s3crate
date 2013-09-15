package com.codexica.s3crate.filetree.history.snapshotstore

import play.api.libs.json.Json
import java.util.UUID
import org.joda.time.DateTime
import com.codexica.s3crate.filetree.{FileType, FileMetaData, FilePath}
import com.codexica.s3crate.filetree.history.{SnappyCompression, FilePathState}
import com.codexica.encryption.{KeyPairReference, EncryptionDetails}
import java.nio.file.attribute.PosixFilePermission
import com.codexica.common.SafeLogSpecification

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class FileSnapshotSpec extends SafeLogSpecification {
  "Serialization" should {
    "deserialize as exactly the same value" in {
      val state = FilePathState(
        FilePath("local/path"),
        exists = true,
        Option(
          FileMetaData(
            FileType(),
            23534897L,
            new DateTime(),
            "owner",
            "group",
            Set(PosixFilePermission.GROUP_WRITE, PosixFilePermission.OWNER_READ, PosixFilePermission.OTHERS_WRITE),
            None,
            isHidden = false
          )
        )
      )
      val snapshot = FileSnapshot(
        UUID.randomUUID(),
        DataBlob(
          "some/path",
          Option(EncryptionDetails(
            "secret_key".getBytes.toList,
            KeyPairReference(UUID.randomUUID())
          )),
          SnappyCompression()
        ),
        state,
        Option(UUID.randomUUID()))
      val jsonData = Json.stringify(Json.toJson(snapshot))
      val result = Json.parse(jsonData).as[FileSnapshot]
      result must be equalTo snapshot
    }
  }
}
