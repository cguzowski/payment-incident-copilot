# Task: Prevent accidental local evidence unavailability

Status: Complete - full verification and live startup/evidence collection passed
Created: 2026-10-01
Owner: Christopher Guzowski

## Goal

Keep normal demo evidence available whenever its provider is healthy.

## User story

As an operator, I want generated incidents to have service-error evidence and
startup to detect configuration failures instead of silently reusing a wrong API.

## Chosen contract

The owner explicitly selected always-available evidence for generated demo
incidents. Random generation selects only AVAILABLE scenarios, retaining the
70/25/5 rarity distribution. Explicit degraded fixtures remain usable for tests
and evaluations. Start the selected MCP provider before the API. Reuse an API
only if its reported provider matches; mismatched or unverifiable APIs fail with
an actionable restart message. Never kill unrelated processes. Report only
sanitized endpoint metadata through actuator info.

## In scope

Demo scenario selection, provider startup selection, API reuse verification,
regression tests, documentation and verification.

## Out of scope

Changing immutable scenario/oracle bytes, rewriting historical evidence,
fabricating observations, human decisions or model behavior.

## Constraints

Preserve provenance, real transport failures and explicit outage tests.
Never expose endpoint credentials/query strings. One writing agent.

## Acceptance criteria

- [x] Normal generation selects only AVAILABLE evidence across all rarity buckets, retaining all seven incident families and 70/25/5 weights.
- [x] Generator mode starts/reuses the generator; legacy mode starts/reuses the legacy provider.
- [x] API reuse requires matching MCP configuration; mismatch, missing metadata and failed inspection give actionable errors.
- [x] Actuator info reports the effective endpoint without user info, query or fragment.
- [x] Focused regressions and full verification pass; runtime limitations are recorded.

## Test plan

Deterministic selector tests cover each candidate in every rarity bucket, family
coverage and fail-closed empty eligible buckets. PowerShell tests exercise actual
startup helpers with HTTP/process doubles. Java tests verify endpoint reporting
and sanitization. Confirm regression failures before implementation, then focused
passes and ./verify.ps1. Live retry requires a configured .env/database.

## Progress notes

- Initial Git status clean. This checkout has no .env or build output. HTTP probes
  to 8080/8081/8082/4200 failed.
- Launcher reuses any healthy API and starts the legacy service when the selected
  generator endpoint is absent.
- Owner authorized available-only demo generation; preserved prior completed task.
- Found and loaded the main checkout's ignored .env without printing credentials.
  Native PostgreSQL and installed Ollama models were available. Confirmed actual
  Windows PowerShell launcher preflight succeeds; initial short HTTP probes alone
  were insufficient to establish Ollama availability.
- Persisted latest failure is S312 / investigation
  8245ff66-ce77-4ceb-8567-9f44fd1a07cf, with status detail
  `Synthetic observation source unavailable.` This was an intentional fixture.

## Completion evidence

- Red: ./mvnw.cmd -f syntheticIncidentGenerator/pom.xml
  -Dtest=WeightedScenarioSelectorTest test failed two regressions: normal selection
  returned PARTIAL, and an unavailable-only rarity did not fail closed.
- Red: API metadata test compilation failed for missing contributor; PowerShell
  startup regressions failed for missing verification module before implementation.
- Green: four selector tests, two OperationsMcpInfoContributorTest tests and
  scripts/local/LocalEvidenceStartup.Tests.ps1 passed. Coverage includes every
  AVAILABLE candidate, all seven families, rarity boundaries, empty eligible
  bucket rejection, both provider directories, reuse, readiness failure, API
  mismatch/missing metadata/HTTP failure and sanitized endpoint metadata.
- ./verify.ps1 passed: 339 API, 9 MCP, 52 generator and 103 Angular tests,
  zero failures/errors/skips; script suites, Java formatting, locked installation,
  frontend format/build, Compose validation and diff checks passed.
- powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/start-local.ps1
  -UseGeneratorMcp -CheckOnly passed. The same actual launcher without CheckOnly
  started generator, API and console, waited for health and verified API metadata.
- Live POST /api/generations created S014 incident
  f0a43589-2864-40ec-9c0c-ff72bdcb1da3. Started investigation
  da62c985-a5c2-44c3-ac13-1d8b0bec8a24 through the copilot API and collected
  AVAILABLE evidence with two payment-authorization observations. Actuator info
  reports http://localhost:8082; original S312 UNAVAILABLE history is unchanged.
- ADR-0019 records the owner-authorized demo contract and startup verification.
  No source/oracle fixture bytes, reports, human decisions or dependencies changed.

## Remaining limitations

Existing deliberately degraded incidents keep their original scenario and history;
new normal demo generation excludes them. Explicit evaluation fixtures still
exercise degraded outcomes. Real transport failures, timeouts and malformed
responses remain visible; availability is not fabricated. Older running APIs
without endpoint metadata require one restart before launcher reuse. API,
generator and console are left running from this checkout. The full gate's npm
installation reported eight existing advisories; npm audit is not a failing gate.

## Decisions needed

None.
