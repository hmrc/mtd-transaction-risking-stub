package uk.gov.hmrc.mtdtransactionriskingstub.models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class AcknowledgeModelsStub extends AnyWordSpec, Matchers:

  "AcknowledgeStubRequest" should:
    "round-trip through JSON" in:
      val model = AcknowledgeStubRequest("123456789", "123456aB-012A-345B-678C-012345678abc","a1e8057e-fbbc-47a8-a8b4-78d9f015c253", java.time.OffsetDateTime.parse("2023-06-01T12:00:00Z"))
      val json  = Json.parse("""{"vrn": "123456789", "reportId": "123456aB-012A-345B-678C-012345678abc", "correlationId": "a1e8057e-fbbc-47a8-a8b4-78d9f015c253", "presentedDateTime": "2023-06-01T12:00:00Z"}""")

      Json.toJson(model) shouldBe json
      json.as[AcknowledgeStubRequest] shouldBe model