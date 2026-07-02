package uk.gov.hmrc.mtdtransactionriskingstub.services

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.mtdtransactionriskingstub.utils.StubResource

class AcknowledgeStubServiceSpec extends AnyWordSpec, Matchers:

  private val service = new AcknowledgeStubService(new StubResource)

  "AcknowledgeFor" should:

    "return a success response for DEFAULT" in:
      val result = service.AcknowledgeFor("DEFAULT")

      result shouldBe defined
      result.get shouldBe a[SuccessResponse]
      result.get.asInstanceOf[SuccessResponse].body shouldBe Json.obj()

    "return 400 ErrorResponse for INVALID_VRN" in:
      val result = service.AcknowledgeFor("INVALID_VRN")

      result shouldBe defined
      result.get shouldBe a[ErrorResponse]
      result.get.asInstanceOf[ErrorResponse].status shouldBe 400

    "return 400 ErrorResponse for FORMAT_RECEIPT_ID" in:
      val result = service.AcknowledgeFor("FORMAT_RECEIPT_ID")

      result shouldBe defined
      result.get shouldBe a[ErrorResponse]
      result.get.asInstanceOf[ErrorResponse].status shouldBe 400

    "return 400 ErrorResponse for FORMAT_DATETIME" in:
      val result = service.AcknowledgeFor("FORMAT_DATETIME")

      result shouldBe defined
      result.get shouldBe a[ErrorResponse]
      result.get.asInstanceOf[ErrorResponse].status shouldBe 400

    "return 401 ErrorResponse for INVALID_CREDENTIALS" in:
      val result = service.AcknowledgeFor("INVALID_CREDENTIALS")

      result shouldBe defined
      result.get shouldBe a[ErrorResponse]
      result.get.asInstanceOf[ErrorResponse].status shouldBe 401

    "return 403 ErrorResponse for CLIENT_OR_AGENT_NOT_AUTHORISED" in:
      val result = service.AcknowledgeFor("CLIENT_OR_AGENT_NOT_AUTHORISED")

      result shouldBe defined
      result.get shouldBe a[ErrorResponse]
      result.get.asInstanceOf[ErrorResponse].status shouldBe 403

    "return 403 ErrorResponse for CORRELATION_ID" in:
      val result = service.AcknowledgeFor("CORRELATION_ID")

      result shouldBe defined
      result.get shouldBe a[ErrorResponse]
      result.get.asInstanceOf[ErrorResponse].status shouldBe 403

    "return 404 ErrorResponse for MATCHING_RESOURCE_NOT_FOUND" in:
      val result = service.AcknowledgeFor("MATCHING_RESOURCE_NOT_FOUND")

      result shouldBe defined
      result.get shouldBe a[ErrorResponse]
      result.get.asInstanceOf[ErrorResponse].status shouldBe 404

    "return None for an unknown scenario" in:
      service.AcknowledgeFor("NONSENSE") shouldBe None