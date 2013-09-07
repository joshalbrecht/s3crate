package com.codexica.s3crate.filesystem.remote

import play.api.libs.json.Json
import org.specs2.mutable.Specification
import java.util.UUID
import com.codexica.s3crate.filesystem.{Created, FilePath, FileMetaData, FileSnapshot}
import org.joda.time.DateTime

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class RemoteSnapshotSpec extends Specification {
  "Serialization" should {
    "deserialize as exactly the same value" in {
      val snapshot = RemoteSnapshot(
        UUID.randomUUID(),
        RemoteBlobData(
          "some/path",
          RemoteEncryptionDetails(
            "secret_key".getBytes().toList,
            SimpleEncryption()
          ),
          false
        ),
        FileSnapshot(
          FilePath("local/path"),
          Created(),
          FileMetaData(
            new DateTime(),
            new DateTime(),
            23534897L,
            "linux",
            Map("property 1" -> "sadfsadf", "otehr" -> "hello")
          )
        ),
        Option(UUID.randomUUID()))
      val jsonData = Json.stringify(Json.toJson(snapshot))
      val result = Json.parse(jsonData).as[RemoteSnapshot]
      result must be equalTo snapshot
    }
  }
}
