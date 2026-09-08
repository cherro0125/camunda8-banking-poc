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
- No authentication configured (`camunda.client.auth.method: none`) — fine for a local cluster, not for anything shared.
