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

import uk.gov.hmrc.mtdtransactionriskingstub.models.*

import javax.inject.Singleton

@Singleton
class InsightsStubService:

  def insightsResponse(vrn: String): InsightsStubResponse =
    InsightsStubResponse(
      attributeType  = "VAT_REGISTRATION_NUMBER",
      attributeValue = vrn,
      insights = Insights(
        strategicRisk = StrategicRisk(
          riskCorrelationId = "180a7587-3b80-40e6-b907-ea641dccbe11",
          riskScore         = 83.33,
          reasons           = List(
            s"VRN '$vrn' is 1 hops from something risky. The average VRN is 2.51 hops from something risky."
          ),
          riskData = List(RiskData(hops = 1, avgHops = 2.51))
        )
      )
    )
