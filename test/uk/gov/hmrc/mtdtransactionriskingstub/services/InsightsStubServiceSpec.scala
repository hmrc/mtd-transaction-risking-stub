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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class InsightsStubServiceSpec extends AnyWordSpec, Matchers:

  private val service = new InsightsStubService

  "insightsResponse" should:

    "echo the provided VRN in attributeValue" in:
      val result = service.insightsResponse("123456789")
      result.attributeValue shouldBe "123456789"

    "set the attributeType to VAT_REGISTRATION_NUMBER" in:
      service.insightsResponse("123456789").attributeType shouldBe "VAT_REGISTRATION_NUMBER"

    "include the VRN in the risk reason text" in:
      val result = service.insightsResponse("123456789")
      result.insights.strategicRisk.reasons.head should include("123456789")

    "return a populated riskData list" in:
      service.insightsResponse("123456789").insights.strategicRisk.riskData should not be empty