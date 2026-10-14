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

import play.api.libs.json.*

import java.time.temporal.ChronoUnit
import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID

object RdsReportService:

  val rdsCorrelationId = "E9F65715BBC9222477B27074804BBDD5C73CDE62F84D8B00CFD05B883534AF3D"

  private val reportModule = "HMRC_ASSIST_VAT_FINSUB_FEEDBACK"
  private val acknowledgeModule = "HMRC_ASSIST_VAT_FINSUB_FEEDBACK_ACK"

  private val columns = Seq("ITEMNUMBER", "MESSAGE", "ACTION", "TITLE", "LINKTITLE", "LINKURL", "PATH")
  private val englishFeedback = ActionRow(
    "1",
    "The VAT due on sales figure appears lower than expected based on your total sales value.",
    "Please review box 1 and box 6 of your VAT return.",
    "HMRC feedback: VAT due on sales appears low",
    "VAT Notice 700",
    "https://www.gov.uk/guidance/vat-notice-700-the-vat-guide",
    "vatDueSales"
  )
  private val welshFeedback = ActionRow(
    "1",
    "Mae'r ffigur TAW sy'n ddyledus ar werthiannau yn ymddangos yn is na'r disgwyl yn seiliedig ar gyfanswm eich gwerthiannau.",
    "Adolygwch flwch 1 a blwch 6 eich ffurflen TAW.",
    "Adborth CThEF: Mae TAW sy'n ddyledus ar werthiannau yn ymddangos yn isel",
    "Hysbysiad TAW 700",
    "https://www.gov.uk/guidance/vat-notice-700-the-vat-guide",
    "vatDueSales"
  )
  // RDS sends item 0 with PATH "0" when it has nothing to report
  private val englishNoFeedback = ActionRow(
    "0",
    "This is not a complete check of your return or confirmation that your return is accurate. Your return has not been submitted at this stage.",
    "Consider any further checks before finalising your return.",
    "HMRC feedback: HMRC Assist has not returned any messages.",
    "Go to GOV.UK for more information on HMRC Assist",
    "https://www.gov.uk",
    "0"
  )
  private val welshNoFeedback = ActionRow(
    "0",
    "Nid yw hwn yn wiriad cyflawn o’ch Ffurflen Dreth nac yn gadarnhad bod eich Ffurflen Dreth yn gywir. Nid yw’ch Ffurflen Dreth wedi cael ei chyflwyno ar hyn o bryd.",
    "Ystyriwch unrhyw wiriadau pellach cyn cadarnhau’ch Ffurflen Dreth yn derfynol.",
    "Adborth CThEF: Nid yw HMRC Assist wedi dychwelyd unrhyw negeseuon.",
    "Ewch i GOV.UK i gael rhagor o wybodaeth am HMRC Assist",
    "https://www.gov.uk",
    "0"
  )

  def reportFor(feedbackId: String, vrn: String): JsValue =
    report(feedbackId, vrn, Some(rdsCorrelationId), Seq(englishFeedback), Seq(welshFeedback))

  // No feedback: a single item 0 message and no correlation id because nothing needs acknowledging
  def noFeedbackReportFor(feedbackId: String, vrn: String): JsValue =
    report(feedbackId, vrn, None, Seq(englishNoFeedback), Seq(welshNoFeedback))

  private def report(feedbackId: String, vrn: String, correlationId: Option[String], english: Seq[ActionRow], welsh: Seq[ActionRow]): JsValue =
    envelope(
      reportModule,
      output("correlationId", correlationId.fold[JsValue](JsNull)(JsString(_))),
      output("createdDttm", JsString(createdDttm())),
      output("englishActions", grid(english)),
      output("feedbackId", JsString(feedbackId)),
      output("vrn", JsString(vrn)),
      output("welshActions", grid(welsh)),
      output("rt_Record_Contacts_Outer", JsString(UUID.randomUUID().toString))
    )

  private def envelope(moduleId: String, outputs: JsObject*): JsObject =
    Json.obj(
      "links" -> Json.arr(),
      "version" -> 2,
      "moduleId" -> moduleId,
      "stepId" -> "execute",
      "executionState" -> "completed",
      "metadata" -> Json.obj("module_id" -> moduleId, "step_id" -> "execute"),
      "outputs" -> outputs
    )

  private def output(name: String, value: JsValue): JsObject = Json.obj("name" -> name, "value" -> value)

  private def grid(rows: Seq[ActionRow]): JsArray =
    Json.arr(
      Json.obj("metadata" -> columns.map(column => Json.obj(column -> "string"))),
      Json.obj("data" -> rows.map(_.toJson))
    )

  private def createdDttm(): String =
    LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS).toString

  def acknowledgementFor(vrn: String, feedbackId: String, accepted: Boolean): JsValue =
    val (code, message) =
      if accepted then (202, "Acknowledgement accepted")
      else (401, "Acknowledgement validation failed")

    envelope(
      acknowledgeModule,
      output("vrn", JsString(vrn)),
      output("createdDttm", JsString(createdDttm())),
      output("feedbackId", JsString(feedbackId)),
      output("responseCode", JsNumber(code)), 
      output("responseMessage", JsString(message)),
      output("rt_Record_Contacts_Outer", JsString(UUID.randomUUID().toString))
    )

  final case class ActionRow(itemNumber: String, message: String, action: String, title: String, linkTitle: String, linkUrl: String, path: String):
    def toJson: JsArray = Json.arr(itemNumber, message, action, title, linkTitle, linkUrl, path)