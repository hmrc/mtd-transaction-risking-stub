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
import play.api.libs.json.{JsValue, Json, OWrites}
import uk.gov.hmrc.mtdtransactionriskingstub.models.nrs.NrsSubmissionRequest

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest
import java.time.Instant
import java.util.{Base64, UUID}
import java.util.concurrent.ConcurrentLinkedDeque
import javax.inject.Singleton
import scala.jdk.CollectionConverters.*
import scala.util.Try

@Singleton
class NrsStubService:

  private val expectedApiKey = "nrs-test-api-key"
  private val maxCaptured    = 100

  // Must match the NotableEventType values sent by mtd-transaction-risking
  private val supportedNotableEvents = Set(
    "vata-request-feedback",
    "vata-generate-report",
    "vata-acknowledge-report"
  )

  private val captured = new ConcurrentLinkedDeque[NrsSubmissionSummary]()

  def submit(apiKey: Option[String], correlationId: Option[String], body: JsValue): NrsStubResponse =
    validate(apiKey, correlationId, body).map { submission =>
      val submissionId = UUID.randomUUID().toString

      capture(NrsSubmissionSummary.from(submissionId, correlationId.getOrElse(""), submission))

      NrsStubResponse(ACCEPTED, Json.obj("nrSubmissionId" -> submissionId))
    }.merge

  def submissionsFor(vrn: String, reportId: String): Seq[NrsSubmissionSummary] =
    captured.iterator().asScala.filter(s => s.vrn == vrn && s.reportId == reportId).toSeq

  private def validate(apiKey: Option[String], correlationId: Option[String], body: JsValue): Either[NrsStubResponse, NrsSubmissionRequest] =
    for
      _ <- check(apiKey.contains(expectedApiKey), UNAUTHORIZED, "NRS_INVALID_API_KEY", "The X-API-Key header is missing or invalid")
      _ <- check(correlationId.exists(isUuid), BAD_REQUEST, "NRS_INVALID_CORRELATION_ID", "The X-Correlation-Id header must be a UUID")

      submission <- body.validate[NrsSubmissionRequest].asEither.left.map(_ =>
        error(BAD_REQUEST, "NRS_INVALID_SUBMISSION", "The NRS submission body is invalid")
      )
      metadata = submission.metadata

      _ <- check(metadata.businessId == "vata", BAD_REQUEST, "NRS_INVALID_BUSINESS_ID", "businessId must be vata")
      _ <- check(
        supportedNotableEvents.contains(metadata.notableEvent),
        BAD_REQUEST,
        "NRS_UNSUPPORTED_NOTABLE_EVENT",
        s"Unsupported notableEvent: ${metadata.notableEvent}"
      )
      _ <- check(
        metadata.payloadContentType == "application/json",
        BAD_REQUEST,
        "NRS_INVALID_PAYLOAD_CONTENT_TYPE",
        "payloadContentType must be application/json"
      )

      payload <- decodeJsonPayload(submission.payload)
      _ <- check(
        sha256(payload) == metadata.payloadSha256Checksum,
        BAD_REQUEST,
        "NRS_INVALID_PAYLOAD_CHECKSUM",
        "payloadSha256Checksum does not match the decoded payload"
      )
    yield submission

  private def check(condition: Boolean, status: Int, code: String, message: String): Either[NrsStubResponse, Unit] =
    Either.cond(condition, (), error(status, code, message))

  private def decodeJsonPayload(payload: String): Either[NrsStubResponse, Array[Byte]] =
    Try {
      val bytes = Base64.getDecoder.decode(payload)
      Json.parse(String(bytes, UTF_8))
      bytes
    }.toOption.toRight(error(BAD_REQUEST, "NRS_INVALID_PAYLOAD", "payload must be Base64-encoded valid JSON"))

  private def capture(summary: NrsSubmissionSummary): Unit =
    captured.addFirst(summary)
    while captured.size() > maxCaptured do captured.pollLast()

  private def error(status: Int, code: String, message: String): NrsStubResponse =
    NrsStubResponse(status, Json.obj("code" -> code, "message" -> message))

  private def isUuid(value: String): Boolean =
    Try(UUID.fromString(value)).isSuccess

  private def sha256(bytes: Array[Byte]): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).map("%02x".format(_)).mkString

final case class NrsStubResponse(status: Int, body: JsValue)

final case class NrsSubmissionSummary(
                                       submissionId: String,
                                       receivedAt: Instant,
                                       correlationId: String,
                                       businessId: String,
                                       notableEvent: String,
                                       vrn: String,
                                       reportId: String,
                                       payloadContentType: String
                                     )

object NrsSubmissionSummary:

  given writes: OWrites[NrsSubmissionSummary] = Json.writes[NrsSubmissionSummary]

  def from(submissionId: String, correlationId: String, submission: NrsSubmissionRequest): NrsSubmissionSummary =
    NrsSubmissionSummary(
      submissionId = submissionId,
      receivedAt = Instant.now(),
      correlationId = correlationId,
      businessId = submission.metadata.businessId,
      notableEvent = submission.metadata.notableEvent,
      vrn = submission.metadata.searchKeys.vrn,
      reportId = submission.metadata.searchKeys.reportId,
      payloadContentType = submission.metadata.payloadContentType
    )