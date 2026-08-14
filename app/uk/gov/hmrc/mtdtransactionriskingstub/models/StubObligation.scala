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

package uk.gov.hmrc.mtdtransactionriskingstub.models

import play.api.libs.json.{Json, OWrites}

import java.time.LocalDate

case class StubObligation(periodKey: String, start: String, end: String, due: String, status: String = "O"):
  def hasEnded(today: LocalDate): Boolean =
    today.isAfter(LocalDate.parse(end))

object StubObligation:
  implicit val writes: OWrites[StubObligation] = Json.writes[StubObligation]
