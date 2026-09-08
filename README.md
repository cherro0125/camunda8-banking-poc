# camunda8-banking-poc

A runnable Camunda 8 + Spring Boot proof of concept for a bank **loan approval** process: credit check, DMN risk scoring, conditional manual review, disbursement, and a compensation path for failed transfers.

This is **Phase 2** of a larger research project. Phase 1 (the design doc, live-validated against a local cluster) lives in Notion: *Camuda Research → Loan Approval — End-to-End Process Design*.

## What's here

- `docker/` — a vendored, trimmed-down copy of Camunda's official [Docker Compose distribution](https://github.com/camunda/camunda-distributions/releases) (v8.9, lightweight profile: Orchestration Cluster + Connectors, H2 storage) — this is how you run Camunda 8 locally for this PoC
- `src/main/resources/processes/loan-approval.bpmn` — the process
- `src/main/resources/processes/risk-scoring.dmn` — the credit-scoring decision (COLLECT/SUM → UNIQUE category)
- `src/main/resources/processes/LoanReviewForm.form` — the manual-review Tasklist form
- `src/main/java/.../worker/` — the three job workers (`CreditCheckWorker`, `DisbursementWorker`, `CompensationWorker`)
- `src/main/java/.../client/` — **stub** integration clients (credit bureau, core banking, notifications) with deterministic fake data so the process is runnable without real external systems
- `src/main/java/.../controller/LoanController.java` — `POST /loans` to start a process instance
- `src/test/` — Camunda Process Test (CPT) suite covering all 5 process paths + 3 DMN boundary cases

## Prerequisites

- Java 21 (this repo was built and tested against Homebrew's `openjdk@21`)
- Maven
- Docker + Docker Compose ≥ 2.24.0 (for running Camunda 8 locally via `docker/`, and for CPT's embedded-engine tests via Testcontainers)
- A running local Camunda 8 cluster to actually exercise the app (see below) — **not** required just to run the CPT test suite, which spins up its own embedded engine via Testcontainers

## Running the tests

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
mvn test
```

This runs the full CPT suite against an embedded Zeebe engine (no local cluster needed, but Docker must be running). Coverage report: `target/coverage-report/report.html`.

## Running the app against a local Camunda 8 cluster

**1. Start Camunda 8 with Docker Compose:**

```bash
cd docker
docker compose up -d
docker compose ps   # wait until "orchestration" and "connectors" report healthy
```

This starts the Orchestration Cluster (Zeebe + Operate + Tasklist) on `http://localhost:8080` and Connectors on `:8086`, using H2 file storage — no auth, matching `application.yaml`'s `camunda.client.auth.method: none`. See `docker/UPSTREAM-README.md` for the full upstream docs (secondary storage swaps, the full/web-modeler variants, etc.) if you need more than the lightweight profile.

**2. Start the app (from the repo root):**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
mvn spring-boot:run
```

The app deploys `loan-approval.bpmn`, `risk-scoring.dmn`, and `LoanReviewForm.form` automatically on startup (via `@Deployment` on `BankingPocApplication`) and listens on **port 8081** (8080 is the Camunda cluster's own REST API).

**3. Tear down when done:**

```bash
cd docker && docker compose down   # add -v to also wipe process data
```

## Optional: run with real OAuth authentication

By default the API has no auth (`camunda.client.auth.method: none`, matching the cluster's `unprotectedApi: true`). To instead run against a real OIDC-secured cluster:

**1. Start Camunda 8 with the OAuth overlay** (adds a standalone dev-mode Keycloak on top of the same lightweight stack — no Postgres/Identity needed):

```bash
cd docker
ORCHESTRATION_CONFIG_FILE=application-h2-oauth.yaml \
  docker compose -f docker-compose.yaml -f docker-compose.oauth.yaml up -d
docker compose -f docker-compose.yaml -f docker-compose.oauth.yaml ps
```

Keycloak imports `docker/keycloak-realm.json` on startup, which declares two clients: `orchestration` (used by the Orchestration Cluster's own OIDC config for interactive Operate/Tasklist login) and `banking-poc` (a service-account/M2M client this app authenticates as). Verify the API is actually protected:

```bash
curl -i http://localhost:8080/v2/topology   # -> 401 Unauthorized
```

**2. Start the app with the `oauth` Spring profile:**

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21
SPRING_PROFILES_ACTIVE=oauth mvn spring-boot:run
```

This activates `src/main/resources/application-oauth.yaml`, which authenticates as `banking-poc` via client-credentials against Keycloak (`http://localhost:18080/realms/camunda-platform`). From here, `POST /loans` behaves exactly as in the no-auth setup.

**Why not `docker-compose-full.yaml`?** Camunda's own official full profile (Keycloak + Identity + Postgres + Elasticsearch) was tried first, and its `component-presets` mechanism turned out to only bootstrap Camunda's own built-in components (`connectors`, `orchestration`, etc.) — not a generic way to register a third-party client. Worse, that specific engine version (`8.9.19`) logged repeated warnings about deprecated legacy Elasticsearch-exporter properties mixed into the same config as the newer unified RDBMS secondary storage, and in practice **nothing was ever actually processed**: deployments and process instances returned success with real keys, but job activation and search queries both came back empty — a genuine bug in that vendored config, unrelated to auth. The lighter setup here (standalone Keycloak, no Identity) sidesteps all of that and was verified end-to-end: unauthenticated requests get 401, the app authenticates and deploys resources, and a submitted loan reaches `COMPLETED` state.

**3. Tear down:**

```bash
cd docker && docker compose -f docker-compose.yaml -f docker-compose.oauth.yaml down -v
```

## Trying it out

Submit a loan application:

```bash
curl -X POST http://localhost:8081/loans \
  -H "Content-Type: application/json" \
  -d '{"customerId": "cust-1", "amount": 5000}'
```

The stub `CreditBureauClientStub` derives a deterministic (fake) credit report from the customer id's hash, so different ids route to different risk categories. Two ids are reserved as demo hooks:

- a customer id containing **`timeout`** (e.g. `cust-timeout`) makes the credit-check worker throw a transient job error (retried with backoff) instead of a normal report
- a customer id containing **`fail`** (e.g. `cust-fail`) makes the disbursement worker throw the `DISBURSEMENT_FAILED` BPMN error, triggering the boundary event and the compensation path

Watch progress in Operate (`http://localhost:8080/operate`) or Tasklist (`http://localhost:8080/tasklist`) — medium-risk applications land a user task in Tasklist for manual approval.

## Known gaps (tracked from the Phase 1 design doc)

- Client stubs (`CreditBureauClientStub`, `CoreBankingClientStub`, `NotificationClientStub`) are deterministic fakes, not real integrations — swap them for real REST clients or Camunda Connectors when this goes beyond a PoC.
- No authentication by default (`camunda.client.auth.method: none`) — fine for a local cluster, not for anything shared. Real OAuth is available as an opt-in profile; see "Optional: run with real OAuth authentication" above. It's still a shared demo secret (`demo-banking-poc-secret`) and a dev-mode Keycloak with no persistent realm data — not production auth, just proof the wiring works.
- The OAuth setup doesn't cover interactive Operate/Tasklist login (only the app's own M2M authentication was verified) — the `orchestration` client is declared in `keycloak-realm.json` for this but untested.
