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

import play.api.http.Status.UNAUTHORIZED
import uk.gov.hmrc.mtdtransactionriskingstub.utils.StubResource

import javax.inject.{Inject, Singleton}

@Singleton
class RdsAcknowledgeStubService @Inject()(stubResource: StubResource):

  def acknowledgeResponse(scenario: String, vrn: String, feedbackId: String): Option[StubResponse] =
    scenario match
      case "DEFAULT"            => Some(success("default-acknowledge.json", vrn, feedbackId))
      case "VALIDATION_FAILED"  => Some(error(UNAUTHORIZED, "error-validation-failed.json"))
      case _                    => None

  private def success(fileName: String, vrn: String, feedbackId: String): StubResponse =
    SuccessResponse(stubResource.loadRdsAcknowledgeResponse(fileName, vrn, feedbackId))

  private def error(status: Int, fileName: String): StubResponse =
    ErrorResponse(status, stubResource.loadErrorResponse("rds/acknowledge", fileName))