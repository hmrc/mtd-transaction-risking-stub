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

import play.api.http.Status.*
import play.api.libs.json.{JsObject, JsValue, Json}

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.time.Instant
import java.util.{Base64, UUID}
import java.util.concurrent.ConcurrentLinkedDeque
import javax.inject.Singleton
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal

@Singleton
class NrsStubService:

  private val expectedApiKey = "nrs-test-api-key"

  private val supportedNotableEvents = Set(
    "vata-request-feedback",
    "vata-report-generated",
    "vata-report-acknowledged"
  )
  
  private val capturedSubmissions = new ConcurrentLinkedDeque[NrsSubmissionSummary]()

  def submit(
              apiKey: Option[String],
              correlationId: Option[String],
              contentType: Option[String],
              body: JsValue
            ): NrsStubResponse =
    validate(apiKey, correlationId, contentType, body).fold(
      identity,
      submission => responseFor(
        submission = submission,
        correlationId = correlationId.getOrElse(""),
        scenario = scenarioFrom(submission)
      )
    )

  def submissionsFor(vrn: String, reportId: String): Seq[NrsSubmissionSummary] =
    capturedSubmissions
      .iterator()
      .asScala
      .filter(summary => summary.vrn == vrn && summary.reportId == reportId)
      .toSeq

  private def validate(
                        apiKey: Option[String],
                        correlationId: Option[String],
                        contentType: Option[String],
                        body: JsValue
                      ): Either[NrsStubResponse, NrsSubmissionRequest] =
    for
      _ <- requireApiKey(apiKey)
      _ <- requireCorrelationId(correlationId)
      _ <- requireJsonContentType(contentType)
      submission <- parseSubmission(body)
      _ <- requireExpectedBusinessId(submission)
      _ <- requireSupportedNotableEvent(submission)
      _ <- requireExpectedPayloadContentType(submission)
      _ <- requireValidPayload(submission)
    yield submission

  private def requireApiKey(apiKey: Option[String]): Either[NrsStubResponse, Unit] =
    Either.cond(
      apiKey.contains(expectedApiKey),
      (),
      error(UNAUTHORIZED, "NRS_INVALID_API_KEY", "The X-API-Key header is missing or invalid")
    )

  private def requireCorrelationId(correlationId: Option[String]): Either[NrsStubResponse, Unit] =
    Either.cond(
      correlationId.exists(isUuid),
      (),
      error(BAD_REQUEST, "NRS_INVALID_CORRELATION_ID", "The X-Correlation-Id header must be a UUID")
    )

  private def requireJsonContentType(contentType: Option[String]): Either[NrsStubResponse, Unit] =
    Either.cond(
      contentType.exists(_.equalsIgnoreCase("application/json")),
      (),
      error(BAD_REQUEST, "NRS_INVALID_CONTENT_TYPE", "Content-Type must be application/json")
    )

  private def parseSubmission(body: JsValue): Either[NrsStubResponse, NrsSubmissionRequest] =
    body.validate[NrsSubmissionRequest].asEither.left.map { _ =>
      error(BAD_REQUEST, "NRS_INVALID_SUBMISSION", "The NRS submission body is invalid")
    }

  private def requireExpectedBusinessId(
                                         submission: NrsSubmissionRequest
                                       ): Either[NrsStubResponse, Unit] =
    Either.cond(
      submission.metadata.businessId == "vata",
      (),
      error(BAD_REQUEST, "NRS_INVALID_BUSINESS_ID", "businessId must be vata")
    )

  private def requireSupportedNotableEvent(
                                            submission: NrsSubmissionRequest
                                          ): Either[NrsStubResponse, Unit] =
    Either.cond(
      supportedNotableEvents.contains(submission.metadata.notableEvent),
      (),
      error(
        BAD_REQUEST,
        "NRS_UNSUPPORTED_NOTABLE_EVENT",
        s"Unsupported notableEvent: ${submission.metadata.notableEvent}"
      )
    )

  private def requireExpectedPayloadContentType(
                                                 submission: NrsSubmissionRequest
                                               ): Either[NrsStubResponse, Unit] =
    Either.cond(
      submission.metadata.payloadContentType == "application/json",
      (),
      error(
        BAD_REQUEST,
        "NRS_INVALID_PAYLOAD_CONTENT_TYPE",
        "payloadContentType must be application/json"
      )
    )

  private def requireValidPayload(
                                   submission: NrsSubmissionRequest
                                 ): Either[NrsStubResponse, Unit] =
    try
      val payloadBytes = Base64.getDecoder.decode(submission.payload)

      Json.parse(String(payloadBytes, UTF_8))

      Either.cond(
        sha256(payloadBytes) == submission.metadata.payloadSha256Checksum,
        (),
        error(
          BAD_REQUEST,
          "NRS_INVALID_PAYLOAD_CHECKSUM",
          "payloadSha256Checksum does not match the decoded payload"
        )
      )
    catch
      case NonFatal(_) =>
        Left(
          error(
            BAD_REQUEST,
            "NRS_INVALID_PAYLOAD",
            "payload must be Base64-encoded valid JSON"
          )
        )

  private def scenarioFrom(submission: NrsSubmissionRequest): String =
    (submission.metadata.headerData \ "Gov-Test-Scenario")
      .asOpt[String]
      .filter(_.nonEmpty)
      .filterNot(_ == "-")
      .getOrElse("DEFAULT")

  private def responseFor(
                           submission: NrsSubmissionRequest,
                           correlationId: String,
                           scenario: String
                         ): NrsStubResponse =
    val captureId = UUID.randomUUID().toString

    capture(
      NrsSubmissionSummary(
        submissionId = captureId,
        receivedAt = Instant.now().toString,
        correlationId = correlationId,
        scenario = scenario,
        businessId = submission.metadata.businessId,
        notableEvent = submission.metadata.notableEvent,
        vrn = submission.metadata.searchKeys.vrn,
        reportId = submission.metadata.searchKeys.reportId,
        payloadContentType = submission.metadata.payloadContentType,
        payloadJsonValid = true,
        payloadChecksumValid = true
      )
    )

    scenario match
      case "DEFAULT" =>
        NrsStubResponse(
          ACCEPTED,
          Json.obj("nrSubmissionId" -> captureId),
          scenario
        )

      case "NRS_BAD_REQUEST" =>
        error(BAD_REQUEST, "NRS_BAD_REQUEST", "NRS rejected the submission", scenario)

      case "NRS_UNAUTHORIZED" =>
        error(UNAUTHORIZED, "NRS_UNAUTHORIZED", "NRS rejected the API key", scenario)

      case "NRS_TOO_MANY_REQUESTS" =>
        error(TOO_MANY_REQUESTS, "NRS_TOO_MANY_REQUESTS", "NRS is rate limiting requests", scenario)

      case "NRS_INTERNAL_SERVER_ERROR" =>
        error(INTERNAL_SERVER_ERROR, "NRS_INTERNAL_SERVER_ERROR", "NRS encountered an internal error", scenario)

      case "NRS_SERVICE_UNAVAILABLE" =>
        error(SERVICE_UNAVAILABLE, "NRS_SERVICE_UNAVAILABLE", "NRS is unavailable", scenario)

      case other =>
        error(
          BAD_REQUEST,
          "TEST_ONLY_UNMATCHED_STUB_ERROR",
          s"No NRS stub data matched Gov-Test-Scenario: $other",
          other
        )

  private def capture(summary: NrsSubmissionSummary): Unit =
    capturedSubmissions.addFirst(summary)

    while capturedSubmissions.size() > 100 do
      capturedSubmissions.pollLast()

  private def error(
                     status: Int,
                     code: String,
                     message: String,
                     scenario: String = "VALIDATION"
                   ): NrsStubResponse =
    NrsStubResponse(
      status,
      Json.obj(
        "code" -> code,
        "message" -> message
      ),
      scenario
    )

  private def isUuid(value: String): Boolean =
    try
      UUID.fromString(value)
      true
    catch
      case _: IllegalArgumentException => false

  private def sha256(bytes: Array[Byte]): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(bytes)
      .map("%02x".format(_))
      .mkString

case class NrsStubResponse(
                            status: Int,
                            body: JsValue,
                            scenario: String
                          )

case class NrsSubmissionSummary(
                                 submissionId: String,
                                 receivedAt: String,
                                 correlationId: String,
                                 scenario: String,
                                 businessId: String,
                                 notableEvent: String,
                                 vrn: String,
                                 reportId: String,
                                 payloadContentType: String,
                                 payloadJsonValid: Boolean,
                                 payloadChecksumValid: Boolean
                               ):

  def asJson: JsObject =
    Json.obj(
      "submissionId" -> submissionId,
      "receivedAt" -> receivedAt,
      "correlationId" -> correlationId,
      "scenario" -> scenario,
      "businessId" -> businessId,
      "notableEvent" -> notableEvent,
      "vrn" -> vrn,
      "reportId" -> reportId,
      "payloadContentType" -> payloadContentType,
      "payloadJsonValid" -> payloadJsonValid,
      "payloadChecksumValid" -> payloadChecksumValid
    )

private case class NrsSubmissionRequest(
                                         payload: String,
                                         metadata: NrsSubmissionMetadata
                                       )

private case class NrsSubmissionMetadata(
                                          businessId: String,
                                          notableEvent: String,
                                          payloadContentType: String,
                                          payloadSha256Checksum: String,
                                          userSubmissionTimestamp: String,
                                          identityData: JsValue,
                                          userAuthToken: String,
                                          headerData: JsObject,
                                          searchKeys: NrsSearchKeys
                                        )

private case class NrsSearchKeys(
                                  vrn: String,
                                  reportId: String
                                )

private object NrsSubmissionRequest:
  given reads: play.api.libs.json.Reads[NrsSubmissionRequest] =
    Json.reads[NrsSubmissionRequest]

private object NrsSubmissionMetadata:
  given reads: play.api.libs.json.Reads[NrsSubmissionMetadata] =
    Json.reads[NrsSubmissionMetadata]

private object NrsSearchKeys:
  given reads: play.api.libs.json.Reads[NrsSearchKeys] =
    Json.reads[NrsSearchKeys]