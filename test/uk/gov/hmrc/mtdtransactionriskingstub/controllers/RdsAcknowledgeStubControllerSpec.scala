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
import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.*
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, header, status, stubControllerComponents}
import uk.gov.hmrc.mtdtransactionriskingstub.services.{ErrorResponse, RdsAcknowledgeStubService, SuccessResponse}

class RdsAcknowledgeStubControllerSpec extends AnyWordSpec, Matchers, MockitoSugar:

  private given system: ActorSystem  = ActorSystem("test")
  private given mat:    Materializer = Materializer(system)

  private val mockService = mock[RdsAcknowledgeStubService]
  private val controller  = new RdsAcknowledgeStubController(stubControllerComponents(), mockService)

  private val vrn        = "123456789"
  private val feedbackId = "cbbb77b9-6779-f24c-b2a6-6f4c27da7900"

  private def requestBody(vrn: String, feedbackId: String) = Json.obj(
    "inputs" -> Json.arr(
      Json.obj("name" -> "correlationId",     "value" -> "9EEB55EF4FA9A24954BC982DF1D59B3D02BC097F6B1377B8B335C7583D92B959"),
      Json.obj("name" -> "feedbackId",        "value" -> feedbackId),
      Json.obj("name" -> "vrn",               "value" -> vrn),
      Json.obj("name" -> "presentedDateTime", "value" -> "2026-02-15T09:35:15.094Z")
    )
  )

  private def requestWith(scenario: Option[String], body: play.api.libs.json.JsValue = requestBody(vrn, feedbackId)) =
    val base = FakeRequest("POST", "/rds/acknowledge").withJsonBody(body)
    scenario.fold(base)(s => base.withHeaders("Gov-Test-Scenario" -> s))

  "acknowledge" should:

    "extract the vrn and feedbackId from the request and pass them unchanged to the service" in:
      val successBody = Json.obj(
        "output" -> Json.obj(
          "vrn"        -> vrn,
          "feedbackId" -> feedbackId
        )
      )
      when(mockService.acknowledgeResponse(eqTo("DEFAULT"), eqTo(vrn), eqTo(feedbackId)))
        .thenReturn(Some(SuccessResponse(successBody)))

      val result = controller.acknowledge()(requestWith(None))

      status(result) shouldBe OK
      contentAsJson(result) shouldBe successBody
      header("X-CorrelationId", result) shouldBe defined

    "return 400 with the error body relayed from the service for the VALIDATION_FAILED scenario" in:
      val errorBody = Json.obj("code" -> "VALIDATION_FAILED", "reason" -> "Acknowledgement validation failed")
      when(mockService.acknowledgeResponse(eqTo("VALIDATION_FAILED"), eqTo(vrn), eqTo(feedbackId)))
        .thenReturn(Some(ErrorResponse(UNAUTHORIZED, errorBody)))

      val result = controller.acknowledge()(requestWith(Some("VALIDATION_FAILED")))

      status(result) shouldBe UNAUTHORIZED
      contentAsJson(result) shouldBe errorBody
      header("X-CorrelationId", result) shouldBe defined

    "return 400 TEST_ONLY_UNMATCHED_STUB_ERROR when the service does not recognise the scenario" in:
      when(mockService.acknowledgeResponse(eqTo("randomInput"), eqTo(vrn), eqTo(feedbackId)))
        .thenReturn(None)

      val result = controller.acknowledge()(requestWith(Some("randomInput")))

      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String]   shouldBe "TEST_ONLY_UNMATCHED_STUB_ERROR"
      (contentAsJson(result) \ "reason").as[String] should include("randomInput")

    "default vrn and feedbackId to empty strings when the request body is not JSON" in:
      when(mockService.acknowledgeResponse(eqTo("DEFAULT"), eqTo(""), eqTo("")))
        .thenReturn(Some(SuccessResponse(Json.obj())))

      val result = controller.acknowledge()(FakeRequest("POST", "/rds/acknowledge").withTextBody("not json"))

      status(result) shouldBe OK

    "default vrn and feedbackId to empty strings when the inputs array is missing them" in:
      val bodyMissingFields = Json.obj("inputs" -> Json.arr(
        Json.obj("name" -> "correlationId", "value" -> "some-correlation-id")
      ))

      when(mockService.acknowledgeResponse(eqTo("DEFAULT"), eqTo(""), eqTo("")))
        .thenReturn(Some(SuccessResponse(Json.obj())))

      val result = controller.acknowledge()(requestWith(None, bodyMissingFields))

      status(result) shouldBe OK

    "treat a Gov-Test-Scenario value of '-' as DEFAULT" in:
      when(mockService.acknowledgeResponse(eqTo("DEFAULT"), eqTo(vrn), eqTo(feedbackId)))
        .thenReturn(Some(SuccessResponse(Json.obj())))

      val result = controller.acknowledge()(requestWith(Some("-")))

      status(result) shouldBe OK

    "use different vrn/feedbackId values sent in the request without mixing them up" in:
      val otherVrn        = "987654321"
      val otherFeedbackId = "11111111-2222-4333-8444-555555555555"

      when(mockService.acknowledgeResponse(eqTo("DEFAULT"), eqTo(otherVrn), eqTo(otherFeedbackId)))
        .thenReturn(Some(SuccessResponse(Json.obj("output" -> Json.obj("vrn" -> otherVrn, "feedbackId" -> otherFeedbackId)))))

      val result = controller.acknowledge()(requestWith(None, requestBody(otherVrn, otherFeedbackId)))

      status(result) shouldBe OK
      (contentAsJson(result) \ "output" \ "vrn").as[String]        shouldBe otherVrn
      (contentAsJson(result) \ "output" \ "feedbackId").as[String] shouldBe otherFeedbackId