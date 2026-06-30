# mtd-transaction-risking-stub

A test stub for the [mtd-transaction-risking](https://github.com/hmrc/mtd-transaction-risking)
microservice, part of the HMRC Making Tax Digital (MTD) platform. It simulates the downstream
services that mtd-transaction-risking depends on, allowing developers to test the service locally
and in external test environments without connecting to real downstream systems.

The stub exposes two endpoints:

- `/check/insights` — simulates the CIP risk insights proxy, returning VAT risk assessment data
- `/feedback` — simulates the VAT Assist feedback service, returning HMRC Assist feedback messages
  in English and Welsh, driven by the `Gov-Test-Scenario` request header

## Nomenclature

- **VAT Assist** — the HMRC service that analyses VAT return data and returns feedback messages
  highlighting potential mistakes or discrepancies before formal submission
- **Gov-Test-Scenario** — an HTTP request header used in HMRC external test environments to select
  which canned response a stub returns, without needing to vary the request body
- **Insights proxy** — the CIP (Customer Insight Platform) risk service that returns a risk score
  and correlation ID for a given VAT registration number
- **Feedback response** — a response containing a `reportId`, `correlationId`, and arrays of
  `englishFeedback` and `welshFeedback` messages conforming to the VATAssistResponse schema
- **Stub resource** — a JSON file stored in `conf/resources/response/feedback/` that is loaded at
  runtime and returned as the response body, with placeholder values replaced by generated UUIDs

## Technical documentation

This is a Scala 3 Play Framework microservice, following standard HMRC microservice conventions.
It runs on port **9859** by default.


### Running the app

```bash
sbt run
```

The stub will start on `http://localhost:9859`.

Verify it is running:

```bash
curl http://localhost:9859/ping/ping
```

### Endpoints

#### `POST /check/insights`

Simulates the CIP risk insights proxy. Accepts a JSON body with a `vatRegistrationNumber` field
and always returns a fixed risk assessment response regardless of the `Gov-Test-Scenario` header.

**Request:**
```json
{ "vatRegistrationNumber": "123456789" }
```

**Response (200):**
```json
{
  "attributeType": "VAT_REGISTRATION_NUMBER",
  "attributeValue": "123456789",
  "insights": {
    "strategicRisk": {
      "riskCorrelationId": "180a7587-3b80-40e6-b907-ea641dccbe11",
      "riskScore": 83.33,
      "reasons": ["VRN '123456789' is 1 hops from something risky."],
      "riskData": [{ "hops": 1, "avgHops": 2.51 }]
    }
  }
}
```

#### `POST /feedback`

Simulates the VAT Assist feedback service. The response is determined by the
`Gov-Test-Scenario` header. If no header is present, the `DEFAULT` scenario is used.

| `Gov-Test-Scenario` header | HTTP status | Description |
|---|---|---|
| absent or `DEFAULT` | 200 | One feedback message in English and Welsh |
| `MULTIPLE_FEEDBACK` | 200 | Multiple feedback messages |
| `NO_FEEDBACK` | 200 | Empty feedback arrays |
| `INVALID_VRN` | 400 | VRN has not passed validation |
| `INVALID_PERIODKEY` | 400 | Period key has not passed validation |
| `INVALID_REQUEST` | 400 | Submission has not passed validation |
| `TAX_PERIOD_NOT_ENDED` | 403 | The accounting period has not ended |
| `INSOLVENT_TRADER` | 403 | The client is an insolvent trader |
| any other value | 400 | `TEST_ONLY_UNMATCHED_STUB_ERROR` |

Response JSON files are stored in `conf/resources/response/feedback/`. Each file uses
`ReportId` and `CorrelationId` as placeholders, which are replaced with
freshly generated UUIDs on each request.


### Running the test suite

```bash
sbt test
```

To run tests with coverage:

```bash
sbt clean coverage test coverageReport
```

The coverage report is written to `target/scala-3.3.6/scoverage-report/index.html`.

## Licence

This code is open source software licensed under the
[Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").