/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.mtdtransactionriskingstub.controllers

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.*
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.mtdtransactionriskingstub.services.NrsStubService

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.util.Base64

class NrsStubControllerSpec extends AnyWordSpec, Matchers:

  private val nrsStubService = new NrsStubService
  private val controller =
    new NrsStubController(
      stubControllerComponents(),
      nrsStubService
    )
  private val vrn = "123456789"
  private val reportId = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
  private val payloadJson =
    Json.obj(
      "periodKey" -> "18AD",
      "vatDueSales" -> 105.50
    )
  private val payloadBytes =
    Json.stringify(payloadJson).getBytes(UTF_8)
  private val validBody: JsObject =
    Json.obj(
      "payload" -> Base64.getEncoder.encodeToString(payloadBytes),
      "metadata" -> Json.obj(
        "businessId" -> "vata",
        "notableEvent" -> "vata-request-feedback",
        "payloadContentType" -> "application/json",
        "payloadSha256Checksum" -> sha256(payloadBytes),
        "userSubmissionTimestamp" -> "2026-10-02T10:15:30Z",
        "identityData" -> Json.obj(
          "internalId" -> "internal-id"
        ),
        "userAuthToken" -> "Bearer vendor-token",
        "headerData" -> Json.obj(),
        "searchKeys" -> Json.obj(
          "vrn" -> vrn,
          "reportId" -> reportId
        )
      )
    )

  private given materializer: Materializer =
    Materializer(system)

  private given system: ActorSystem =
    ActorSystem("NrsStubControllerSpec")

  private def request(
                       body: JsValue = validBody,
                       apiKey: String = "nrs-test-api-key",
                       correlationId: String = "a1e8057e-fbbc-47a8-a8b4-78d9f015c253"
                     ) =
    FakeRequest("POST", "/nrs-orchestrator/submission")
      .withBody(body)
      .withHeaders(
        "X-API-Key" -> apiKey,
        "X-Correlation-Id" -> correlationId,
        "Content-Type" -> "application/json"
      )

  private def sha256(bytes: Array[Byte]): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(bytes)
      .map("%02x".format(_))
      .mkString

  "NrsStubController.submit" should :

    "return 202 and an NRS submission ID for a valid default submission" in :
      val result = controller.submit()(request())

      status(result) shouldBe ACCEPTED
      (contentAsJson(result) \ "nrSubmissionId").as[String] should not be empty

    "return 401 when X-API-Key is invalid" in :
      val result = controller.submit()(
        request(apiKey = "invalid-key")
      )

      status(result) shouldBe UNAUTHORIZED
      (contentAsJson(result) \ "code").as[String] shouldBe "NRS_INVALID_API_KEY"

    "return 400 when X-Correlation-Id is not a UUID" in :
      val result = controller.submit()(
        request(correlationId = "not-a-uuid")
      )

      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] shouldBe "NRS_INVALID_CORRELATION_ID"

    "return 400 for an invalid NRS submission body" in :
      val result = controller.submit()(
        request(body = Json.obj("payload" -> "payload-only"))
      )

      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] shouldBe "NRS_INVALID_SUBMISSION"

    "return 400 when the business ID is not vata" in :
      val body =
        validBody.deepMerge(
          Json.obj(
            "metadata" -> Json.obj(
              "businessId" -> "other-business"
            )
          )
        )

      val result = controller.submit()(request(body = body))

      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] shouldBe "NRS_INVALID_BUSINESS_ID"

    "return 400 for an unsupported notable event" in :
      val body =
        validBody.deepMerge(
          Json.obj(
            "metadata" -> Json.obj(
              "notableEvent" -> "unexpected-event"
            )
          )
        )

      val result = controller.submit()(request(body = body))

      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] shouldBe "NRS_UNSUPPORTED_NOTABLE_EVENT"

    "return 400 when the payload checksum does not match" in :
      val body =
        validBody.deepMerge(
          Json.obj(
            "metadata" -> Json.obj(
              "payloadSha256Checksum" -> "invalid-checksum"
            )
          )
        )

      val result = controller.submit()(request(body = body))

      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] shouldBe "NRS_INVALID_PAYLOAD_CHECKSUM"

    "return 400 when the payload is not Base64-encoded JSON" in :
      val body =
        validBody.deepMerge(
          Json.obj(
            "payload" -> "not-base64"
          )
        )

      val result = controller.submit()(request(body = body))

      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] shouldBe "NRS_INVALID_PAYLOAD"

    "return 202 for each notable event mtd-transaction-risking sends" in :
      Seq("vata-request-feedback", "vata-generate-report", "vata-acknowledge-report").foreach { event =>
        val body = validBody.deepMerge(Json.obj("metadata" -> Json.obj("notableEvent" -> event)))

        withClue(s"$event: ") {
          status(controller.submit()(request(body = body))) shouldBe ACCEPTED
        }
      }

    "accept a submission whose headerData carries a vendor Gov-Test-Scenario" in :
      val body = validBody.deepMerge(
        Json.obj("metadata" -> Json.obj("headerData" -> Json.obj("Gov-Test-Scenario" -> "MULTIPLE_FEEDBACK")))
      )

      status(controller.submit()(request(body = body))) shouldBe ACCEPTED

    "return 400 when the payload content type is not application/json" in :
      val body = validBody.deepMerge(Json.obj("metadata" -> Json.obj("payloadContentType" -> "text/plain")))

      val result = controller.submit()(request(body = body))

      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] shouldBe "NRS_INVALID_PAYLOAD_CONTENT_TYPE"

  "NrsStubController.submissions" should :

    "return a redacted summary of a captured submission for the supplied VRN and report ID" in :
      status(controller.submit()(request())) shouldBe ACCEPTED

      val result = controller.submissions(vrn, reportId)(
        FakeRequest("GET", s"/nrs-orchestrator/submissions?vrn=$vrn&reportId=$reportId")
      )

      status(result) shouldBe OK

      val submissions =
        (contentAsJson(result) \ "submissions").as[Seq[JsValue]]

      submissions should not be empty

      val submission = submissions.head

      (submission \ "businessId").as[String] shouldBe "vata"
      (submission \ "notableEvent").as[String] shouldBe "vata-request-feedback"
      (submission \ "vrn").as[String] shouldBe vrn
      (submission \ "reportId").as[String] shouldBe reportId
      (submission \ "payloadContentType").as[String] shouldBe "application/json"
      (submission \ "receivedAt").toOption shouldBe defined

      (submission \ "userAuthToken").toOption shouldBe None
      (submission \ "identityData").toOption shouldBe None
      (submission \ "payload").toOption shouldBe None