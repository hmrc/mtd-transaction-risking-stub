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

package uk.gov.hmrc.mtdtransactionriskingstub.utils

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc.Headers

class GovTestScenarioSpec extends AnyWordSpec, Matchers:

  "effectiveScenario" should:

    "return DEFAULT when no Gov-Test-Scenario header is present" in:
      GovTestScenario.effectiveScenario(Headers()) shouldBe "DEFAULT"

    "return DEFAULT when the header value is '-'" in:
      GovTestScenario.effectiveScenario(Headers("Gov-Test-Scenario" -> "-")) shouldBe "DEFAULT"

    "return DEFAULT when the header value is explicitly DEFAULT" in:
      GovTestScenario.effectiveScenario(Headers("Gov-Test-Scenario" -> "DEFAULT")) shouldBe "DEFAULT"

    "return the scenario value when a named scenario is present" in:
      GovTestScenario.effectiveScenario(Headers("Gov-Test-Scenario" -> "MULTIPLE_FEEDBACK")) shouldBe "MULTIPLE_FEEDBACK"

  "hasScenario" should:

    "return false when no header is present" in:
      GovTestScenario.hasScenario(Headers()) shouldBe false

    "return false when the header value is '-'" in:
      GovTestScenario.hasScenario(Headers("Gov-Test-Scenario" -> "-")) shouldBe false

    "return false when the header value is empty" in:
      GovTestScenario.hasScenario(Headers("Gov-Test-Scenario" -> "")) shouldBe false

    "return true when the header value is DEFAULT" in:
      GovTestScenario.hasScenario(Headers("Gov-Test-Scenario" -> "DEFAULT")) shouldBe true

    "return true when a named scenario is present" in:
      GovTestScenario.hasScenario(Headers("Gov-Test-Scenario" -> "MULTIPLE_FEEDBACK")) shouldBe true