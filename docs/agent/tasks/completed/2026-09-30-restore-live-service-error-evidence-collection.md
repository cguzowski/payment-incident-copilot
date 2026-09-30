# Task: Restore live service-error evidence collection

Status: Complete — live collection verified
Created: 2026-09-30
Owner: Christopher Guzowski

## Goal

Restore service-error evidence collection reported as always unavailable.

## User story

As an operator, I want to collect the generated incident's observed errors so
that I can continue its investigation.

## Chosen contract

Restore the existing generator MCP v1 connection without changing application
behavior or replacing historical unavailable attempts.

## In scope

Inspect local service health, restore the evidence provider, restart the local
API with the generator endpoint, and verify the affected investigation.

## Out of scope

Production-code changes, automatic retries, and report generation.

## Constraints

Preserve synthetic-only evidence, tenant scope, and immutable attempt history.

## Acceptance criteria

- [x] The generator evidence provider is running on port 8082.
- [x] Collection for the affected S207 investigation returns AVAILABLE with
      sourced observations while earlier UNAVAILABLE attempts remain retained.

## Test plan

Manual operational verification through the tenant-scoped evidence API. No
executable behavior changed, so a new automated regression test is unnecessary.
Run Repository verification for documentation changes.

## Progress notes

- Initially only the API listened on port 8080; neither evidence provider
  listened on port 8081 or 8082.
- Started the generator with
  `.\mvnw.cmd -f syntheticIncidentGenerator/pom.xml spring-boot:run`.
- Collection still returned UNAVAILABLE immediately with the existing API
  process. Its original endpoint/session state was not established.
- Restarted the API with local environment settings and
  `OPERATIONS_MCP_BASE_URL=http://localhost:8082` using
  `.\mvnw.cmd -pl backend/copilot-api spring-boot:run`.

## Completion evidence

- `.\verify.ps1 -Scope Repository` passed verification-system, local
  prerequisite/preparation, evaluation-runner, Compose, and diff checks.
- POST `/api/investigations/557b4000-d5eb-4c54-8e7f-6050b0db413e/evidence-collections`
  returned AVAILABLE on 2026-09-30 at 12:39:34 UTC.
- Evidence `be374b86-93a9-46dd-a434-9e3fab15b07f` contains
  `UNICODE_NORMALIZATION_FAILURE` and `MERCHANT_PROFILE_PARSE_FAILED`, each
  with count 36, matching generated S207 source identifiers.
- Earlier unavailable attempts were observed in the history and were not
  rewritten or deleted.

## Remaining limitations

The generator must remain running for generated-alert evidence. Recovery does
not establish automatic reconnection after provider failure or restart.

## Decisions needed

None.
