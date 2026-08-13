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
import play.api.http.Status.*
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsJson, defaultAwaitTimeout, header, status, stubControllerComponents}

class ValidateStubControllerSpec extends AnyWordSpec, Matchers:

  private given system: ActorSystem  = ActorSystem("test")
  private given mat:    Materializer = Materializer(system)

  private val controller = new ValidateStubController(stubControllerComponents())
  private val vrn        = "123456789"

  private val validBody: JsValue = Json.parse(
    """
      |{
      |  "periodKey": "AB12",
      |  "vatDueSales": 100.00,
      |  "vatDueAcquisitions": 100.00,
      |  "totalVatDue": 200.00,
      |  "vatReclaimedCurrPeriod": 100.00,
      |  "netVatDue": 100.00,
      |  "totalValueSalesExVAT": 500,
      |  "totalValuePurchasesExVAT": 500,
      |  "totalValueGoodsSuppliedExVAT": 500,
      |  "totalAcquisitionsExVAT": 500
      |}
      |""".stripMargin
  )

  private def requestWith(body: JsValue, correlationId: Option[String] = None) =
    val base = FakeRequest("POST", s"/internal/validate/$vrn").withBody(body)
    correlationId.fold(base)(id => base.withHeaders("X-CorrelationId" -> id))

  "validateReturn" should :

    "return 200 with obligation JSON and a correlation header for a valid body with a known ended period" in :
      val result = controller.validateReturn(vrn)(requestWith(validBody, Some("test-id")))
      status(result) shouldBe OK
      (contentAsJson(result) \ "periodKey").as[String] shouldBe "AB12"
      header("X-CorrelationId", result) shouldBe Some("test-id")

    "generate a correlation id when the header is absent" in :
      val result = controller.validateReturn(vrn)(requestWith(validBody))
      status(result) shouldBe OK
      header("X-CorrelationId", result) shouldBe Some("no-correlation-id")

    "return 204 for a valid body with an unknown period key" in :
      val unknownPeriodBody = validBody.as[JsObject] + ("periodKey" -> Json.toJson("ZZ99"))
      val result = controller.validateReturn(vrn)(requestWith(unknownPeriodBody, Some("test-id")))
      status(result) shouldBe NO_CONTENT

    "return 400 TAX_PERIOD_NOT_ENDED for a period that has not ended" in :
      // AB14 ends 2026-09-30, which has not yet passed
      val futureBody = validBody.as[JsObject] + ("periodKey" -> Json.toJson("AB14"))
      val result     = controller.validateReturn(vrn)(requestWith(futureBody, Some("test-id")))
      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] shouldBe "TAX_PERIOD_NOT_ENDED"
      header("X-CorrelationId", result) shouldBe Some("test-id")

    "generate a correlation id ()when the header is absent" in :
      val result = controller.validateReturn(vrn)(requestWith(validBody))
      System.out.println(s"result: ${contentAsJson(result)}")
      status(result) shouldBe OK
      header("X-CorrelationId", result) shouldBe Some("no-correlation-id")

    "return 400 with wrapper for PERIOD_KEY_INVALID error" in :
      val badBody = validBody.as[JsObject] + ("periodKey" -> Json.toJson("TOOLONG"))
      val result  = controller.validateReturn(vrn)(requestWith(badBody, Some("test-id")))

      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] shouldBe "INVALID_REQUEST"
      ((contentAsJson(result) \ "errors")(0) \ "code").as[String] shouldBe "PERIOD_KEY_INVALID"
      header("X-CorrelationId", result) shouldBe Some("test-id")

    "return 400 with bare error for numericTypeErrors error" in :
      val badBody = validBody.as[JsObject] + ("vatDueSales" -> Json.toJson("NonNumber"))
      val result = controller.validateReturn(vrn)(requestWith(badBody, Some("test-id")))

      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] shouldBe "INVALID_NUMERIC_VALUE"
      header("X-CorrelationId", result) shouldBe Some("test-id")

    "return 400 with an INVALID_REQUEST wrapper and errors array when multiple rules fail" in :
      val badBody = validBody.as[JsObject] +
        ("vatDueSales" -> Json.toJson("five")) + ("periodKey" -> Json.toJson("TOOLONG"))
      val result  = controller.validateReturn(vrn)(requestWith(badBody, Some("test-id")))

      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] shouldBe "INVALID_REQUEST"
      (contentAsJson(result) \ "errors").as[Seq[JsValue]] should not be empty

    "return 400 VRN_INVALID for a malformed VRN" in :
      val result = controller.validateReturn("notAVrn")(requestWith(validBody))
      status(result) shouldBe BAD_REQUEST
      (contentAsJson(result) \ "code").as[String] shouldBe "VRN_INVALID"