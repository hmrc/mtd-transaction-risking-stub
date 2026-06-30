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

import play.api.libs.json.JsValue
import uk.gov.hmrc.mtdtransactionriskingstub.utils.StubResource

import javax.inject.{Inject, Singleton}

sealed trait StubResponse
case class SuccessResponse(body: JsValue)              extends StubResponse
case class ErrorResponse(status: Int, body: JsValue)   extends StubResponse

@Singleton
class FeedbackStubService @Inject()(stubResource: StubResource):

  def feedbackFor(scenario: String): Option[StubResponse] =
    scenario match
      case "DEFAULT"             => Some(success("default-single-feedback.json"))
      case "MULTIPLE_FEEDBACK"   => Some(success("multiple-feedback.json"))
      case "NO_FEEDBACK"         => Some(success("no-feedback.json"))
      case "INVALID_VRN"         => Some(error(400, "error-vrn-invalid.json"))
      case "INVALID_PERIODKEY"   => Some(error(400, "error-period-key-invalid.json"))
      case "INVALID_REQUEST"     => Some(error(400, "error-invalid-request.json"))
      case "TAX_PERIOD_NOT_ENDED" => Some(error(403, "error-tax-period.json"))
      case "INSOLVENT_TRADER"    => Some(error(403, "error-insolvent-trader.json"))
      case _                     => None

  private def success(fileName: String): StubResponse =
    SuccessResponse(stubResource.loadFeedbackResponse(fileName))

  private def error(status: Int, fileName: String): StubResponse =
    ErrorResponse(status, stubResource.loadErrorResponse(fileName))