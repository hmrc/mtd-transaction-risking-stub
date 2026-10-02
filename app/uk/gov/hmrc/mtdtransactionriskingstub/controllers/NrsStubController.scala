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
import uk.gov.hmrc.mtdtransactionriskingstub.services.NrsStubService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class NrsStubController @Inject()(
                                   cc: ControllerComponents,
                                   nrsStubService: NrsStubService
                                 ) extends BackendController(cc), Logging:

  def submit(): Action[JsValue] = Action.async(parse.json) { implicit request =>
    val response = nrsStubService.submit(
      apiKey = request.headers.get("X-API-Key"),
      correlationId = request.headers.get("X-Correlation-Id"),
      contentType = request.contentType,
      body = request.body
    )

    logger.info(
      s"[NrsStubController][submit] " +
        s"status=${response.status} " +
        s"scenario=${response.scenario}"
    )

    Future.successful(
      Status(response.status)(response.body)
        .as("application/json")
    )
  }

  def submissions(vrn: String, reportId: String): Action[AnyContent] = Action {
    val submissions = nrsStubService.submissionsFor(vrn, reportId)

    Ok(
      Json.obj(
        "submissions" -> submissions.map(_.asJson)
      )
    )
  }