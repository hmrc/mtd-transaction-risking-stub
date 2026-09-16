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

package uk.gov.hmrc.mtdtransactionriskingstub.models

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json

class RdsAcknowledgeModelsSpec extends AnyWordSpec, Matchers:

  "RdsAcknowledgeInput" should:
    "round-trip through JSON" in:
      val model = RdsAcknowledgeInput("vrn", "123456789")
      val json  = Json.parse("""{"name": "vrn", "value": "123456789"}""")

      Json.toJson(model) shouldBe json
      json.as[RdsAcknowledgeInput] shouldBe model

  "RdsAcknowledgeRequestWrapper" should:

    "round-trip through JSON" in:
      val model = RdsAcknowledgeRequestWrapper(
        List(
          RdsAcknowledgeInput("correlationId", "9EEB55EF4FA9A24954BC982DF1D59B3D02BC097F6B1377B8B335C7583D92B959"),
          RdsAcknowledgeInput("feedbackId", "cbbb77b9-6779-f24c-b2a6-6f4c27da7900"),
          RdsAcknowledgeInput("vrn", "123456789"),
          RdsAcknowledgeInput("presentedDateTime", "2026-02-15T09:35:15.094Z")
        )
      )
      val json = Json.parse(
        """
          |{
          |  "inputs": [
          |    {"name": "correlationId", "value": "9EEB55EF4FA9A24954BC982DF1D59B3D02BC097F6B1377B8B335C7583D92B959"},
          |    {"name": "feedbackId", "value": "cbbb77b9-6779-f24c-b2a6-6f4c27da7900"},
          |    {"name": "vrn", "value": "123456789"},
          |    {"name": "presentedDateTime", "value": "2026-02-15T09:35:15.094Z"}
          |  ]
          |}
        """.stripMargin
      )

      Json.toJson(model) shouldBe json
      json.as[RdsAcknowledgeRequestWrapper] shouldBe model

    "extract vrn, feedbackId, correlationId and presentedDateTime when all inputs are present" in:
      val wrapper = RdsAcknowledgeRequestWrapper(
        List(
          RdsAcknowledgeInput("correlationId", "corr-id"),
          RdsAcknowledgeInput("feedbackId", "cbbb77b9-6779-f24c-b2a6-6f4c27da7900"),
          RdsAcknowledgeInput("vrn", "123456789"),
          RdsAcknowledgeInput("presentedDateTime", "2026-02-15T09:35:15.094Z")
        )
      )

      wrapper.vrn shouldBe Some("123456789")
      wrapper.feedbackId shouldBe Some("cbbb77b9-6779-f24c-b2a6-6f4c27da7900")
      wrapper.correlationId shouldBe Some("corr-id")
      wrapper.presentedDateTime shouldBe Some("2026-02-15T09:35:15.094Z")

    "return None for fields missing from the inputs list" in:
      val wrapper = RdsAcknowledgeRequestWrapper(
        List(RdsAcknowledgeInput("correlationId", "corr-id"))
      )

      wrapper.vrn shouldBe None
      wrapper.feedbackId shouldBe None
      wrapper.presentedDateTime shouldBe None
      wrapper.correlationId shouldBe Some("corr-id")

    "return None for all fields when inputs is empty" in:
      RdsAcknowledgeRequestWrapper(List.empty).vrn shouldBe None
      RdsAcknowledgeRequestWrapper(List.empty).feedbackId shouldBe None
      RdsAcknowledgeRequestWrapper(List.empty).correlationId shouldBe None
      RdsAcknowledgeRequestWrapper(List.empty).presentedDateTime shouldBe None

    "pick the first matching input when duplicate names are present" in:
      val wrapper = RdsAcknowledgeRequestWrapper(
        List(
          RdsAcknowledgeInput("vrn", "111111111"),
          RdsAcknowledgeInput("vrn", "222222222")
        )
      )

      wrapper.vrn shouldBe Some("111111111")

  "RdsAcknowledgeResponse" should:

    "round-trip through JSON with all fields present" in:
      val model = RdsAcknowledgeResponse(
        vrn             = Some("123456789"),
        feedbackId      = Some("cbbb77b9-6779-f24c-b2a6-6f4c27da7900"),
        createdDttm     = Some("2019-02-15T09:35:15.094Z"),
        responseCode    = Some(202),
        responseMessage = Some("Acknowledgement accepted")
      )
      val json = Json.parse(
        """
          |{
          |  "vrn": "123456789",
          |  "feedbackId": "cbbb77b9-6779-f24c-b2a6-6f4c27da7900",
          |  "createdDttm": "2019-02-15T09:35:15.094Z",
          |  "responseCode": 202,
          |  "responseMessage": "Acknowledgement accepted"
          |}
        """.stripMargin
      )

      Json.toJson(model) shouldBe json
      json.as[RdsAcknowledgeResponse] shouldBe model

    "round-trip through JSON with all fields absent" in:
      val model = RdsAcknowledgeResponse(None, None, None, None, None)
      val json  = Json.obj()

      Json.toJson(model) shouldBe json
      json.as[RdsAcknowledgeResponse] shouldBe model

  "RdsAcknowledgeResponseWrapper" should:
    "round-trip through JSON, matching the vrn and feedbackId sent by the caller" in:
      val model = RdsAcknowledgeResponseWrapper(
        RdsAcknowledgeResponse(
          vrn             = Some("123456789"),
          feedbackId      = Some("cbbb77b9-6779-f24c-b2a6-6f4c27da7900"),
          createdDttm     = Some("2019-02-15T09:35:15.094Z"),
          responseCode    = Some(202),
          responseMessage = Some("Acknowledgement accepted")
        )
      )
      val json = Json.parse(
        """
          |{
          |  "output": {
          |    "vrn": "123456789",
          |    "feedbackId": "cbbb77b9-6779-f24c-b2a6-6f4c27da7900",
          |    "createdDttm": "2019-02-15T09:35:15.094Z",
          |    "responseCode": 202,
          |    "responseMessage": "Acknowledgement accepted"
          |  }
          |}
        """.stripMargin
      )

      Json.toJson(model) shouldBe json
      json.as[RdsAcknowledgeResponseWrapper] shouldBe model

      (json \ "output" \ "vrn").as[String] shouldBe model.output.vrn.get
      (json \ "output" \ "feedbackId").as[String] shouldBe model.output.feedbackId.get