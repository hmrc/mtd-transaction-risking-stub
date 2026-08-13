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

import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{Json, OWrites}
import uk.gov.hmrc.mtdtransactionriskingstub.utils.ValidationStubError

import java.time.LocalDate

object ObligationStubService:

  case class StubObligation(periodKey: String, start: String, end: String, due: String):
    def hasEnded(today: LocalDate): Boolean =
      today.isAfter(LocalDate.parse(end))

  object StubObligation:
    implicit val writes: OWrites[StubObligation] = Json.writes[StubObligation]

  private val obligations: Map[String, StubObligation] = Map(
    "AB12" -> StubObligation("AB12", "2026-01-01", "2026-03-31", "2036-05-07"),
    "AB13" -> StubObligation("AB13", "2026-04-01", "2026-06-30", "2036-08-07"),
    "AB14" -> StubObligation("AB14", "2026-07-01", "2026-09-30", "2036-11-07"),
    "AB15" -> StubObligation("AB15", "2025-10-01", "2025-12-31", "2036-02-07")
  )

  private val taxPeriodNotEnded =
    ValidationStubError("TAX_PERIOD_NOT_ENDED", "Tax period not ended", None, selfWraps = false)

  private val periodKeyNotFound =
    ValidationStubError("PERIOD_KEY_NOT_FOUND", "Period key not found", None, selfWraps = false)
    
    
  /** Returns Right(obligation) if the period has ended (or end date is unknown), Left(error) otherwise. */
  def lookupByPeriodKey(periodKey: String, today: LocalDate = LocalDate.now()): Either[ValidationStubError, Option[StubObligation]] =
    obligations.get(periodKey) match

      case None => Left(periodKeyNotFound)

      case Some(obligation) =>

        if obligation.hasEnded(today) then Right(Some(obligation))
        else Left(taxPeriodNotEnded)
