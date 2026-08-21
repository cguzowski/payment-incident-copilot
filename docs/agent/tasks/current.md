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

## Expected approach

1. Agree on the smallest alert request and queue response schemas.
2. Implement persistence, validation, idempotency, and API contracts.
3. Initialize the Angular application and implement the queue view.
4. Validate through automated tests and one manual synthetic alert.

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

- Tests: Not started
- Manual verification: Not started
- Documentation updated: Initial task only
- Remaining limitations: Entire implementation remains
