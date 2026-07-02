package uk.gov.hmrc.mtdtransactionriskingstub.models

import play.api.libs.json.{Json, OFormat}

import java.time.OffsetDateTime

case class AcknowledgeStubRequest(vrn: String,
                                  reportId: String,
                                  correlationId: String,
                                  presentedDateTime: OffsetDateTime)

object AcknowledgeStubRequest:
  given format: OFormat[AcknowledgeStubRequest] = Json.format[AcknowledgeStubRequest]