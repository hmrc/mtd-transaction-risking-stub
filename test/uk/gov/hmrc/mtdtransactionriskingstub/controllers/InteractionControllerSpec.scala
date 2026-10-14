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
import play.api.test.Helpers.{contentAsJson, contentType, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.mtdtransactionriskingstub.utils.StubPeriodKeys

class InteractionControllerSpec extends AnyWordSpec, Matchers:

  private given system: ActorSystem  = ActorSystem("test")
  private given mat:    Materializer = Materializer(system)

  private val controller = new InteractionController(stubControllerComponents())

  private def action(path: Option[String]): JsObject =
    Json.obj(
      "itemNumber" -> 1,
      "title"      -> "VAT title",
      "body"       -> "VAT body",
      "action"     -> "VAT action",
      "links"      -> Json.arr(Json.obj("linkTitle" -> "VAT guidance", "linkUrl" -> "https://www.gov.uk/vat-returns"))
    ) ++ path.fold(Json.obj())(p => Json.obj("path" -> p))

  private def message(englishPath: Option[String] = Some("vatDueSales"), welshPath: Option[String] = Some("vatDueSales")): JsObject =
    Json.obj("englishActions" -> action(englishPath), "welshActions" -> action(welshPath))

  private def generateReportInteraction(periodKey: String, messages: Seq[JsObject] = Seq(message())): JsValue =
    Json.obj(
      "serviceRegime"  -> "vat-assist",
      "eventName"      -> "generate-report",
      "feedbackId"     -> "f2fb30e5-4ab6-4a29-b3c1-c00000000001",
      "eventTimestamp" -> "2026-08-13T09:00:00Z",
      "metadata" -> Json.arr(
        Json.obj(
          "vrn"                  -> "123456789",
          "start"                -> "2026-01-01",
          "end"                  -> "2026-03-31",
          "additionalProperties" -> Json.obj("periodKey" -> periodKey)
        )
      ),
      "payload" -> Json.obj("reportId" -> "f2fb30e5-4ab6-4a29-b3c1-c00000000001", "messages" -> messages)
    )

  private val acknowledgementInteraction: JsValue =
    Json.obj(
      "serviceRegime"  -> "vat-assist",
      "eventName"      -> "acknowledge-report",
      "feedbackId"     -> "f2fb30e5-4ab6-4a29-b3c1-c00000000001",
      "eventTimestamp" -> "2026-08-13T09:00:00Z",
      "metadata"       -> Json.arr(Json.obj("vrn" -> "123456789", "additionalProperties" -> Json.obj("presentedDateTime" -> "2026-08-13T09:00:00Z"))),
      "payload"        -> Json.obj("reportId" -> "f2fb30e5-4ab6-4a29-b3c1-c00000000001")
    )

  private def store(body: JsValue, correlationId: Option[String] = Some("test-correlation-id")) =
    val request = FakeRequest("POST", "/rsd/receive-and-store").withBody(body)
    controller.store()(correlationId.fold(request)(id => request.withHeaders("CorrelationId" -> id)))

  private def failures(json: JsValue): Seq[(String, String)] =
    (json \ "response" \ "failures").as[Seq[JsObject]].map(f => ((f \ "type").as[String], (f \ "reason").as[String]))

  private def shouldBeHipFailure(result: scala.concurrent.Future[play.api.mvc.Result], expectedStatus: Int) =
    status(result) shouldBe expectedStatus
    contentType(result) shouldBe Some("application/json")
    (contentAsJson(result) \ "origin").as[String] shouldBe "HIP"
    failures(contentAsJson(result)) should not be empty

  "store" when:

    "the interaction is valid" should:

      "return 204 for a generate-report interaction" in:
        status(store(generateReportInteraction("AB12"))) shouldBe NO_CONTENT

      "return 204 for an acknowledgement interaction" in:
        status(store(acknowledgementInteraction)) shouldBe NO_CONTENT

      "accept a request with no CorrelationId header" in:
        status(store(generateReportInteraction("AB12"), correlationId = None)) shouldBe NO_CONTENT

    "a message is missing path, as RSD rejects it" should:

      "return 400 naming the english actions" in:
        val result = store(generateReportInteraction("AB12", Seq(message(englishPath = None))))

        shouldBeHipFailure(result, BAD_REQUEST)
        failures(contentAsJson(result)) shouldBe Seq(
          "body.schema.required" -> "Path '/payload/messages/0/englishActions' validation failed."
        )

      "return 400 naming every action that is missing path" in:
        val result = store(generateReportInteraction("AB12", Seq(message(), message(englishPath = None, welshPath = None))))

        status(result) shouldBe BAD_REQUEST
        failures(contentAsJson(result)).map(_._2) shouldBe Seq(
          "Path '/payload/messages/1/englishActions' validation failed.",
          "Path '/payload/messages/1/welshActions' validation failed."
        )

    "the periodKey selects a failure scenario" should:

      "return 400 in the HIP shape for the bad-request key" in:
        val result = store(generateReportInteraction(StubPeriodKeys.rsdBadRequest))

        shouldBeHipFailure(result, BAD_REQUEST)
        failures(contentAsJson(result)).head._1 shouldBe "body.schema.required"

      "return 429 in the HIP shape for the too-many-requests key" in:
        shouldBeHipFailure(store(generateReportInteraction(StubPeriodKeys.rsdTooManyRequests)), TOO_MANY_REQUESTS)

      "return 500 with an empty body for the server-error key" in:
        val result = store(generateReportInteraction(StubPeriodKeys.rsdServerError))

        status(result) shouldBe INTERNAL_SERVER_ERROR
        contentAsJson(result) shouldBe Json.obj()

      "return 503 in the HIP shape for the service-unavailable key" in:
        shouldBeHipFailure(store(generateReportInteraction(StubPeriodKeys.rsdServiceUnavailable)), SERVICE_UNAVAILABLE)