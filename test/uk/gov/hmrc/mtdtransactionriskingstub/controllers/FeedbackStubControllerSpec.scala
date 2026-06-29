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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.JsArray
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers.*
import uk.gov.hmrc.mtdtransactionriskingstub.utils.StubResource
import uk.gov.hmrc.mtdtransactionriskingstub.services.FeedbackStubService

class FeedbackStubControllerSpec extends AnyWordSpec, Matchers:

  private val controller =
    new FeedbackStubController(Helpers.stubControllerComponents(), new FeedbackStubService(new StubResource))

  "requestFeedback" should:

    "return 200 with single feedback when no Gov-Test-Scenario header is present" in:
      val result = controller.requestFeedback()(FakeRequest("POST", "/feedback"))

      status(result) shouldBe OK
      headers(result).get("X-CorrelationId") shouldBe defined
      (contentAsJson(result) \ "englishFeedback").as[JsArray].value should not be empty

    "return 200 with single feedback for Gov-Test-Scenario: DEFAULT" in:
      val request = FakeRequest("POST", "/feedback").withHeaders("Gov-Test-Scenario" -> "DEFAULT")
      val result  = controller.requestFeedback()(request)

      status(result) shouldBe OK
      (contentAsJson(result) \ "englishFeedback").as[JsArray].value should not be empty

    "return 200 with multiple feedback for Gov-Test-Scenario: MULTIPLE_FEEDBACK" in:
      val request = FakeRequest("POST", "/feedback").withHeaders("Gov-Test-Scenario" -> "MULTIPLE_FEEDBACK")
      val result  = controller.requestFeedback()(request)

      status(result) shouldBe OK
      (contentAsJson(result) \ "englishFeedback").as[JsArray].value.size should be > 1

    "return 200 with empty arrays for Gov-Test-Scenario: NO_FEEDBACK" in:
      val request = FakeRequest("POST", "/feedback").withHeaders("Gov-Test-Scenario" -> "NO_FEEDBACK")
      val result  = controller.requestFeedback()(request)

      status(result) shouldBe OK
      (contentAsJson(result) \ "englishFeedback").as[JsArray].value shouldBe empty

    "return 400 for an unmatched Gov-Test-Scenario" in:
      val request = FakeRequest("POST", "/feedback").withHeaders("Gov-Test-Scenario" -> "NONSENSE")
      val result  = controller.requestFeedback()(request)

      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] shouldBe "TEST_ONLY_UNMATCHED_STUB_ERROR"
      
    "return 400 for INVALID_VRN scenario" in :
      val request = FakeRequest("POST", "/feedback")
        .withHeaders("Gov-Test-Scenario" -> "INVALID_VRN")
      val result = controller.requestFeedback()(request)

      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] shouldBe "VRN_INVALID"

    "return 400 for INVALID_PERIODKEY scenario" in :
      val request = FakeRequest("POST", "/feedback")
        .withHeaders("Gov-Test-Scenario" -> "INVALID_PERIODKEY")
      val result = controller.requestFeedback()(request)

      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] shouldBe "INVALID_REQUEST"
      (contentAsJson(result) \ "errors").as[JsArray].value should not be empty

    "return 400 for INVALID_REQUEST scenario" in :
      val request = FakeRequest("POST", "/feedback")
        .withHeaders("Gov-Test-Scenario" -> "INVALID_REQUEST")
      val result = controller.requestFeedback()(request)

      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] shouldBe "INVALID_REQUEST"

    "return 403 for TAX_PERIOD_NOT_ENDED scenario" in :
      val request = FakeRequest("POST", "/feedback")
        .withHeaders("Gov-Test-Scenario" -> "TAX_PERIOD_NOT_ENDED")
      val result = controller.requestFeedback()(request)

      status(result) shouldBe FORBIDDEN
      (contentAsJson(result) \ "code").as[String] shouldBe "TAX_PERIOD_NOT_ENDED"

    "return 403 for INSOLVENT_TRADER scenario" in :
      val request = FakeRequest("POST", "/feedback")
        .withHeaders("Gov-Test-Scenario" -> "INSOLVENT_TRADER")
      val result = controller.requestFeedback()(request)

      status(result) shouldBe FORBIDDEN
      (contentAsJson(result) \ "code").as[String] shouldBe "RULE_INSOLVENT_TRADER"