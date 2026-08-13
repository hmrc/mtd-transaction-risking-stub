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

import play.api.libs.json.{Json, OWrites}
import uk.gov.hmrc.mtdtransactionriskingstub.utils.ValidationStubError

import java.time.LocalDate

object ObligationStubService:

  case class StubObligation(periodKey: String, start: String, end: String, due: String)

  object StubObligation:
    implicit val writes: OWrites[StubObligation] = Json.writes[StubObligation]

  private val obligations: Map[String, StubObligation] = Map(
    "AB12" -> StubObligation("AB12", "2026-01-01", "2026-03-31", "2026-05-07"),
    "AB13" -> StubObligation("AB13", "2026-04-01", "2026-06-30", "2026-08-07"),
    "AB14" -> StubObligation("AB14", "2026-07-01", "2026-09-30", "2026-11-07"),
    "AB15" -> StubObligation("AB15", "2025-10-01", "2025-12-31", "2026-02-07")
  )

  private val taxPeriodNotEnded: ValidationStubError =
    ValidationStubError("TAX_PERIOD_NOT_ENDED", "The tax period has not ended", None, selfWraps = false)

  /** Returns Right(obligation) if the period has ended (or is unknown), Left(error) if it hasn't ended yet. */
  def lookupByPeriodKey(periodKey: String, today: LocalDate = LocalDate.now()): Either[ValidationStubError, Option[StubObligation]] =
    obligations.get(periodKey) match
      case None => Right(None)
      case Some(obligation) =>
        obligation.hasEnded(today) match
          case Some(true)  => Right(Some(obligation))
          case Some(false) => Left(taxPeriodNotEnded)
          case None        => Right(Some(obligation))

  extension (o: StubObligation)
    private def hasEnded(today: LocalDate): Option[Boolean] =
      scala.util.Try(LocalDate.parse(o.end)).toOption.map(today.isAfter)