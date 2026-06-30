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

package uk.gov.hmrc.mtdtransactionriskingstub.utils

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.JsArray

class StubResourceSpec extends AnyWordSpec, Matchers:

  private val stubResource = new StubResource

  "loadFeedbackResponse" should:

    "load the default single feedback file and replace placeholders with valid UUIDs" in:
      val result = stubResource.loadFeedbackResponse("default-single-feedback.json")

      val uuidRegex = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"

      (result \ "reportId").as[String]      should fullyMatch regex uuidRegex
      (result \ "correlationId").as[String] should fullyMatch regex uuidRegex
      (result \ "englishFeedback").as[JsArray].value should not be empty
      (result \ "welshFeedback").as[JsArray].value should not be empty

    "generate different report and correlation IDs on each call" in:
      val first  = stubResource.loadFeedbackResponse("default-single-feedback.json")
      val second = stubResource.loadFeedbackResponse("default-single-feedback.json")

      (first \ "reportId").as[String] should not be (second \ "reportId").as[String]

    "load the no-feedback file with empty arrays" in:
      val result = stubResource.loadFeedbackResponse("no-feedback.json")

      (result \ "englishFeedback").as[List[Int]] shouldBe empty
      (result \ "welshFeedback").as[List[Int]]   shouldBe empty

    "throw when the template file does not exist" in:
      assertThrows[IllegalStateException] {
        stubResource.loadFeedbackResponse("does-not-exist.json")
      }

  "findResource" should:

    "return None for a missing file" in:
      stubResource.findResource("resources/response/feedback/missing.json") shouldBe None

    "return Some content for an existing file" in:
      stubResource.findResource("resources/response/feedback/default-single-feedback.json") shouldBe defined