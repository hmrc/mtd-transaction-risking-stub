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

import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{BAD_REQUEST, FORBIDDEN, SERVICE_UNAVAILABLE}
import uk.gov.hmrc.mtdtransactionriskingstub.utils.StubPeriodKeys

class ObligationStubServiceSpec extends AnyWordSpec, Matchers, EitherValues:

  "lookupByPeriodKey" should:

    "return 400 PERIOD_KEY_INVALID when no open obligation matches" in:
      val result = ObligationStubService.lookupByPeriodKey(StubPeriodKeys.periodKeyNotFound).left.value

      result.status shouldBe BAD_REQUEST
      result.error.code shouldBe "PERIOD_KEY_INVALID"
      result.error.message shouldBe "The supplied period key does not match an open obligation"

    "return 403 TAX_PERIOD_NOT_ENDED when the period has not ended" in:
      val result = ObligationStubService.lookupByPeriodKey(StubPeriodKeys.taxPeriodNotEnded).left.value

      result.status shouldBe FORBIDDEN
      result.error.code shouldBe "TAX_PERIOD_NOT_ENDED"

    "return 403 RULE_INSOLVENT_TRADER for an insolvent trader" in:
      val result = ObligationStubService.lookupByPeriodKey(StubPeriodKeys.insolventTrader).left.value

      result.status shouldBe FORBIDDEN
      result.error.code shouldBe "RULE_INSOLVENT_TRADER"

    "return 503 SERVICE_UNAVAILABLE when the obligations lookup fails" in:
      val result = ObligationStubService.lookupByPeriodKey(StubPeriodKeys.obligationsUnavailable).left.value

      result.status shouldBe SERVICE_UNAVAILABLE
      result.error.code shouldBe "SERVICE_UNAVAILABLE"

    "return an ended obligation for any other key" in:
      val obligation = ObligationStubService.lookupByPeriodKey("AB12").value

      obligation.periodKey shouldBe "AB12"
      obligation.end shouldBe "2026-03-31"

    "echo the requested period key back on the obligation" in:
      ObligationStubService.lookupByPeriodKey("XY45").value.periodKey shouldBe "XY45"

    "return an ended obligation for the RDS and RSD scenario keys, so requests reach them" in:
      val scenarioKeys = Seq(
        StubPeriodKeys.rdsBadRequest,
        StubPeriodKeys.rdsNotFound,
        StubPeriodKeys.rdsServiceUnavailable,
        StubPeriodKeys.rdsNoFeedback,
        StubPeriodKeys.rdsMalformedReport,
        StubPeriodKeys.rsdBadRequest,
        StubPeriodKeys.rsdServerError,
        StubPeriodKeys.rsdServiceUnavailable,
        StubPeriodKeys.rsdTooManyRequests
      )

      scenarioKeys.foreach { key =>
        withClue(s"for period key $key: ")(ObligationStubService.lookupByPeriodKey(key).value.periodKey shouldBe key)
      }