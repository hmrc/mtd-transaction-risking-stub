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

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.mtdtransactionriskingstub.utils.StubResource

import javax.inject.{Inject, Singleton}



@Singleton
class AcknowledgeStubService @Inject()(stubResource: StubResource):

  def AcknowledgeFor(scenario: String): Option[StubResponse] =
    scenario match
      case "DEFAULT"             => Some(success)
      case "INVALID_VRN"         => Some(error(400, "error-invalid-vrn-format.json"))
      case "FORMAT_RECEIPT_ID"   => Some(error(400, "error-invalid-receipt-id-format.json"))
      case "FORMAT_DATETIME"     => Some(error(400, "error-invalid-datetime-format.json"))
      case "INVALID_CREDENTIALS" => Some(error(401, "error-invalid-credentials.json"))
      case "CLIENT_OR_AGENT_NOT_AUTHORISED"    => Some(error(403, "error-client-or-agent-not-authorised.json"))
      case "CORRELATION_ID"    => Some(error(403, "error-correlation-id.json"))
      case "MATCHING_RESOURCE_NOT_FOUND" => Some(error(404, "error-matching-resource-not-found.json"))

      case _                     => None
  
  
  private def success: StubResponse =
    SuccessResponse(Json.obj())

  private def error(status: Int, fileName: String): StubResponse =
    ErrorResponse(status, stubResource.loadErrorResponse(fileName))