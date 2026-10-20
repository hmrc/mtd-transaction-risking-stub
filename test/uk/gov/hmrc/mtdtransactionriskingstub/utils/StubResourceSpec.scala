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

  private val uuidPattern          = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
  private val correlationIdPattern = "^[0-9A-F]{64}$"

  "findResource" should:

    "return the file contents when the resource exists" in:
      stubResource.findResource("resources/response/feedback/default-single-feedback.json") shouldBe defined

    "return None when the resource does not exist" in:
      stubResource.findResource("resources/response/does-not-exist.json") shouldBe None

  "loadErrorResponse" should:

    "throw when the error file cannot be found" in:
      val ex = intercept[IllegalStateException] {
        stubResource.loadErrorResponse("acknowledge", "missing-file.json")
      }
      ex.getMessage should include("resources/response/acknowledge/missing-file.json")

  "the acknowledge error files" should:

    Seq(
      "error-vrn-invalid.json"                       -> "VRN_INVALID",
      "error-invalid-report-id.json"                 -> "REPORT_ID_INVALID",
      "error-invalid-correlation-id.json"            -> "CORRELATION_ID_INVALID",
      "error-invalid-datetime.json"                  -> "PRESENTED_DATE_TIME_INVALID",
      "error-acknowledgement-validation-failed.json" -> "ACKNOWLEDGEMENT_VALIDATION_FAILED",
      "error-invalid-credentials.json"               -> "INVALID_CREDENTIALS",
      "error-client-or-agent-not-authorised.json"    -> "CLIENT_OR_AGENT_NOT_AUTHORISED",
      "error-matching-resource-not-found.json"       -> "MATCHING_RESOURCE_NOT_FOUND"
    ).foreach { (fileName, code) =>
      s"return $code from $fileName with a message" in:
        val json = stubResource.loadErrorResponse("acknowledge", fileName)
        (json \ "code").as[String] shouldBe code
        (json \ "message").as[String] should not be empty
    }

  "the feedback error files" should:

    "load and parse the period key error" in:
      (stubResource.loadErrorResponse("feedback", "error-period-key-invalid.json") \ "code").as[String] should not be empty

  "loadFeedbackResponse" should:

    Seq("default-single-feedback.json", "multiple-feedback.json").foreach { fileName =>
      s"generate a UUID reportId and a 64-character hex correlationId for $fileName" in:
        val json = stubResource.loadFeedbackResponse(fileName)
        (json \ "reportId").as[String] should fullyMatch regex uuidPattern
        (json \ "correlationId").as[String] should fullyMatch regex correlationIdPattern
    }

    "generate different IDs on each call" in:
      val first  = stubResource.loadFeedbackResponse("default-single-feedback.json")
      val second = stubResource.loadFeedbackResponse("default-single-feedback.json")

      (first \ "reportId").as[String] should not be (second \ "reportId").as[String]
      (first \ "correlationId").as[String] should not be (second \ "correlationId").as[String]

    "generate a reportId but no correlationId for the no-feedback response" in:
      val json = stubResource.loadFeedbackResponse("no-feedback.json")

      (json \ "reportId").as[String] should fullyMatch regex uuidPattern
      (json \ "correlationId").toOption shouldBe None

    "return no path for the no-feedback item" in:
      val json = stubResource.loadFeedbackResponse("no-feedback.json")

      (json \ "englishFeedback" \ 0 \ "path").toOption shouldBe None
      (json \ "welshFeedback" \ 0 \ "path").toOption shouldBe None

    "not contain doubled apostrophes in any feedback text" in:
      Seq("default-single-feedback.json", "multiple-feedback.json", "no-feedback.json").foreach { fileName =>
        withClue(s"$fileName: ") {
          stubResource.loadFeedbackResponse(fileName).toString should not include "''"
        }
      }