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
import play.api.libs.json.{JsObject, JsValue, Json}

class VatValidatorServiceSpec extends AnyWordSpec, Matchers:

  private val vrn = "123456789"

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

  private def codes(errors: Seq[JsObject]): Seq[String]  = errors.map(e => (e \ "code").as[String])
  private def paths(errors: Seq[JsObject]): Seq[String]  = errors.flatMap(e => (e \ "path").asOpt[String])
  private def withField(field: String, value: JsValue): JsValue =
    validBody.as[JsObject] + (field -> value)
  private def without(field: String): JsValue =
    JsObject(validBody.as[JsObject].fields.filterNot(_._1 == field))

  "validate" should :

    "return no errors for a valid body" in :
      VatValidatorService.validate(vrn, validBody) shouldBe empty

    "return VRN_INVALID for a malformed VRN, short-circuiting other checks" in :
      val result = VatValidatorService.validate("notAVrn", validBody)
      codes(result) shouldBe Seq("VRN_INVALID")

    "return MANDATORY_FIELD_MISSING with the field path when a field is absent" in :
      val result = VatValidatorService.validate(vrn, without("vatDueSales"))
      codes(result) should contain("MANDATORY_FIELD_MISSING")
      paths(result) should contain("/vatDueSales")

    "return PERIOD_KEY_INVALID when the period key is the wrong length" in :
      val result = VatValidatorService.validate(vrn, withField("periodKey", Json.toJson("TOOLONG")))
      codes(result) should contain("PERIOD_KEY_INVALID")
      paths(result) should contain("/periodKey")

    "return INVALID_STRING_VALUE when the period key is not a string" in :
      val result = VatValidatorService.validate(vrn, withField("periodKey", Json.toJson(1234)))
      codes(result) should contain("INVALID_STRING_VALUE")

    "return INVALID_NUMERIC_VALUE when a numeric field is not a number" in :
      val result = VatValidatorService.validate(vrn, withField("vatDueSales", Json.toJson("five")))
      codes(result) should contain("INVALID_NUMERIC_VALUE")
      paths(result) should contain("/vatDueSales")

    "return INVALID_MONETARY_AMOUNT when a decimal field exceeds its range" in :
      val result = VatValidatorService.validate(vrn, withField("vatDueSales", Json.toJson(BigDecimal("10000000000000.00"))))
      codes(result) should contain("INVALID_MONETARY_AMOUNT")
      paths(result) should contain("/vatDueSales")

    "return INVALID_MONETARY_AMOUNT when a decimal field has more than 2 decimal places" in :
      val result = VatValidatorService.validate(vrn, withField("vatDueSales", Json.toJson(BigDecimal("100.123"))))
      codes(result) should contain("INVALID_MONETARY_AMOUNT")

    "return INVALID_MONETARY_AMOUNT when netVatDue is negative" in :
      val result = VatValidatorService.validate(vrn, withField("netVatDue", Json.toJson(BigDecimal("-1.00"))))
      codes(result) should contain("INVALID_MONETARY_AMOUNT")
      paths(result) should contain("/netVatDue")

    "return VAT_TOTAL_VALUE when totalVatDue does not equal sales plus acquisitions" in :
      val result = VatValidatorService.validate(vrn, withField("totalVatDue", Json.toJson(BigDecimal("201.00"))))
      codes(result) should contain("VAT_TOTAL_VALUE")
      paths(result) should contain("/totalVatDue")

    "return VAT_NET_VALUE when netVatDue is not the difference of totalVatDue and vatReclaimedCurrPeriod" in :
      val result = VatValidatorService.validate(vrn, withField("netVatDue", Json.toJson(BigDecimal("99.00"))))
      codes(result) should contain("VAT_NET_VALUE")
      paths(result) should contain("/netVatDue")

    "not run cross-field rules while field-level errors are present" in :
      // totalVatDue is wrong (would trigger VAT_TOTAL_VALUE) but a field is also missing —
      // field errors take precedence, cross-field is skipped
      val body   = without("vatDueSales").as[JsObject] + ("totalVatDue" -> Json.toJson(BigDecimal("999.00")))
      val result = VatValidatorService.validate(vrn, body)
      codes(result) should contain("MANDATORY_FIELD_MISSING")
      codes(result) should not contain "VAT_TOTAL_VALUE"

    "trip both VAT_TOTAL_VALUE and VAT_NET_VALUE when totalVatDue is changed in isolation" in :
      // totalVatDue = 201 breaks the total rule (201 ≠ 100 + 100) AND the net rule
      // (netVatDue is 100, but |201 - 100| = 101), because the two cross-field rules
      // both depend on totalVatDue
      val result = VatValidatorService.validate(vrn, withField("totalVatDue", Json.toJson(BigDecimal("201.00"))))
      codes(result) should contain allOf("VAT_TOTAL_VALUE", "VAT_NET_VALUE")

    "trip only VAT_TOTAL_VALUE when netVatDue is adjusted to keep the net rule satisfied" in :
      // Change totalVatDue to 201 AND set netVatDue to |201 - 100| = 101 so the net rule
      // passes — isolating the total rule as the single failure
      val body = validBody.as[JsObject] +
        ("totalVatDue" -> Json.toJson(BigDecimal("201.00"))) +
        ("netVatDue" -> Json.toJson(BigDecimal("101.00")))
      val result = VatValidatorService.validate(vrn, body)
      codes(result) shouldBe Seq("VAT_TOTAL_VALUE")

    "return multiple field-level errors together" in :
      val body   = withField("vatDueSales", Json.toJson("five")).as[JsObject] + ("periodKey" -> Json.toJson("TOOLONG"))
      val result = VatValidatorService.validate(vrn, body)
      codes(result) should contain allOf("INVALID_NUMERIC_VALUE", "PERIOD_KEY_INVALID")