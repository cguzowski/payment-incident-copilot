# Task: Demo maintenance cleanup

Status: Complete — full gate passed and demo services restored
Created: 2026-10-01
Owner: Christopher Guzowski

## Goal

Remove demonstrably unnecessary or stale material while preserving the demo.

## User story

As the project owner, I want a lean, simple repository for demonstrating the
completed application.

## Chosen contract

Preserve public behavior, runnable services, tests, historical citations,
frozen tenant assets and audit/comparison artifacts. Remove only material whose
lack of use is established; consolidate stale status prose into current facts
and links to retained completion evidence.

## In scope

Unused style selectors, deployment placeholders, disposable local dependencies
and scratch output, current project documentation and verification.

## Out of scope

Feature changes, migrations, corpus/scenario/oracle modifications, dependency
upgrades, deletion of retained evaluation/comparison artifacts, commits/pushes.

## Constraints

Follow repository guardrails. Do not delete running service binaries or logs,
local configuration, frontend dependencies needed for the demo, or historical
compatibility inputs. Check absolute cleanup paths stay inside the workspace.

## Acceptance criteria

- [x] Remove proven unused selectors and deployment placeholder.
- [x] Remove disposable local clutter while preserving runtime and retained artifacts.
- [x] Keep status documentation concise, accurate and linked to historical evidence.
- [x] Full verification passes and existing demo surfaces remain reachable.
- [x] Final diff contains only scoped maintenance changes.

## Test plan

No production behavior changes are intended, so no artificial regression tests
are added. Confirm selector absence from templates and dynamic class bindings;
run TypeScript noUnusedLocals/noUnusedParameters, frontend formatting/build and
the full ./verify.ps1 gate. Check the existing console, API and generator HTTP
surfaces without creating incidents or invoking models.

## Progress notes

- Clean initial Git status. Read required context and all service instructions.
- Archived the previous completed comparison task unchanged before replacement.
- TypeScript unused-local/parameter check passed. Java import/private-method and
  class-reference review found no safely removable production Java code;
  low-reference types are framework-discovered components or boundary DTOs.
- Three unused shared CSS aliases occur only in their selector declarations.
- Root node_modules contains an unrelated AWS SDK installation; no root npm
  manifest or maintained source/script reference uses it. Frontend dependencies
  remain in frontend/operator-console/node_modules.
- Removed 2,863 disposable files / 6,802,489 bytes, including the root AWS SDK
  installation, 19 old root test logs, six one-off scratch scripts and Python
  bytecode. Removed five empty permission-restricted validation temp folders
  after they blocked fixture-copy tests. Active runtime JARs/logs, evaluation results,
  local configuration and three retained comparison JSON artifacts remain.
- Shortened STATUS.md by about two-thirds, retaining known limitations and
  measured quality gaps with links to original completion/evaluation evidence.
- First sandboxed gate failed with Docker access denied and seven fixture-copy
  errors caused by stale validation temp folder permissions. After removing
  those folders, the unrestricted gate passed 362 API, 9 MCP and 74 generator
  tests with no failures/errors/skips, then failed npm ci because console PID
  12664 held an LMDB native dependency lock. Verified its exact Angular command
  and port before stopping it for the final complete gate.
- Verified live Java command lines: API PID 37328 uses tmp/e3/api-runtime.jar;
  generator PID 28812 uses tmp/comparison-runtime.jar. Removed the unused older
  tmp/e3/generator-runtime.jar and its obsolete scratch launcher, plus four
  empty extraction directories. Total removed: 2,865 files / 40,096,595 bytes.

## Completion evidence

- Final ./verify.ps1 passed on 2026-10-01: 362 API, 9 MCP, 74 generator and
  103 console tests; zero failures/errors/skips, plus seven nested Node UI
  behavior cases. Script checks, Java/Prettier formatting, production builds,
  Compose validation and diff checks passed. Log: tmp/demo-cleanup-verify-final.log.
- TypeScript --noEmit --noUnusedLocals --noUnusedParameters passed. Unused CSS
  alias absence was confirmed across all templates and dynamic class bindings;
  rendered selectors and behavior are unchanged, so no new visual QA was needed.
- Completed comparison archive matches the original HEAD task; changed Markdown
  links resolve. Final diff review found no changes to tenant assets, contracts,
  migrations, compatibility sources, package manifests or lockfiles.
- Restored console as hidden Angular process PID 27336. Console root, API health,
  generator root and generator health all return HTTP 200. Active API/generator
  JARs and three retained comparison artifacts are preserved. No new incidents,
  reports, decisions or model calls were made. No commit or push was performed.

## Remaining limitations

Existing model-quality, authentication, dependency-advisory and deployment
limitations remain documented in STATUS.md.
The final npm ci reports nine advisories (three moderate, four high, two critical).
No dependency upgrades or independent security assessment were performed.

## Decisions needed

None; owner requested cleanup with the demo preserved.
