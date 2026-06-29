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

package uk.gov.hmrc.mtdtransactionriskingstub.utils
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

import play.api.Logging
import play.api.libs.json.{JsValue, Json}

import java.util.UUID
import javax.inject.Singleton
import scala.io.Source
import scala.util.Using

@Singleton
class StubResource extends Logging:

  def loadFeedbackResponse(fileName: String): JsValue =
    val reportId      = UUID.randomUUID().toString
    val correlationId = UUID.randomUUID().toString

    val templateContent = findResource(s"resources/response/feedback/$fileName").getOrElse(
      throw new IllegalStateException(
        s"[StubResource][loadFeedbackResponse] Template not found: $fileName"
      )
    )
    
    Json.parse(
      templateContent
        .replace("ReportId",      reportId)
        .replace("CorrelationId", correlationId)
    )

  def loadErrorResponse(fileName: String): JsValue =
    val content = findResource(s"resources/response/feedback/$fileName").getOrElse(
      throw new IllegalStateException(
        s"[StubResource][loadErrorResponse] Template not found: $fileName"
      )
    )
    Json.parse(content)
    
  def findResource(path: String): Option[String] =
    Option(getClass.getClassLoader.getResourceAsStream(path))
      .fold[Option[String]] {
        logger.error(s"[StubResource][findResource] File not found: $path")
        None
      } { stream =>
        Using(stream)(Source.fromInputStream(_).mkString).toOption
      }
