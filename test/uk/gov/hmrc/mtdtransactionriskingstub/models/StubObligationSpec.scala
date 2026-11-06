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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

import java.time.LocalDate

class StubObligationSpec extends AnyWordSpec, Matchers:

  "StubObligation.hasEnded" should:
    "return true only when today is after the end date" in:
      val obligation = StubObligation("18A1", "2025-04-01", "2026-03-31", "2026-05-07")

      obligation.hasEnded(LocalDate.parse("2026-03-30")) shouldBe false
      obligation.hasEnded(LocalDate.parse("2026-03-31")) shouldBe false
      obligation.hasEnded(LocalDate.parse("2026-04-01")) shouldBe true

  "StubObligation" should:
    "write all fields to JSON, including the default status" in:
      val model = StubObligation("18A1", "2025-04-01", "2026-03-31", "2026-05-07")
      val json = Json.parse(
        """{"periodKey":"18A1","start":"2025-04-01","end":"2026-03-31","due":"2026-05-07","status":"O"}"""
      )

      Json.toJson(model) shouldBe json

    "write an explicitly provided status to JSON" in:
      val model = StubObligation("18A1", "2025-04-01", "2026-03-31", "2026-05-07", "F")
      val json = Json.parse(
        """{"periodKey":"18A1","start":"2025-04-01","end":"2026-03-31","due":"2026-05-07","status":"F"}"""
      )

      Json.toJson(model) shouldBe json