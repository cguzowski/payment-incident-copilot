# Task: Receive and queue a synthetic payment alert

Status: Proposed  
Created: 2026-08-20  
Owner: Christopher Guzowski

## Goal

Accept a synthetic payment alert through the API, persist it, and display it in
the operator's alert queue.

## User story

As a payment operations analyst, I want new alerts to appear in a clear queue
so that I can identify which incident to investigate next.

## Context

This is the first executable portion of the product lifecycle. It deliberately
ends before MCP, retrieval, and report generation so the intake and queue
contract can be verified independently.

## In scope

- Define the first synthetic payment-alert contract.
- Add an HTTP endpoint for alert ingestion.
- Validate required input.
- Enforce idempotency using tenant and external alert identifiers.
- Persist a new incident with status `NEW`.
- Add an endpoint that returns queue summaries.
- Generate the Angular application if it has not been initialized.
- Display the alert queue with loading, empty, success, and error states.

## Out of scope

- MCP calls
- Runbook or policy retrieval
- Bedrock integration
- Investigation reports
- Authentication
- Automatic prioritization by AI

## Constraints

- Use synthetic data only.
- Include `tenant_id` even though the UI demonstrates one tenant.
- Use Flyway for database changes.
- Do not return JPA entities from controllers.
- Duplicate alerts must not create duplicate incidents.

## Acceptance criteria

- [ ] A valid alert is persisted with status `NEW`.
- [ ] Repeating the same tenant and external alert ID is idempotent.
- [ ] Invalid alerts receive a structured HTTP 400 response.
- [ ] Queue results contain only the fields necessary for triage.
- [ ] The operator console displays the queue and all asynchronous states.
- [ ] Backend and frontend tests cover successful, invalid, and empty cases.

## Test plan

- Alert ingestion integration tests cover valid persistence, invalid input, and
  duplicate idempotency before the ingestion implementation is written.
- Queue contract tests cover the triage projection and empty results before the
  queue implementation is written.
- Operator-console tests cover loading, empty, success, and error states before
  the queue view is implemented.

## Expected approach

1. Agree on the smallest alert request and queue response schemas, then map
   every acceptance criterion to a named test.
2. Write and run the focused backend tests to confirm the expected failures.
3. Implement the minimum backend behavior needed to pass those tests.
4. If needed, create only the Angular and test harness scaffold; then write and
   run the focused frontend tests to confirm the expected failures.
5. Implement the minimum queue UI behavior needed to pass those tests.
6. Refactor while green, then run the broader validation commands and one
   manual synthetic alert.

## Likely files or components

- `backend/copilot-api/src/main/java/.../incident/`
- `backend/copilot-api/src/main/resources/db/migration/`
- `frontend/operator-console/src/app/features/alert-queue/`

## Validation commands

```bash
mvn clean verify
cd frontend/operator-console && npm test -- --watch=false && npm run build
```

## Decisions needed

- Choose the first concrete payment incident family and sample alert fields.

## Progress notes

- 2026-08-20: Initial task brief created with the repository scaffold.

## Completion evidence

- Red-phase evidence: Not started
- Green-phase evidence: Not started
- Acceptance-criteria coverage: Not started
- Full verification: Not started
- Manual verification: Not started
- Documentation updated: Initial task only
- Remaining limitations: Entire implementation remains
