# Task: Thorough behavior-preserving demo cleanup

Status: Implementation prepared — awaiting Docker-verification authorization
Created: 2026-10-01
Owner: Christopher Guzowski

## Goal

Remove inactive authoring tools and unused code/dependencies without changing
application functionality, while retaining useful history and maintenance tools.

## User story

As the demo owner, I want only necessary application code and useful supporting
assets in the active tree, including retirement of completed PDF-authoring tools.

## Chosen contract

Preserve every public workflow, failure path, tenant/evidence/audit boundary,
local startup/preparation command and independently runnable service. Retire
PDF-generation/render/validation Python files into one hash-verified historical
archive with original paths and restore instructions; retain exact frozen PDF,
source, inventory, manifest and authoring-record bytes. Remove unused internal
helpers and the unused ORM layer, preserving existing JDBC transaction behavior.
Keep tests, CI, startup/preparation tools and offline evaluators useful for
verification and reproducing recorded results.

## In scope

Reference/dependency review across all services, frontend and scripts; inactive
PDF-authoring source archival; proven unused internal code and ORM dependencies;
related documentation, package-size checks, focused and full verification.

## Out of scope

Feature/API/data/model changes, corpus reauthoring, evaluation reinterpretation,
dependency upgrades, removal of public maintenance commands or historical data,
commits/pushes, destructive database operations.

## Constraints

Preserve prior cleanup changes. Archive the completed task before replacement.
Do not modify frozen hashes to accommodate cleanup. Compare archive contents to
original bytes before deletion. Keep active demo JARs/configuration/artifacts.
Scope recursive deletion to checked absolute workspace paths. No new behavior
is intended; existing tests characterize functionality before/after refactoring.

## Acceptance criteria

- [x] No active Python PDF-authoring source remains; all original tools are recoverable by exact path/hash.
- [x] Frozen sources/PDFs/manifests and historical records remain byte-identical.
- [ ] Remove proven unused runtime helpers and ORM layer; preserve JDBC atomicity/rollback and all public behavior.
- [x] Review remaining code/dependencies/tooling and document why useful items remain.
- [ ] Focused characterization tests, full gate and final repository/static checks pass.
- [ ] Tested demo services are running and healthy after verification.

## Test plan

Map preservation of transactions to existing named regressions:
AlertApiPostgresIntegrationTest.rollsBackInvestigationWhenIncidentTransitionFails,
HumanDecisionPersistencePostgresIntegrationTest.rollsBackDecisionWhenLifecycleTransitionFails,
ReportPersistencePostgresIntegrationTest.rollsBackAvailableReportWhenLifecycleTransitionCannotCommit,
and SynTenPdfCatalogPostgresIntegrationTest.rollsBackEveryEarlierInsertWhenTheFinalVersionConflicts.
Run these before dependency edits and after refactoring. Existing full suites
cover all remaining workflows and invalid inputs. Because removal is a refactor
with no new behavior, use passing characterization tests rather than artificial
red tests for removed implementation details.

Verify archival integrity entry-by-entry with SHA-256, package/input hash checks,
and restoration into an ignored scratch checkout. Confirm no Python reference
from startup/build/CI, no JPA usage, no unused TS locals/parameters, package library
reduction, Markdown links, git diff --check and the unscoped ./verify.ps1 gate.
A temporary copied-database API smoke check validates the newly packaged runtime
without modifying the demo database. Console may pause for npm ci and is restored.

## Progress notes

- Separate owner-requested investigation diagnostic on 2026-10-01 completed five
  fresh decision/reveal/comparison workflows using the existing tested demo and
  unchanged rubric. Reconciliation's two timeouts are retained. Results and
  optimization candidates are in
  [the diagnostic report](../../../SynTen%20Inc/evaluation/2026-10-01-investigation-diagnostic.md).
  No production changes or cleanup acceptance claims result from this work;
  the locked cleanup contract and pending verification remain unchanged.

- Read required context, service instructions, corpus standards and relevant ADRs.
- Previous cleanup changes remain uncommitted and preserved; archived that
  completed task unchanged before starting this successor.
- No application/startup/CI reference invokes PDF Python tools. Frozen package
  metadata pins authoring tools, so preserve them as recoverable historical bytes.
- All production persistence uses JdbcClient; there are no JPA entities,
  repositories or EntityManager usages. JPA currently adds an unused ORM stack.
- Reference scan found an uncalled package-private report evidenceSnapshotIds
  helper. Framework bean/exception-handler methods remain required.
- Retired all 13 authoring scripts (3,082 lines / 151,326 bytes) into a
  49,538-byte historical ZIP, preserving exact original paths and hashes.
  Frozen package/control records retain their original historical commands;
  the tenant README explains restoring the archive before using those commands.
- Removed unused evidenceSnapshotIds and its List import. Replaced JPA with the
  existing JDBC starter and removed eight dormant Hibernate configuration lines.
- Retained direct runtime dependencies used by HTTP validation, MCP, Spring AI,
  schema validation, PDF parsing, PostgreSQL and JDBC; frontend dependencies are
  required by Angular/runtime/build/test tooling. Hibernate Validator remains.
- Retained independently runnable legacy MCP service, startup/readiness/import/
  backfill/smoke commands, offline evaluators and test/CI tooling because they
  support compatibility, repeatable preparation, diagnosis and historical checks.
- Post-change Docker rollback command was rejected by the permission prompt.
  No post-change Docker/full-gate execution is claimed. Owner was asked whether
  to authorize the complete verification gate or revert the JDBC dependency change.
  Read-only/static and non-Docker checks continued; the demo was not restarted.

## Completion evidence

- Before dependency edits, the four named rollback characterization tests passed
  with zero failures/errors/skips. Log: tmp/deep-cleanup-transactions-before.log.
- After cleanup, ReportGenerationServiceTest, ReportPromptAndParserTest and
  FeatureArchitectureTest passed 33 tests with zero failures/errors/skips.
  Log: tmp/deep-cleanup-unit-checks.log.
- ./mvnw.cmd -pl backend/copilot-api -DskipTests package passed Java formatting,
  compilation and packaging only; it intentionally did not execute tests.
  Packaged libraries fell from 155 to 133; JAR bytes from 89,910,066 to 61,627,328.
  ORM libraries are absent and HTTP Hibernate Validator remains. Removal also
  resolves the existing ANTLR runtime to 4.13.1 instead of ORM's 4.13.2 override.
  Package log and library comparison are retained under ignored tmp/deep-cleanup-*.
- Verified ZIP SHA-256 and every restored entry's path/length/SHA-256. A scratch
  reproduction checkout matched all 13 original tools and frozen generator/
  package-file hashes; 226 protected asset/contract/prompt hashes are unchanged.
  Removed that scratch checkout and empty retired tool directories afterwards.
- TypeScript --noEmit --noUnusedLocals --noUnusedParameters, changed Markdown
  link resolution, tracked diff scope review, git diff --check and
  ./verify.ps1 -Scope Repository passed. Original comparison task archive still
  matches HEAD. No startup/build/CI references invoke retired Python tools.
- Existing console, API health and generator root return HTTP 200. Active runtime
  JARs are untouched; these checks cover the existing demo, not the new JDBC-only
  package. No live models, incident/report/decision mutations, commits or pushes.

## Remaining limitations

Existing model/security/deployment limitations remain in STATUS.md. Archival
requires restoration and compatible Python packages for historical PDF reproduction.
The rejected command was ./mvnw.cmd -pl backend/copilot-api with -Dtest selecting
the four rollback methods listed above, followed by test. It required Docker
access outside the sandbox. Post-change transaction regressions, the unscoped
./verify.ps1 gate and a runtime smoke check of the new package remain unrun.
The existing demo still runs its previous tested API JAR; the new package must
not replace it until verification is authorized and passes.

## Decisions needed

None; owner authorized thorough cleanup preserving functionality and useful history.
