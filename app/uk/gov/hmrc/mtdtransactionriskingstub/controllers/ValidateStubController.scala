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
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.mtdtransactionriskingstub.services.VatValidatorService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ValidateStubController @Inject()(cc: ControllerComponents) extends BackendController(cc), Logging:

  def validateReturn(vrn: String): Action[JsValue] = Action.async(parse.json) { request =>

    val correlationId = request.headers.get("X-CorrelationId").getOrElse("no-correlation-id")

    val result = VatValidatorService.validate(vrn, request.body) match
      case Seq() =>
        NoContent

      case Seq(single) =>
        BadRequest(single.singleForm)

      case many =>
        BadRequest(Json.obj(
          "code" -> "INVALID_REQUEST",
          "message" -> "Invalid request",
          "errors" -> many.map(_.bare)
        ))

    Future.successful(result.withHeaders("X-CorrelationId" -> correlationId))
  }