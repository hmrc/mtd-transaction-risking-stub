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
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.*
import play.api.libs.json.{JsNull, JsObject, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, contentAsString, defaultAwaitTimeout, status, stubControllerComponents}
import uk.gov.hmrc.mtdtransactionriskingstub.services.RdsReportService
import uk.gov.hmrc.mtdtransactionriskingstub.utils.StubPeriodKeys

class RdsControllerSpec extends AnyWordSpec, Matchers, OptionValues:

  private val controller = new RdsController(stubControllerComponents())
  private val fixedId = "2dd537bc-4244-4ebf-bac9-96321be13cdc"
  private val vrn = "123456789"

  private given mat: Materializer = Materializer(system)

  private given system: ActorSystem = ActorSystem("test")

  private def acknowledgeRequest(correlationId: String): JsValue = inputs(
    "correlationId" -> Json.toJson(correlationId),
    "feedbackId" -> Json.toJson(fixedId),
    "vrn" -> Json.toJson(vrn),
    "presentedDateTime" -> Json.toJson("2026-02-15T09:35:15.094Z")
  )

  private def inputs(entries: (String, JsValue)*): JsValue =
    Json.obj("inputs" -> entries.map((name, value) => Json.obj("name" -> name, "value" -> value)))

  private def generateFor(periodKey: String) = generate(reportRequest(periodKey))

  private def reportRequest(periodKey: String): JsValue = inputs(
    "fixedId" -> Json.toJson(fixedId),
    "vrn" -> Json.toJson(vrn),
    "periodKey" -> Json.toJson(periodKey),
    "startDate" -> Json.toJson("2026-01-01"),
    "endDate" -> Json.toJson("2026-03-31"),
    "customerType" -> Json.toJson("T"),
    "vatDueSales" -> Json.toJson(100.00)
  )

  private def generate(body: JsValue) =
    controller.generateReport()(
      FakeRequest("POST", "/rds/assessments/generate")
        .withBody(body)
        .withHeaders("X-CorrelationId" -> "test-correlation-id")
    )

  private def acknowledge(body: JsValue) =
    controller.acknowledge()(FakeRequest("POST", "/rds/assessments/acknowledge").withBody(body))

  private def output(json: JsValue, name: String): Option[JsValue] =
    outputs(json).find(o => (o \ "name").as[String] == name).map(o => (o \ "value").as[JsValue])

  private def outputs(json: JsValue): Seq[JsObject] = (json \ "outputs").as[Seq[JsObject]]

  private def columns(grid: JsValue): Seq[String] =
    (grid \ 0 \ "metadata").as[Seq[JsObject]].flatMap(_.keys)

  private def rows(grid: JsValue): Seq[Seq[String]] =
    (grid \ 1 \ "data").as[Seq[Seq[String]]]

  "generateReport" should :

    "return 201 with the outputs RDS returns, and no responseCode" in :
      val result = generateFor("AB12")

      status(result) shouldBe CREATED

      val names = outputs(contentAsJson(result)).map(o => (o \ "name").as[String])
      names should contain allOf("correlationId", "createdDttm", "englishActions", "feedbackId", "vrn", "welshActions", "rt_Record_Contacts_Outer")
      names should not contain "responseCode"

    "echo fixedId as the feedbackId, and the vrn" in :
      val json = contentAsJson(generateFor("AB12"))

      output(json, "feedbackId").value.as[String] shouldBe fixedId
      output(json, "vrn").value.as[String] shouldBe vrn

    "return the stub correlation id" in :
      output(contentAsJson(generateFor("AB12")), "correlationId").value.as[String] shouldBe RdsReportService.rdsCorrelationId

    "return upper-case grid columns with separate link title and url" in :
      val english = output(contentAsJson(generateFor("AB12")), "englishActions").value

      columns(english) shouldBe Seq("ITEMNUMBER", "MESSAGE", "ACTION", "TITLE", "LINKTITLE", "LINKURL", "PATH")
      rows(english).head.head shouldBe "1"
      rows(english).head(4) shouldBe "VAT Notice 700"
      rows(english).head(5) shouldBe "https://www.gov.uk/guidance/vat-notice-700-the-vat-guide"
      rows(english).head(6) shouldBe "vatDueSales"

    "return createdDttm without a timezone offset, as RDS does" in :
      output(contentAsJson(generateFor("AB12")), "createdDttm").value.as[String] should not endWith "Z"

    "return a single item 0 message with path 0, and no correlation id, when no feedback applies" in :
      val json = contentAsJson(generateFor(StubPeriodKeys.rdsNoFeedback))

      output(json, "correlationId").value shouldBe JsNull

      Seq("englishActions", "welshActions").foreach { grid =>
        val data = rows(output(json, grid).value)
        data should have size 1
        data.head.head shouldBe "0"
        data.head.last shouldBe "0"
      }

    "return a correlation id when there is feedback" in :
      output(contentAsJson(generateFor("AB12")), "correlationId").value.as[String] shouldBe RdsReportService.rdsCorrelationId

    "return 201 with a body that is not a report when the malformed key is used" in :
      val result = generateFor(StubPeriodKeys.rdsMalformedReport)

      status(result) shouldBe CREATED
      (contentAsJson(result) \ "outputs").toOption shouldBe None

    "return 400 with a non-JSON body when the bad-request key is used" in :
      val result = generateFor(StubPeriodKeys.rdsBadRequest)

      status(result) shouldBe BAD_REQUEST
      contentAsString(result) should include("Unexpected close marker")

    "return 404 when the not-found key is used" in :
      status(generateFor(StubPeriodKeys.rdsNotFound)) shouldBe NOT_FOUND

    "return 503 when the service-unavailable key is used" in :
      status(generateFor(StubPeriodKeys.rdsServiceUnavailable)) shouldBe SERVICE_UNAVAILABLE

    "return the default report when the request has no period key" in :
      val result = generate(inputs("fixedId" -> Json.toJson(fixedId)))

      status(result) shouldBe CREATED
      rows(output(contentAsJson(result), "englishActions").value).map(_.head) shouldBe Seq("1")

    "generate a distinct feedback id for each report when no fixedId is sent" in :
      val feedbackIdOf = (json: JsValue) => output(json, "feedbackId").value.as[String]

      val first = feedbackIdOf(contentAsJson(generate(inputs("periodKey" -> Json.toJson("AB12")))))
      val second = feedbackIdOf(contentAsJson(generate(inputs("periodKey" -> Json.toJson("AB12")))))

      first should not be second

  "acknowledge" should :

    "return 201 with responseCode 202 when the correlation id matches the one the stub issued" in :
      val result = acknowledge(acknowledgeRequest(RdsReportService.rdsCorrelationId))
      val json = contentAsJson(result)

      status(result) shouldBe CREATED
      output(json, "vrn").value.as[String] shouldBe vrn
      output(json, "feedbackId").value.as[String] shouldBe fixedId
      output(json, "responseCode").value.as[Int] shouldBe 202
      output(json, "responseMessage").value.as[String] shouldBe "Acknowledgement accepted"
      output(json, "createdDttm") shouldBe defined

    "return 201 with responseCode 401 when the correlation id does not match" in :
      val result = acknowledge(acknowledgeRequest("9EEB55EF4FA9A24954BC982DF1D59B3D02BC097F6B1377B8B335C7583D92B959"))
      val json = contentAsJson(result)

      status(result) shouldBe CREATED
      output(json, "responseCode").value.as[Int] shouldBe 401
      output(json, "responseMessage").value.as[String] shouldBe "Acknowledgement validation failed"

    "send responseCode as a int" in :
      contentAsString(acknowledge(acknowledgeRequest(RdsReportService.rdsCorrelationId))) should include("\"value\":202")

    "use an lower-case vrn output name" in :
      outputs(contentAsJson(acknowledge(acknowledgeRequest(RdsReportService.rdsCorrelationId))))
        .map(o => (o \ "name").as[String]) should contain("vrn")

    "return 400 when the body is not an inputs envelope" in :
      status(acknowledge(Json.obj("unexpected" -> "shape"))) shouldBe BAD_REQUEST
