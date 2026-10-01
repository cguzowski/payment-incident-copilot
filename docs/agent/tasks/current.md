# Task: Restore passing GitHub repository checks

Status: Complete — all four GitHub checks pass
Created: 2026-10-01
Owner: Christopher Guzowski

## Goal

Resolve the two failed GitHub checks on main without weakening verification.

## User story

As the repository owner, I want all four GitHub checks to pass so the current
implementation has reproducible verification on the Ubuntu CI runner.

## Chosen contract

Use a platform-native synthetic root in knowledge-preparation tests. Preserve
all six preparation steps and their environment, provider and mode assertions.
Keep the existing CI scopes and aggregate success requirement.

## In scope

Portable test fixture, focused and full verification, GitHub CI verification.

## Out of scope

Application behavior, accepted PDF bytes, deployment and workflow gate changes.

## Constraints

No real data or live model providers; no skipped or weakened checks.

## Acceptance criteria

- [x] Knowledge-preparation assertions pass with a platform-native root.
- [x] Focused repository scope and full verify.ps1 pass locally.
- [x] GitHub backend, frontend, repository and ci checks pass on the fix commit.

## Test plan

Retain LocalKnowledgePreparation.Tests.ps1 as the regression: run 48 failed on
Ubuntu at Join-Path with "Cannot find drive ... C ... does not exist". Run the
focused script, Repository scope and full verify.ps1 on Windows; run the unchanged
workflow on GitHub Ubuntu to verify portability and all four check outcomes.

## Progress notes

- Clean checkout on main at 8245b8e. GitHub run 36878944702 has passing backend
  and frontend; repository fails in the synthetic C: fixture and ci propagates
  that failure. No production behavior change is required.
- Archived completed E3 task before replacing the current contract.
- GitHub run 36882660901 proves the portable root passes on Ubuntu. It exposed
  a second separator-dependent test fixture: the exact three-file oracle
  allowlist used backslashes while GetRelativePath returned forward slashes.
  Normalized relative paths and existing allowlist/suffix comparisons to slash
  separators; permitted membership and forbidden-reference detection are unchanged.
- First full gate passed the backend and generator builds/tests, then npm ci
  failed with EPERM unlinking esbuild.exe held by the running Angular preview.
  Stopped only verified repository preview PID 26044 and its esbuild child
  PID 32824; reran the full gate. The preview will be restored after verification.

## Completion evidence

- Focused scripts/local/LocalKnowledgePreparation.Tests.ps1 passed all existing
  preparation assertions under PowerShell 7 on Windows.
- ./verify.ps1 -Scope Repository passed all six script suites, Compose validation
  and git diff --check on 2026-10-01.
- Full ./verify.ps1 passed after releasing the preview lock: 362 API, 9 MCP,
  52 generator and 103 console tests; zero failures/errors/skips. Script suites,
  Java/Prettier formatting, production builds, Compose and diff checks passed.
- After the second test-only portability fix, Repository scope passed again.
  A temporary unauthorized backend Java oracle-reference probe was correctly
  rejected by keepsOracleAndGraderOutOfRuntimeInputs and removed afterward.
- Restored Angular preview PID 4336 with the original host/port; HTTP 200.
- GitHub run [36883849928](https://github.com/cguzowski/payment-incident-copilot/actions/runs/36883849928)
  passed backend, frontend, repository and ci on fix commit
  2566145f2fd291003e6d7dd26d716faf9ff03323. Ubuntu ran the unchanged verification
  scopes, including both corrected script fixtures. Local full verification
  preceded the second test-only fix; its relevant Repository scope was rerun
  locally and all three scopes passed afterward on GitHub.

## Remaining limitations

No remaining limitation for this CI repair. Existing npm advisories and product
limitations remain recorded separately; no dependency changes were made.

## Decisions needed

None.
