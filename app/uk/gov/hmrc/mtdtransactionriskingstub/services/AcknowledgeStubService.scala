package uk.gov.hmrc.mtdtransactionriskingstub.services

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.mtdtransactionriskingstub.utils.StubResource

import javax.inject.{Inject, Singleton}



@Singleton
class AcknowledgeStubService @Inject()(stubResource: StubResource):

  def AcknowledgeFor(scenario: String): Option[StubResponse] =
    scenario match
      case "DEFAULT"             => Some(success)
      case "INVALID_VRN"         => Some(error(400, "error-invalid-vrn-format.json"))
      case "FORMAT_RECEIPT_ID"   => Some(error(400, "error-invalid-receipt-id-format.json"))
      case "FORMAT_DATETIME"     => Some(error(400, "error-invalid-datetime-format.json"))
      case "INVALID_CREDENTIALS" => Some(error(401, "error-invalid-credentials.json"))
      case "CLIENT_OR_AGENT_NOT_AUTHORISED"    => Some(error(403, "error-client-or-agent-not-authorised.json"))
      case "CORRELATION_ID"    => Some(error(403, "error-correlation-id.json"))
      case "MATCHING_ RESOURCE_NOT_FOUND" => Some(error(404, "error-matching-resource-not-found.json"))

      case _                     => None
  
  
  private def success: StubResponse =
    SuccessResponse(Json.obj())

  private def error(status: Int, fileName: String): StubResponse =
    ErrorResponse(status, stubResource.loadErrorResponse(fileName))