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

class InsightsModelsSpec extends AnyWordSpec, Matchers:

  "InsightsStubRequest" should:
    "round-trip through JSON" in:
      val model = InsightsStubRequest("123456789")
      val json  = Json.parse("""{"vatRegistrationNumber": "123456789"}""")

      Json.toJson(model) shouldBe json
      json.as[InsightsStubRequest] shouldBe model

  "RiskData" should:
    "round-trip through JSON" in:
      val model = RiskData(hops = 1, avgHops = 2.51)
      val json  = Json.parse("""{"hops": 1, "avgHops": 2.51}""")

      Json.toJson(model) shouldBe json
      json.as[RiskData] shouldBe model

  "StrategicRisk" should:
    "round-trip through JSON" in:
      val model = StrategicRisk(
        riskCorrelationId = "abc-123",
        riskScore         = 83.33,
        reasons           = List("reason one"),
        riskData          = List(RiskData(1, 2.51))
      )
      val json = Json.parse(
        """
          |{
          |  "riskCorrelationId": "abc-123",
          |  "riskScore": 83.33,
          |  "reasons": ["reason one"],
          |  "riskData": [{"hops": 1, "avgHops": 2.51}]
          |}
        """.stripMargin
      )

      Json.toJson(model) shouldBe json
      json.as[StrategicRisk] shouldBe model

  "Insights" should:
    "round-trip through JSON" in:
      val model = Insights(StrategicRisk("abc-123", 83.33, List("reason"), List(RiskData(1, 2.51))))
      Json.toJson(model).as[Insights] shouldBe model

  "InsightsStubResponse" should:
    "round-trip through JSON" in:
      val model = InsightsStubResponse(
        attributeType  = "VAT_REGISTRATION_NUMBER",
        attributeValue = "123456789",
        insights       = Insights(StrategicRisk("abc-123", 83.33, List("reason"), List(RiskData(1, 2.51))))
      )
      val json = Json.parse(
        """
          |{
          |  "attributeType": "VAT_REGISTRATION_NUMBER",
          |  "attributeValue": "123456789",
          |  "insights": {
          |    "strategicRisk": {
          |      "riskCorrelationId": "abc-123",
          |      "riskScore": 83.33,
          |      "reasons": ["reason"],
          |      "riskData": [{"hops": 1, "avgHops": 2.51}]
          |    }
          |  }
          |}
        """.stripMargin
      )

      Json.toJson(model) shouldBe json
      json.as[InsightsStubResponse] shouldBe model