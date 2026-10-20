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
import play.api.http.Status.*
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.mtdtransactionriskingstub.utils.StubResource

class AcknowledgeStubServiceSpec extends AnyWordSpec, Matchers, MockitoSugar:

  private val stubResource = mock[StubResource]
  private val service      = new AcknowledgeStubService(stubResource)

  private def errorJson(code: String): JsValue =
    Json.obj("code" -> code, "message" -> s"$code message")

  // Each scenario loads the named file and returns it with the given status
  private def expectError(scenario: String, status: Int, fileName: String, code: String): Unit =
    s"return a $status $code error for $scenario" in:
      when(stubResource.loadErrorResponse(eqTo("acknowledge"), eqTo(fileName))).thenReturn(errorJson(code))
      service.acknowledgeFor(scenario) shouldBe Some(ErrorResponse(status, errorJson(code)))

  "acknowledgeFor" should:

    "return a SuccessResponse for the DEFAULT scenario" in:
      service.acknowledgeFor("DEFAULT") shouldBe Some(SuccessResponse(Json.obj()))

    expectError("INVALID_VRN", BAD_REQUEST, "error-vrn-invalid.json", "VRN_INVALID")
    expectError("INVALID_REPORTID", BAD_REQUEST, "error-invalid-report-id.json", "REPORT_ID_INVALID")
    expectError("INVALID_CORRELATIONID", BAD_REQUEST, "error-invalid-correlation-id.json", "CORRELATION_ID_INVALID")
    expectError("INVALID_DATETIME", BAD_REQUEST, "error-invalid-datetime.json", "PRESENTED_DATE_TIME_INVALID")

    // The VRN, report ID and correlation ID don't match a report
    expectError("CORRELATION_ID", BAD_REQUEST, "error-acknowledgement-validation-failed.json", "ACKNOWLEDGEMENT_VALIDATION_FAILED")

    expectError("INVALID_CREDENTIALS", UNAUTHORIZED, "error-invalid-credentials.json", "INVALID_CREDENTIALS")
    expectError("NOT_AUTHORISED", FORBIDDEN, "error-client-or-agent-not-authorised.json", "CLIENT_OR_AGENT_NOT_AUTHORISED")
    expectError("NOT_FOUND", NOT_FOUND, "error-matching-resource-not-found.json", "MATCHING_RESOURCE_NOT_FOUND")

    "return None for an unrecognised scenario" in:
      service.acknowledgeFor("SOMETHING_ELSE") shouldBe None