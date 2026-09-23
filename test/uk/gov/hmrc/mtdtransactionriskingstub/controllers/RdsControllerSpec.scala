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
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.*
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, contentAsString, defaultAwaitTimeout, header, status, stubControllerComponents}
import uk.gov.hmrc.mtdtransactionriskingstub.utils.StubPeriodKeys

class RdsControllerSpec extends AnyWordSpec, Matchers:

  private given system: ActorSystem  = ActorSystem("test")
  private given mat:    Materializer = Materializer(system)

  private val controller = new RdsController(stubControllerComponents())

  private def reportRequest(periodKey: String): JsValue = Json.obj(
    "fixedId"      -> "2dd537bc-4244-4ebf-bac9-96321be13cdc",
    "periodKey"    -> periodKey,
    "startDate"    -> "2026-01-01",
    "endDate"      -> "2026-03-31",
    "customerType" -> "T",
    "vatDueSales"  -> 100.00
  )

  private def acknowledgeRequest(vrn: String, feedbackId: String) = Json.obj(
    "inputs" -> Json.arr(
      Json.obj("name" -> "correlationId", "value" -> "9EEB55EF4FA9A24954BC982DF1D59B3D02BC097F6B1377B8B335C7583D92B959"),
      Json.obj("name" -> "feedbackId", "value" -> feedbackId),
      Json.obj("name" -> "vrn", "value" -> vrn),
      Json.obj("name" -> "presentedDateTime", "value" -> "2026-02-15T09:35:15.094Z")
    )
  )

  private def requestWith(periodKey: String) =
    FakeRequest("POST", "/rds/assessments/generate")
      .withBody(reportRequest(periodKey))
      .withHeaders("X-CorrelationId" -> "test-correlation-id")

  "generateReport" should:

    "return 201 with a report for an unreserved period key" in:
      val result = controller.generateReport()(requestWith("AB12"))

      status(result) shouldBe CREATED

      val outputs = (contentAsJson(result) \ "outputs").as[Seq[JsValue]]
      outputs.map(output => (output \ "name").as[String]) should contain allOf ("feedbackId", "correlationId", "responseCode", "englishActions", "welshActions")

    "return an inner responseCode of 201 on the happy path" in:
      val result  = controller.generateReport()(requestWith("AB12"))
      val outputs = (contentAsJson(result) \ "outputs").as[Seq[JsValue]]

      outputs.find(output => (output \ "name").as[String] == "responseCode").map(output => (output \ "value").as[String]) shouldBe Some("201")

    "return 201 with empty datagrids when no feedback applies" in:
      val result  = controller.generateReport()(requestWith(StubPeriodKeys.rdsNoFeedback))
      val outputs = (contentAsJson(result) \ "outputs").as[Seq[JsValue]]

      status(result) shouldBe CREATED

      val englishData = outputs
        .find(output => (output \ "name").as[String] == "englishActions")
        .map(output => (output \ "value" \\ "data").flatMap(_.as[Seq[JsValue]]))

      englishData.value shouldBe empty

    "return 201 with a body that is not a report when the malformed key is used" in:
      val result = controller.generateReport()(requestWith(StubPeriodKeys.rdsMalformedReport))

      status(result) shouldBe CREATED
      (contentAsJson(result) \ "outputs").toOption shouldBe None

    "return 400 with a non-JSON body when the bad-request key is used" in:
      val result = controller.generateReport()(requestWith(StubPeriodKeys.rdsBadRequest))

      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include("Unexpected close marker")

    "return 404 when the not-found key is used" in:
      status(controller.generateReport()(requestWith(StubPeriodKeys.rdsNotFound))) shouldBe NOT_FOUND

    "return 503 when the service-unavailable key is used" in:
      status(controller.generateReport()(requestWith(StubPeriodKeys.rdsServiceUnavailable))) shouldBe SERVICE_UNAVAILABLE

    "return 201 when the request has no period key" in:
      val result = controller.generateReport()(
        FakeRequest("POST", "/rds/assessments/generate")
          .withBody(Json.obj("fixedId" -> "abc")))

      status(result) shouldBe CREATED

    "generate a distinct feedback id for each report" in:
      val outputsOf = (result: JsValue) =>
        (result \ "outputs").as[Seq[JsValue]].find(o => (o \ "name").as[String] == "feedbackId").map(o => (o \ "value").as[String])

      val first  = outputsOf(contentAsJson(controller.generateReport()(requestWith("AB12"))))
      val second = outputsOf(contentAsJson(controller.generateReport()(requestWith("AB12"))))

      first should not be second

  "acknowledge" should:

    "return 200" in:
      val result = controller.acknowledge()(
        FakeRequest("POST", "/rds/acknowledge")
          .withJsonBody(acknowledgeRequest("123456789", "feedback-id"))
      )

      status(result) shouldBe OK
      (contentAsJson(result) \ "output" \ "vrn").as[String] shouldBe "123456789"
      (contentAsJson(result) \ "output" \ "feedbackId").as[String] shouldBe "feedback-id"
      (contentAsJson(result) \ "output" \ "responseCode").as[Int] shouldBe 202
      (contentAsJson(result) \ "output" \ "createdDttm").as[String] shouldBe "2026-06-09T10:30:00Z"
      (contentAsJson(result) \ "output" \ "responseMessage").as[String] shouldBe "Acknowledgement accepted"
      header("X-CorrelationId", result) shouldBe defined

    "return 400 without body" in:
      val result = controller.acknowledge()(
        FakeRequest("POST", "/rds/acknowledge").withHeaders("Content-Type" -> "application/json")
      )

      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "output" \ "responseMessage").as[String] shouldBe "Acknowledgement validation failed"
      (contentAsJson(result) \ "output" \ "responseCode").as[Int] shouldBe 401
