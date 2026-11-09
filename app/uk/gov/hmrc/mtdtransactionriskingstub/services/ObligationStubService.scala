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

import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, SERVICE_UNAVAILABLE}
import uk.gov.hmrc.mtdtransactionriskingstub.models.StubObligation
import uk.gov.hmrc.mtdtransactionriskingstub.utils.{StubPeriodKeys, ValidationStubError}

final case class ObligationLookupError(status: Int, error: ValidationStubError)

// Resolves a period key to an open obligation, or to the error vat-api returns for it
object ObligationStubService:

  def lookupByPeriodKey(periodKey: String): Either[ObligationLookupError, StubObligation] =
    periodKey match
      case StubPeriodKeys.periodKeyNotFound      => Left(ObligationLookupError(BAD_REQUEST, noOpenObligationError))
      case StubPeriodKeys.taxPeriodNotEnded      => Left(ObligationLookupError(FORBIDDEN, taxPeriodNotEndedError))
      case StubPeriodKeys.insolventTrader        => Left(ObligationLookupError(FORBIDDEN, insolventTraderError))
      case StubPeriodKeys.obligationsUnavailable => Left(ObligationLookupError(SERVICE_UNAVAILABLE, serviceUnavailableError))
      case _                                     => Right(endedObligation(periodKey))

  private def endedObligation(periodKey: String): StubObligation =
    StubObligation(periodKey, start = "2026-01-01", end = "2026-03-31", due = "2026-05-07")

  private val noOpenObligationError =
    ValidationStubError("PERIOD_KEY_INVALID", "The supplied period key does not match an open obligation", None, selfWraps = false)

  private val taxPeriodNotEndedError =
    ValidationStubError(
      "TAX_PERIOD_NOT_ENDED",
      "The remote endpoint has indicated that the submission is for a tax period that has not ended",
      None,
      selfWraps = false
    )

  private val insolventTraderError =
    ValidationStubError("RULE_INSOLVENT_TRADER", "The remote endpoint has indicated that the Trader is insolvent", None, selfWraps = false)

  // vat-api's own 503 body, which TxR passes on unchanged
  private val serviceUnavailableError =
    ValidationStubError("SERVICE_UNAVAILABLE", "Internal server error", None, selfWraps = false)