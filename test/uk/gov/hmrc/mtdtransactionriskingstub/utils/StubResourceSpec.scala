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

class StubResourceSpec extends AnyWordSpec, Matchers:

  private val stubResource = new StubResource

  "findResource" should:

    "return the file contents when the resource exists" in:
      stubResource.findResource("resources/response/feedback/default-single-feedback.json") shouldBe defined

    "return None when the resource does not exist" in:
      stubResource.findResource("resources/response/does-not-exist.json") shouldBe None

  "loadErrorResponse" should:

    "load and parse an acknowledge error file from the acknowledge folder" in:
      val json = stubResource.loadErrorResponse("acknowledge", "error-invalid-report-id.json")
      (json \ "code").as[String] shouldBe "FORMAT_REPORT_ID"

    "load and parse a feedback error file from the feedback folder" in:
      val json = stubResource.loadErrorResponse("feedback", "error-period-key-invalid.json")
      (json \ "code").as[String].nonEmpty shouldBe true

    "throw when the error file cannot be found" in:
      val ex = intercept[IllegalStateException] {
        stubResource.loadErrorResponse("acknowledge", "missing-file.json")
      }
      ex.getMessage should include("resources/response/acknowledge/missing-file.json")

  "loadFeedbackResponse" should:

    "load a feedback template and substitute fresh reportId and correlationId" in:
      val json = stubResource.loadFeedbackResponse("default-single-feedback.json")
      (json \ "reportId").as[String] should not be "ReportId"
      (json \ "correlationId").as[String] should not be "CorrelationId"

    "generate a different reportId on each call" in:
      val first  = (stubResource.loadFeedbackResponse("default-single-feedback.json") \ "reportId").as[String]
      val second = (stubResource.loadFeedbackResponse("default-single-feedback.json") \ "reportId").as[String]
      first should not be second