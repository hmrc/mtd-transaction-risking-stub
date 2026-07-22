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

import play.api.libs.json.JsValue
import uk.gov.hmrc.mtdtransactionriskingstub.utils.ValidationStubError

object VatValidatorService:

  private val vrnRegex       = "^[0-9]{9}$"
  private val periodKeyRegex = "^[a-zA-Z0-9#]{4}$"

  private val decimalFields    = Seq("vatDueSales", "vatDueAcquisitions", "totalVatDue", "vatReclaimedCurrPeriod")
  private val nonDecimalFields = Seq("totalValueSalesExVAT", "totalValuePurchasesExVAT", "totalValueGoodsSuppliedExVAT", "totalAcquisitionsExVAT")
  private val mandatoryFields  = "periodKey" +: (decimalFields ++ nonDecimalFields) :+ "netVatDue"

  private val decimalMin = BigDecimal("-9999999999999.99")
  private val decimalMax = BigDecimal("9999999999999.99")
  private val wholeMin   = BigDecimal("-9999999999999")
  private val wholeMax   = BigDecimal("9999999999999")
  private val netVatMax  = BigDecimal("99999999999.99")

  private val decimalRangeMessage = "amount should be a monetary value (to 2 decimal places), between -9,999,999,999,999.99 and 9,999,999,999,999.99"
  private val wholeRangeMessage = "The value must be between -9999999999999 and 9999999999999"
  private val netVatRangeMessage = "amount should be a monetary value (to 2 decimal places), between 0 and 99,999,999,999.99"

  def validate(vrn: String, body: JsValue): Seq[ValidationStubError] =
    if !vrn.matches(vrnRegex) then Seq(ValidationStubError("VRN_INVALID", "The provided VRN is invalid", None, selfWraps = false))
    else
      val fieldErrors = missingFieldErrors(body) ++ periodKeyErrors(body) ++ numericTypeErrors(body) ++ rangeErrors(body)
      if fieldErrors.nonEmpty then fieldErrors else crossFieldErrors(body)

  private def missingFieldErrors(body: JsValue): Seq[ValidationStubError] =
    mandatoryFields
      .filter(field => (body \ field).isEmpty)
      .map(field => ValidationStubError("MANDATORY_FIELD_MISSING", "a mandatory field is missing", Some(s"/$field"), selfWraps = false))

  private def periodKeyErrors(body: JsValue): Seq[ValidationStubError] =
    (body \ "periodKey").asOpt[String] match
      case Some(periodKey) if !periodKey.matches(periodKeyRegex) =>
        Seq(ValidationStubError("PERIOD_KEY_INVALID", "period key should be a 4 character string", Some("/periodKey"), selfWraps = true))
      case None if (body \ "periodKey").isDefined =>
        Seq(ValidationStubError("INVALID_STRING_VALUE", "please provide a string field", Some("/periodKey"), selfWraps = true))
      case _ => Seq.empty

  private def numericTypeErrors(body: JsValue): Seq[ValidationStubError] =
    (decimalFields ++ nonDecimalFields :+ "netVatDue")
      .filter(field => (body \ field).isDefined && numeric(body, field).isEmpty)
      .map(field => ValidationStubError("INVALID_NUMERIC_VALUE", "please provide a numeric field", Some(s"/$field"), selfWraps = false))

  private def numeric(body: JsValue, field: String): Option[BigDecimal] =
    (body \ field).asOpt[BigDecimal]

  private def rangeErrors(body: JsValue): Seq[ValidationStubError] =
    def outOfRange(field: String, min: BigDecimal, max: BigDecimal, maxScale: Int, message: String): Option[ValidationStubError] =
      numeric(body, field)
        .filter(value => value < min || value > max || value.scale > maxScale)
        .map(_ => ValidationStubError("INVALID_MONETARY_AMOUNT", message, Some(s"/$field"), selfWraps = false))

    decimalFields.flatMap(outOfRange(_, decimalMin, decimalMax, maxScale = 2, decimalRangeMessage)) ++
      nonDecimalFields.flatMap(outOfRange(_, wholeMin, wholeMax, maxScale = 0, wholeRangeMessage)) ++
      outOfRange("netVatDue", min = 0, netVatMax, maxScale = 2, netVatRangeMessage)

  private def crossFieldErrors(body: JsValue): Seq[ValidationStubError] =
    val result = for
      sales     <- numeric(body, "vatDueSales")
      acq       <- numeric(body, "vatDueAcquisitions")
      total     <- numeric(body, "totalVatDue")
      reclaimed <- numeric(body, "vatReclaimedCurrPeriod")
      net       <- numeric(body, "netVatDue")
    yield
      val totalError = Option.when(total != sales + acq):
        ValidationStubError("VAT_TOTAL_VALUE", "totalVatDue should be equal to vatDueSales + vatDueAcquisitions", Some("/totalVatDue"), selfWraps = true)
      val netError = Option.when(net != (total - reclaimed).abs):
        ValidationStubError("VAT_NET_VALUE", "netVatDue should be the difference between the largest and the smallest values among totalVatDue and vatReclaimedCurrPeriod", Some("/netVatDue"), selfWraps = true)
      totalError.toSeq ++ netError.toSeq

    result.getOrElse(Seq.empty)