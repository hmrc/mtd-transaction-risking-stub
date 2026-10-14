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

object StubPeriodKeys:

  // Obligation lookup
  val periodKeyNotFound = "ZZ99"      // No obligation exist vat-api returns PERIOD_KEY_NOT_FOUND
  val taxPeriodNotEnded = "ZZ98"      // The obligation exists but its period has not ended vat-api returns TAX_PERIOD_NOT_ENDED

  // RDS report generation
  val rdsBadRequest = "RD01"          // RDS returns 400 as request malformed (400)
  val rdsNotFound = "RD02"            // RDS is not reachable
  val rdsServiceUnavailable = "RD03"  // RDS is unavailable (503)
  val rdsNoFeedback = "RD04"          // RDS generates a report with no feedback messages
  val rdsMalformedReport = "RD05"     // RDS returns a 201 with a malformed report

  // Interactions datastore
  val rsdBadRequest = "RS01"          // datastore rejects with a bad request (400)
  val rsdServerError = "RS02"         // datastore fails internally (500)
  val rsdServiceUnavailable = "RS03"  // datastore unavailable (503)
  val rsdTooManyRequests = "RS04"