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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.JsArray
import uk.gov.hmrc.mtdtransactionriskingstub.utils.StubResource

class FeedbackStubServiceSpec extends AnyWordSpec, Matchers:


  private val service = new FeedbackStubService(new StubResource)

  "feedbackFor" should:

    "return a single feedback response for DEFAULT" in :
      val result = service.feedbackFor("DEFAULT")
      val body = result.get.asInstanceOf[SuccessResponse].body

      result shouldBe defined
      result.get shouldBe a[SuccessResponse]
      (body \ "englishFeedback").as[JsArray].value should not be empty

    "return multiple feedback for MULTIPLE_FEEDBACK" in :
      val result = service.feedbackFor("MULTIPLE_FEEDBACK")
      val body = result.get.asInstanceOf[SuccessResponse].body

      result shouldBe defined
      result.get shouldBe a[SuccessResponse]
      (body \ "englishFeedback").as[JsArray].value.size should be > 1

    "return empty arrays for NO_FEEDBACK" in :
      val result = service.feedbackFor("NO_FEEDBACK")
      val body = result.get.asInstanceOf[SuccessResponse].body

      result shouldBe defined
      result.get shouldBe a[SuccessResponse]
      (body \ "englishFeedback").as[JsArray].value shouldBe empty

    "return None for an unknown scenario" in:
      service.feedbackFor("NONSENSE") shouldBe None

    "return 400 ErrorResponse for INVALID_VRN" in :
      val result = service.feedbackFor("INVALID_VRN")
      result shouldBe defined
      result.get shouldBe a[ErrorResponse]
      result.get.asInstanceOf[ErrorResponse].status shouldBe 400

    "return 400 ErrorResponse for INVALID_PERIODKEY" in :
      val result = service.feedbackFor("INVALID_PERIODKEY")
      result shouldBe defined
      result.get.asInstanceOf[ErrorResponse].status shouldBe 400

    "return 400 ErrorResponse for INVALID_REQUEST" in :
      val result = service.feedbackFor("INVALID_REQUEST")
      result shouldBe defined
      result.get.asInstanceOf[ErrorResponse].status shouldBe 400

    "return 403 ErrorResponse for TAX_PERIOD_NOT_ENDED" in :
      val result = service.feedbackFor("TAX_PERIOD_NOT_ENDED")
      result shouldBe defined
      result.get.asInstanceOf[ErrorResponse].status shouldBe 403

    "return 403 ErrorResponse for INSOLVENT_TRADER" in :
      val result = service.feedbackFor("INSOLVENT_TRADER")
      result shouldBe defined
      result.get.asInstanceOf[ErrorResponse].status shouldBe 403