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

import uk.gov.hmrc.mtdtransactionriskingstub.models.StubObligation
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.LocalDate

class ObligationStubServiceSpec extends AnyWordSpec, Matchers:

  "lookupByPeriodKey" should:

    "return Right(obligation) when the period key is known and the end date is in the past" in:
      val dayAfterEnd = LocalDate.parse("2026-04-01")
      ObligationStubService.lookupByPeriodKey("AB12", dayAfterEnd) shouldBe
        Right(StubObligation("AB12", "2026-01-01", "2026-03-31", "2026-05-07"))

    "return Left(TAX_PERIOD_NOT_ENDED) when the period key is known and today equals the end date" in:
      val onEndDate = LocalDate.parse("2026-03-31")
      val result    = ObligationStubService.lookupByPeriodKey("AB12", onEndDate)
      result.isLeft shouldBe true
      result.swap.getOrElse(fail("Expected Left result")).code shouldBe "TAX_PERIOD_NOT_ENDED"

    "return Left(TAX_PERIOD_NOT_ENDED) when the period key is known and the end date is in the future" in:
      val beforeEnd = LocalDate.parse("2026-03-01")
      val result    = ObligationStubService.lookupByPeriodKey("AB12", beforeEnd)
      result.isLeft shouldBe true
      result.swap.getOrElse(fail("Expected Left result")).code shouldBe "TAX_PERIOD_NOT_ENDED"

    "return Left(PERIOD_KEY_NOT_FOUND) when the period key is not in the hardcoded set" in:
      val result = ObligationStubService.lookupByPeriodKey("ZZZZ", LocalDate.now())
      result.isLeft shouldBe true
      result.swap.getOrElse(fail("Expected Left result")).code shouldBe "PERIOD_KEY_NOT_FOUND"