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

package uk.gov.hmrc.mtdtransactionriskingstub.controllers

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers.*
import uk.gov.hmrc.mtdtransactionriskingstub.services.InsightsStubService

class InsightsStubControllerSpec extends AnyWordSpec, Matchers:

  private given system: ActorSystem  = ActorSystem("test")
  private given materializer:    Materializer = Materializer(system)

  private val controller =
    new InsightsStubController(Helpers.stubControllerComponents(), new InsightsStubService)

  "checkInsights" should:

    "return 200 with the insights response for a valid request body" in :
      val request = FakeRequest("POST", "/check/insights")
        .withBody(AnyContentAsJson(Json.obj("vatRegistrationNumber" -> "123456789")))
      val result = controller.checkInsights()(request)

      status(result) shouldBe OK
      (contentAsJson(result) \ "attributeValue").as[String] shouldBe "123456789"
      (contentAsJson(result) \ "insights" \ "strategicRisk" \ "riskScore").as[Double] shouldBe 83.33

    "return 400 for an invalid request body" in :
      val request = FakeRequest("POST", "/check/insights")
        .withBody(AnyContentAsJson(Json.obj("wrong" -> "field")))
      val result = controller.checkInsights()(request)

      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] shouldBe "INVALID_REQUEST"