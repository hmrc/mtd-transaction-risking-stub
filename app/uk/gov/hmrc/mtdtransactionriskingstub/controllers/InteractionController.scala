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
import play.api.mvc.{Action, ControllerComponents, Result}
import uk.gov.hmrc.mtdtransactionriskingstub.utils.StubPeriodKeys
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class InteractionController @Inject()(cc: ControllerComponents) extends BackendController(cc), Logging:

  private val actionGroups = Seq("englishActions", "welshActions")

  def store(): Action[JsValue] = Action.async(parse.json) { request =>

    val correlationId = request.headers.get("CorrelationId").getOrElse("no-correlation-id")
    val feedbackId = (request.body \ "feedbackId").asOpt[String].getOrElse("")
    // Only generate-report interactions carry a periodKey 
    val periodKey = (request.body \ "metadata" \ 0 \ "additionalProperties" \ "periodKey").asOpt[String].getOrElse("")

    logger.info(s"$correlationId::[InteractionController][store] received interaction for feedbackId $feedbackId, periodKey $periodKey")

    val result = periodKey match

      case StubPeriodKeys.rsdBadRequest =>
        hipFailure(BadRequest, "body.schema.required" -> "Path '/payload/messages/0/englishActions' validation failed.")

      case StubPeriodKeys.rsdTooManyRequests =>
        hipFailure(TooManyRequests, "throttled" -> "rate limit exceeded")

      case StubPeriodKeys.rsdServerError =>
        InternalServerError(Json.obj())

      case StubPeriodKeys.rsdServiceUnavailable =>
        hipFailure(ServiceUnavailable, "internal" -> "service temporarily unavailable")

      case _ =>
        missingPaths(request.body) match
          case Nil =>
            NoContent

          case missing =>
            // RSD requires path on every action
            logger.warn(s"$correlationId::[InteractionController][store] rejecting interaction with missing path at ${missing.mkString(", ")}")
            hipFailure(BadRequest, missing.map(location => "body.schema.required" -> s"Path '$location' validation failed.") *)

    Future.successful(result)
  }

  private def missingPaths(body: JsValue): Seq[String] =
    (body \ "payload" \ "messages")
      .asOpt[Seq[JsValue]]
      .getOrElse(Nil)
      .zipWithIndex
      .flatMap { (message, index) =>
        actionGroups.collect {
          case group if (message \ group \ "path").asOpt[String].isEmpty => s"/payload/messages/$index/$group"
        }
      }

  // Error uses the HIP shape that RSD returns
  private def hipFailure(status: Status, failures: (String, String)*): Result =
    status(
      Json.obj(
        "origin" -> "HIP",
        "response" -> Json.obj(
          "failures" -> failures.map((failureType, reason) => Json.obj("type" -> failureType, "reason" -> reason))
        )
      )
    ).as("application/json;charset=UTF-8")