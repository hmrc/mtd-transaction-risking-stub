# mtd-transaction-risking-stub

A stub for [mtd-transaction-risking](https://github.com/hmrc/mtd-transaction-risking) (VAT Assist).
It stands in for every service that mtd-transaction-risking calls, so the service can be run and
tested without connecting to real downstream systems.

It is used in two ways:

- **External Test** – vendors call VAT Assist through the API platform, and mtd-transaction-risking
  returns canned **feedback** and **acknowledge** responses from this stub. The response is chosen
  with the `Gov-Test-Scenario` header.
- **Local development and automated tests** – mtd-transaction-risking runs its real journey against
  this stub's versions of **vat-api, insights, RDS, RSD and NRS**. The response is chosen with the
  **period key** in the VAT return.

## Running the app

This is a Scala 3 Play Framework microservice. It runs on port **9859** by default.

```bash
sbt run
```

Check it is running:

```bash
curl http://localhost:9859/ping/ping
```

## Endpoints

| Endpoint | Stands in for | Scenario selected by |
|---|---|---|
| `POST /feedback` | the VAT Assist feedback response (External Test) | `Gov-Test-Scenario` |
| Acknowledge (see `conf/app.routes`) | the VAT Assist acknowledge response (External Test) | `Gov-Test-Scenario` |
| `POST /internal/validate/:vrn` | vat-api's internal validate endpoint | period key |
| `POST /check/insights` | the insights proxy | – (fixed response) |
| `POST /rds/assessments/generate` | RDS report generation | period key |
| `POST /rds/assessments/acknowledge` | RDS acknowledgement | correlation ID |
| `POST /rsd/receive-and-store` | the RSD interactions datastore | period key |
| `POST /nrs-orchestrator/submission` | the NRS orchestrator | – (accepts every valid submission) |
| `GET /nrs-orchestrator/submissions` | – (test helper) | – |

### External Test endpoints

#### `POST /feedback`

The response is chosen by `Gov-Test-Scenario`. With no header, the default scenario is used.

| `Gov-Test-Scenario` | Status | Response |
|---|---|---|
| absent | 200 | One feedback message in English and Welsh |
| `MULTIPLE_FEEDBACK` | 200 | Several feedback messages |
| `NO_FEEDBACK` | 200 | Item 0 only, with no `correlationId` and no `path` |
| `INVALID_VRN` | 400 | `VRN_INVALID` |
| `INVALID_PERIODKEY` | 400 | Period key error |
| `INVALID_REQUEST` | 400 | `INVALID_REQUEST` |
| `TAX_PERIOD_NOT_ENDED` | 403 | `TAX_PERIOD_NOT_ENDED` |
| `INSOLVENT_TRADER` | 403 | `RULE_INSOLVENT_TRADER` |
| any other value | 400 | No matching scenario |

The response files are in `conf/resources/response/feedback/`. They contain placeholders that are
replaced on every request:

- `ReportId` – a new UUID
- `CorrelationId` – 64 random upper-case hexadecimal characters (not included in the no-feedback
  response, as no acknowledgement is needed)

#### Acknowledge

The response is chosen by `Gov-Test-Scenario`. With no header, the acknowledgement succeeds.

| `Gov-Test-Scenario` | Status | Code |
|---|---|---|
| absent or `DEFAULT` | success | – |
| `INVALID_VRN` | 400 | `VRN_INVALID` |
| `INVALID_REPORTID` | 400 | `REPORT_ID_INVALID` |
| `INVALID_CORRELATIONID` | 400 | `CORRELATION_ID_INVALID` |
| `INVALID_DATETIME` | 400 | `PRESENTED_DATE_TIME_INVALID` |
| `ACKNOWLEDGEMENT_VALIDATION_FAILED` | 400 | `ACKNOWLEDGEMENT_VALIDATION_FAILED` |
| `INVALID_CREDENTIALS` | 401 | `INVALID_CREDENTIALS` |
| `NOT_AUTHORISED` | 403 | `CLIENT_OR_AGENT_NOT_AUTHORISED` |
| `NOT_FOUND` | 404 | `MATCHING_RESOURCE_NOT_FOUND` |
| any other value | – | No matching scenario |


The error bodies are in `conf/resources/response/acknowledge/`. 

## Running the tests

```bash
sbt test
```

With a coverage report:

```bash
sbt clean coverage test coverageReport
```

The coverage report is written to `target/scala-3.3.6/scoverage-report/index.html`.

## Licence

This code is open source software licensed under the
[Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).