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

import play.api.Logging
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.mtdtransactionriskingstub.services.RdsReportService
import uk.gov.hmrc.mtdtransactionriskingstub.utils.StubPeriodKeys
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class RdsController @Inject()(cc: ControllerComponents) extends BackendController(cc), Logging:

  def generateReport(): Action[JsValue] = Action.async(parse.json) { request =>

    val correlationId = request.headers.get("X-CorrelationId").getOrElse("no-correlation-id")

    val periodKey = input(request.body, "periodKey").getOrElse("")
    val vrn = input(request.body, "vrn").getOrElse("")
    val feedbackId = input(request.body, "fixedId").getOrElse(UUID.randomUUID().toString)

    logger.info(s"$correlationId::[RdsController][generateReport] report requested for periodKey $periodKey")

    val result = periodKey match
      case StubPeriodKeys.rdsBadRequest => BadRequest("Unexpected close marker '}': expected ']'")
      case StubPeriodKeys.rdsNotFound => NotFound("")
      case StubPeriodKeys.rdsServiceUnavailable => ServiceUnavailable("")
      case StubPeriodKeys.rdsMalformedReport => Created(Json.obj("unexpected" -> "shape"))
      case StubPeriodKeys.rdsNoFeedback => Created(RdsReportService.noFeedbackReportFor(feedbackId, vrn))
      case _ => Created(RdsReportService.reportFor(feedbackId, vrn))

    Future.successful(result)
  }

  private def input(body: JsValue, name: String): Option[String] =
    (body \ "inputs")
      .asOpt[Seq[JsObject]]
      .getOrElse(Nil)
      .find(entry => (entry \ "name").asOpt[String].contains(name))
      .flatMap(entry => (entry \ "value").asOpt[String])

  // RDS returns 201 for every processed acknowledgement with the outcome in responseCode
  def acknowledge(): Action[JsValue] = Action.async(parse.json) { request =>
    val body = request.body

    val result =
      if (body \ "inputs").asOpt[Seq[JsObject]].isEmpty then BadRequest("Malformed acknowledge request")
      else
        Created(RdsReportService.acknowledgementFor(
          vrn = input(body, "vrn").getOrElse(""),
          feedbackId = input(body, "feedbackId").getOrElse(""),
          accepted = input(body, "correlationId").contains(RdsReportService.rdsCorrelationId))
        )
    Future.successful(result)
  }
