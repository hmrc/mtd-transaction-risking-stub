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

import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, NOT_FOUND, UNAUTHORIZED}
import play.api.libs.json.Json
import uk.gov.hmrc.mtdtransactionriskingstub.utils.StubResource

import javax.inject.{Inject, Singleton}



@Singleton
class AcknowledgeStubService @Inject()(stubResource: StubResource):

  def acknowledgeFor(scenario: String): Option[StubResponse] =
    scenario match
      case "DEFAULT"             => Some(success)
      case "INVALID_VRN"         => Some(error(BAD_REQUEST,   "error-vrn-invalid.json"))
      case "INVALID_REPORTID"    => Some(error(BAD_REQUEST,   "error-invalid-report-id.json"))
      case "INVALID_DATETIME"    => Some(error(BAD_REQUEST,   "error-invalid-datetime.json"))
      case "INVALID_CREDENTIALS" => Some(error(UNAUTHORIZED,  "error-invalid-credentials.json"))
      case "NOT_AUTHORISED"      => Some(error(FORBIDDEN,     "error-client-or-agent-not-authorised.json"))
      case "CORRELATION_ID"      => Some(error(FORBIDDEN,     "error-correlation-id.json"))
      case "NOT_FOUND"           => Some(error(NOT_FOUND,     "error-matching-resource-not-found.json"))
      case _                     => None
  
  
  private def success: StubResponse =
    SuccessResponse(Json.obj())

  private def error(status: Int, fileName: String): StubResponse =
    ErrorResponse(status, stubResource.loadErrorResponse("acknowledge", fileName))