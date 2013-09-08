package com.codexica.s3crate.filesystem.remote

import play.api.libs.json.Json
import org.specs2.mutable.Specification
import java.util.UUID
import com.codexica.s3crate.filesystem.{Created}
import org.joda.time.DateTime
import com.codexica.s3crate.common.models._

/**
 * @author Josh Albrecht (joshalbrecht@gmail.com)
 */
class RemoteSnapshotSpec extends Specification {
  "Serialization" should {
    "deserialize as exactly the same value" in {
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
        FilePathState(
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
      val result = Json.parse(jsonData).as[FileSnapshot]
      result must be equalTo snapshot
    }
  }
}
