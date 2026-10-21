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

  "acknowledgeFor" should:

    "return a SuccessResponse for the DEFAULT scenario" in:
      service.acknowledgeFor("DEFAULT") shouldBe Some(SuccessResponse(Json.obj()))

    "return a 400 FORMAT_VRN error for INVALID_VRN" in:
      when(stubResource.loadErrorResponse(eqTo("acknowledge"), eqTo("error-vrn-invalid.json")))
        .thenReturn(errorJson("FORMAT_VRN"))
      service.acknowledgeFor("INVALID_VRN") shouldBe Some(ErrorResponse(BAD_REQUEST, errorJson("FORMAT_VRN")))

    "return a 400 FORMAT_REPORT_ID error for INVALID_REPORTID" in:
      when(stubResource.loadErrorResponse(eqTo("acknowledge"), eqTo("error-invalid-report-id.json")))
        .thenReturn(errorJson("FORMAT_REPORT_ID"))
      service.acknowledgeFor("INVALID_REPORTID") shouldBe Some(ErrorResponse(BAD_REQUEST, errorJson("FORMAT_REPORT_ID")))

    "return a 400 FORMAT_DATETIME error for INVALID_DATETIME" in:
      when(stubResource.loadErrorResponse(eqTo("acknowledge"), eqTo("error-invalid-datetime.json")))
        .thenReturn(errorJson("FORMAT_DATETIME"))
      service.acknowledgeFor("INVALID_DATETIME") shouldBe Some(ErrorResponse(BAD_REQUEST, errorJson("FORMAT_DATETIME")))

    "return a 401 INVALID_CREDENTIALS error for INVALID_CREDENTIALS" in:
      when(stubResource.loadErrorResponse(eqTo("acknowledge"), eqTo("error-invalid-credentials.json")))
        .thenReturn(errorJson("INVALID_CREDENTIALS"))
      service.acknowledgeFor("INVALID_CREDENTIALS") shouldBe Some(ErrorResponse(UNAUTHORIZED, errorJson("INVALID_CREDENTIALS")))

    "return a 403 CLIENT_OR_AGENT_NOT_AUTHORISED error for NOT_AUTHORISED" in:
      when(stubResource.loadErrorResponse(eqTo("acknowledge"), eqTo("error-client-or-agent-not-authorised.json")))
        .thenReturn(errorJson("CLIENT_OR_AGENT_NOT_AUTHORISED"))
      service.acknowledgeFor("NOT_AUTHORISED") shouldBe Some(ErrorResponse(FORBIDDEN, errorJson("CLIENT_OR_AGENT_NOT_AUTHORISED")))

    "return a 403 CORRELATION_ID error for CORRELATION_ID" in:
      when(stubResource.loadErrorResponse(eqTo("acknowledge"), eqTo("error-correlation-id.json")))
        .thenReturn(errorJson("CORRELATION_ID"))
      service.acknowledgeFor("CORRELATION_ID") shouldBe Some(ErrorResponse(FORBIDDEN, errorJson("CORRELATION_ID")))

    "return a 404 MATCHING_RESOURCE_NOT_FOUND error for NOT_FOUND" in:
      when(stubResource.loadErrorResponse(eqTo("acknowledge"), eqTo("error-matching-resource-not-found.json")))
        .thenReturn(errorJson("MATCHING_RESOURCE_NOT_FOUND"))
      service.acknowledgeFor("NOT_FOUND") shouldBe Some(ErrorResponse(NOT_FOUND, errorJson("MATCHING_RESOURCE_NOT_FOUND")))

    "return None for an unrecognised scenario" in:
      service.acknowledgeFor("SOMETHING_ELSE") shouldBe None