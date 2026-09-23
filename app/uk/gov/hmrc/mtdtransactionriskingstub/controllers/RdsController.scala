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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.mtdtransactionriskingstub.models.RdsAcknowledgeRequestWrapper
import uk.gov.hmrc.mtdtransactionriskingstub.services.RdsReportService
import uk.gov.hmrc.mtdtransactionriskingstub.utils.StubPeriodKeys
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.util.UUID
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class RdsController @Inject()(cc: ControllerComponents) extends BackendController(cc), Logging:

  private val rdsCorrelationId = "E9F65715BBC9222477B27074804BBDD5C73CDE62F84D8B00CFD05B883534AF3D"

  def generateReport(): Action[JsValue] = Action.async(parse.json) { request =>

    val correlationId = request.headers.get("X-CorrelationId").getOrElse("no-correlation-id")
    val periodKey     = (request.body \ "periodKey").asOpt[String].getOrElse("")
    val feedbackId    = UUID.randomUUID().toString

    logger.info(s"$correlationId::[RdsController][generateReport] report requested for periodKey $periodKey")

    val result = periodKey match

      case StubPeriodKeys.rdsBadRequest =>
        BadRequest("Unexpected close marker '}': expected ']'")

      case StubPeriodKeys.rdsNotFound =>
        NotFound("")

      case StubPeriodKeys.rdsServiceUnavailable =>
        ServiceUnavailable("")

      case StubPeriodKeys.rdsMalformedReport =>
        Created(Json.obj("unexpected" -> "shape"))

      case StubPeriodKeys.rdsNoFeedback =>
        Created(RdsReportService.emptyReportFor(feedbackId, rdsCorrelationId))

      case _ =>
        Created(RdsReportService.reportFor(feedbackId, rdsCorrelationId))

    Future.successful(result)
  }

  def acknowledge(): Action[AnyContent] = Action.async { request =>
    val invalidRequest = BadRequest(Json.obj(
      "output" -> Json.obj(
        "responseCode"    -> 401,
        "responseMessage" -> "Acknowledgement validation failed"
      )
    ))

    request.body.asJson
      .toRight(invalidRequest)
      .flatMap(_.validate[RdsAcknowledgeRequestWrapper].asEither.left.map(_ => invalidRequest))
      .fold(
        Future.successful,
        parsed =>
          val vrn        = parsed.vrn.getOrElse("")
          val feedbackId = parsed.feedbackId.getOrElse("")

          Future.successful(
            Ok(Json.obj(
              "output" -> Json.obj(
                "vrn"             -> vrn,
                "feedbackId"      -> feedbackId,
                "createdDttm"     -> "2026-06-09T10:30:00Z",
                "responseCode"    -> 202,
                "responseMessage" -> "Acknowledgement accepted"
              )
            )).withHeaders("X-CorrelationId" -> UUID.randomUUID().toString)
          )
      )
  }
