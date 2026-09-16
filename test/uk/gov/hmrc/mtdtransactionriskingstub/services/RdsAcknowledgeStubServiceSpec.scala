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

package uk.gov.hmrc.mtdtransactionriskingstub.services

import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.UNAUTHORIZED
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.mtdtransactionriskingstub.utils.StubResource

class RdsAcknowledgeStubServiceSpec extends AnyWordSpec, Matchers, MockitoSugar:

  private val stubResource = mock[StubResource]
  private val service      = new RdsAcknowledgeStubService(stubResource)

  private val vrn        = "123456789"
  private val feedbackId = "cbbb77b9-6779-f24c-b2a6-6f4c27da7900"

  "acknowledgeResponse" should:

    "return a SuccessResponse containing the vrn and feedbackId supplied by the caller for the DEFAULT scenario" in:
      val successBody: JsValue = Json.obj(
        "output" -> Json.obj(
          "vrn"             -> vrn,
          "feedbackId"      -> feedbackId,
          "createdDttm"     -> "2026-02-15T09:35:15.094Z",
          "responseCode"    -> 202,
          "responseMessage" -> "Acknowledgement accepted"
        )
      )

      when(stubResource.loadRdsAcknowledgeResponse(eqTo("default-acknowledge.json"), eqTo(vrn), eqTo(feedbackId)))
        .thenReturn(successBody)

      service.acknowledgeResponse("DEFAULT", vrn, feedbackId) shouldBe Some(SuccessResponse(successBody))

    "pass through the exact vrn and feedbackId it was given, without altering them" in:
      val otherVrn        = "987654321"
      val otherFeedbackId = "11111111-2222-4333-8444-555555555555"

      when(stubResource.loadRdsAcknowledgeResponse(eqTo("default-acknowledge.json"), eqTo(otherVrn), eqTo(otherFeedbackId)))
        .thenReturn(Json.obj())

      service.acknowledgeResponse("DEFAULT", otherVrn, otherFeedbackId)

      org.mockito.Mockito
        .verify(stubResource)
        .loadRdsAcknowledgeResponse(eqTo("default-acknowledge.json"), eqTo(otherVrn), eqTo(otherFeedbackId))

    "return a 401 error response for the VALIDATION_FAILED scenario, ignoring the supplied vrn and feedbackId" in:
      val errorBody = Json.obj("code" -> "VALIDATION_FAILED", "reason" -> "Acknowledgement validation failed")
      when(stubResource.loadErrorResponse(eqTo("rds/acknowledge"), eqTo("error-validation-failed.json")))
        .thenReturn(errorBody)

      service.acknowledgeResponse("VALIDATION_FAILED", vrn, feedbackId) shouldBe Some(ErrorResponse(UNAUTHORIZED, errorBody))

    "return None for an unrecognised scenario" in:
      service.acknowledgeResponse("SOMETHING_ELSE", vrn, feedbackId) shouldBe None

    "return None when vrn and feedbackId are blank and the scenario is unrecognised" in:
      service.acknowledgeResponse("SOMETHING_ELSE", "", "") shouldBe None