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

import play.api.libs.json.{JsValue, Json, JsObject}

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

  def validate(vrn: String, body: JsValue): Seq[JsObject] =
    if !vrn.matches(vrnRegex) then Seq(err("VRN_INVALID", "The provided Vrn is invalid"))
    else
      val fieldErrors = missingFieldErrors(body) ++ periodKeyErrors(body) ++ numericTypeErrors(body) ++ rangeErrors(body)
      if fieldErrors.nonEmpty then fieldErrors else crossFieldErrors(body)

  private def err(code: String, message: String, path: Option[String] = None): JsObject =
    Json.obj("code" -> code, "message" -> message) ++ path.fold(Json.obj())(p => Json.obj("path" -> p))

  private def numeric(body: JsValue, field: String): Option[BigDecimal] =
    (body \ field).asOpt[BigDecimal]

  private def missingFieldErrors(body: JsValue): Seq[JsObject] =
    mandatoryFields
      .filter(field => (body \ field).isEmpty)
      .map(field => err("MANDATORY_FIELD_MISSING", "a mandatory field is missing", Some(s"/$field")))

  private def periodKeyErrors(body: JsValue): Seq[JsObject] =
    (body \ "periodKey").asOpt[String] match
      case Some(periodKey) if !periodKey.matches(periodKeyRegex) =>
        Seq(err("PERIOD_KEY_INVALID", "period key should be a 4 character string", Some("/periodKey")))
      case None if (body \ "periodKey").isDefined =>
        Seq(err("INVALID_STRING_VALUE", "periodKey should be a string", Some("/periodKey")))
      case _ => Seq.empty

  private def numericTypeErrors(body: JsValue): Seq[JsObject] =
    (decimalFields ++ nonDecimalFields :+ "netVatDue")
      .filter(field => (body \ field).isDefined && numeric(body, field).isEmpty)
      .map(field => err("INVALID_NUMERIC_VALUE", s"$field should be a valid number", Some(s"/$field")))

  private def rangeErrors(body: JsValue): Seq[JsObject] =
    def outOfRange(field: String, min: BigDecimal, max: BigDecimal, maxScale: Int, message: String): Option[JsObject] =
      numeric(body, field)
        .filter(value => value < min || value > max || value.scale > maxScale)
        .map(_ => err("INVALID_MONETARY_AMOUNT", message, Some(s"/$field")))

    val decimalMessage = "amount should be a monetary value (to 2 decimal places), between -9999999999999.99 and 9999999999999.99"
    val wholeMessage   = "The value must be between -9999999999999 and 9999999999999"
    val netVatMessage  = "amount should be a non-negative monetary value (to 2 decimal places), between 0.00 and 99999999999.99"

    decimalFields.flatMap(outOfRange(_, decimalMin, decimalMax, maxScale = 2, decimalMessage)) ++
      nonDecimalFields.flatMap(outOfRange(_, wholeMin, wholeMax, maxScale = 0, wholeMessage)) ++
      outOfRange("netVatDue", min = 0, netVatMax, maxScale = 2, netVatMessage)

  private def crossFieldErrors(body: JsValue): Seq[JsObject] =
    val result = for
      sales     <- numeric(body, "vatDueSales")
      acq       <- numeric(body, "vatDueAcquisitions")
      total     <- numeric(body, "totalVatDue")
      reclaimed <- numeric(body, "vatReclaimedCurrPeriod")
      net       <- numeric(body, "netVatDue")
    yield
      val totalError = Option.when(total != sales + acq):
        err("VAT_TOTAL_VALUE", "totalVatDue should be equal to vatDueSales + vatDueAcquisitions", Some("/totalVatDue"))
      val netError = Option.when(net != (total - reclaimed).abs):
        err("VAT_NET_VALUE", "netVatDue should be the difference between the largest and the smallest values among totalVatDue and vatReclaimedCurrPeriod", Some("/netVatDue"))
      totalError.toSeq ++ netError.toSeq

    result.getOrElse(Seq.empty)