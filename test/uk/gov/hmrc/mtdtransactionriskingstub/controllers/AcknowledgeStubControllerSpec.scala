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
import play.api.test.Helpers.{stubControllerComponents, contentAsJson, defaultAwaitTimeout, header, status}
import uk.gov.hmrc.mtdtransactionriskingstub.services.{AcknowledgeStubService, ErrorResponse, SuccessResponse}

class AcknowledgeStubControllerSpec extends AnyWordSpec, Matchers, MockitoSugar:

  private given system: ActorSystem  = ActorSystem("test")
  private given mat:    Materializer = Materializer(system)

  private val mockService = mock[AcknowledgeStubService]
  private val controller  = new AcknowledgeStubController(stubControllerComponents(), mockService)

  private def requestWith(scenario: Option[String]) =
    val base = FakeRequest("POST", "/acknowledge")
    scenario.fold(base)(s => base.withHeaders("Gov-Test-Scenario" -> s))

  "requestAcknowledge" should:

    "return 204 No Content with a correlation header when the scenario resolves to a success" in:
      when(mockService.acknowledgeFor(eqTo("DEFAULT")))
        .thenReturn(Some(SuccessResponse(Json.obj())))

      val result = controller.requestAcknowledge()(requestWith(None))

      status(result) shouldBe NO_CONTENT
      header("X-CorrelationId", result) shouldBe defined

    "relay the error status and body with a correlation header when the scenario resolves to an error" in:
      val errorBody = Json.obj("code" -> "FORMAT_REPORT_ID", "message" -> "The provided Report ID is invalid")
      when(mockService.acknowledgeFor(eqTo("INVALID_REPORTID")))
        .thenReturn(Some(ErrorResponse(BAD_REQUEST, errorBody)))

      val result = controller.requestAcknowledge()(requestWith(Some("INVALID_REPORTID")))

      status(result) shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe errorBody
      header("X-CorrelationId", result) shouldBe defined

    "relay a 403 error for the CORRELATION_ID scenario" in:
      val errorBody = Json.obj("code" -> "CORRELATION_ID", "message" -> "The Correlation ID is not the expected value for this report")
      when(mockService.acknowledgeFor(eqTo("CORRELATION_ID")))
        .thenReturn(Some(ErrorResponse(FORBIDDEN, errorBody)))

      val result = controller.requestAcknowledge()(requestWith(Some("CORRELATION_ID")))

      status(result) shouldBe FORBIDDEN
      contentAsJson(result) shouldBe errorBody

    "return 400 TEST_ONLY_UNMATCHED_STUB_ERROR when the service does not recognise the scenario" in:
      when(mockService.acknowledgeFor(eqTo("NONSENSE")))
        .thenReturn(None)

      val result = controller.requestAcknowledge()(requestWith(Some("NONSENSE")))

      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String]   shouldBe "TEST_ONLY_UNMATCHED_STUB_ERROR"
      (contentAsJson(result) \ "reason").as[String] should include("NONSENSE")

    "treat a Gov-Test-Scenario value of '-' as DEFAULT" in:
      when(mockService.acknowledgeFor(eqTo("DEFAULT")))
        .thenReturn(Some(SuccessResponse(Json.obj())))

      val result = controller.requestAcknowledge()(requestWith(Some("-")))

      status(result) shouldBe NO_CONTENT