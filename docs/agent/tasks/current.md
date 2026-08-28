# Task: Establish one authoritative verification entry point

Status: Proposed

Created: 2026-08-28

Owner: Christopher Guzowski

## Goal

Make local and CI verification run the same complete repository checks through
one versioned entry point, with pinned toolchains and explicit failure when any
required test is skipped.

## User story

As a repository maintainer using Codex, I want one deterministic verification
command locally and in CI so that an agent cannot accidentally claim completion
after running only a service-specific or incomplete subset of checks.

## Context

The current GitHub Actions workflow runs only `mvn clean verify`. Frontend tests,
formatting, production build, Docker Compose validation, and diff checks are
documented but depend on an agent running several separate commands. Maven is
not wrapped, Node is not pinned at repository level, Java formatting is not
enforced, and the current runners do not fail merely because a test was skipped.

This is a development-system task. It follows the completed service-error
evidence slice and must not change product behavior.

## Proposed contract

- Add one repository-root PowerShell script as the authoritative verification
  entry point. It must run on Windows PowerShell/PowerShell Core locally and
  PowerShell Core on the Ubuntu GitHub Actions runner.
- The script must perform, in a fixed and visible order:
  1. prerequisite and pinned-version checks;
  2. full Maven reactor clean verification through the Maven Wrapper;
  3. a no-skipped-backend-tests gate based on generated Surefire reports;
  4. locked frontend installation;
  5. full non-watch frontend tests with an explicit no-skips gate;
  6. frontend formatting verification;
  7. frontend production build;
  8. Docker Compose configuration validation;
  9. `git diff --check`.
- The script exits nonzero at the first failed required check and identifies the
  failing check without hiding its underlying command output.
- GitHub Actions performs only environment setup and then invokes that same
  script. CI must not maintain a second list of verification commands.
- Add and use the Maven Wrapper; CI and the authoritative script must not depend
  on an independently installed Maven version.
- Pin a supported Node.js line at repository level and make CI consume the same
  pin. Preserve the existing npm lockfile and pinned npm package-manager version.
- Add a Java formatting check to the Maven lifecycle. This task may mechanically
  format existing Java sources only when required to establish the baseline; it
  may not refactor or behaviorally alter them.
- Update `docs/agent/QUALITY.md`, contributor-facing README instructions, and
  project status so all completion guidance points to the authoritative command.

## In scope

- One cross-platform repository verification script.
- Maven Wrapper files and a repository-level Node version pin.
- CI parity with the local script.
- Backend and frontend no-skips enforcement.
- Java formatting enforcement and a mechanical baseline-format pass if needed.
- Focused tests for script decision logic where it can be isolated without
  spawning the full suite.
- Factual development documentation updates.

## Out of scope

- Product, API, database, MCP, frontend behavior, or presentation changes.
- Dependency upgrades unrelated to the wrapper, formatter, or verification
  runner.
- Shared Testcontainers fixtures or test-suite performance work; that is the
  next separate recommendation.
- Roadmap creation, contract extraction, feature-package reorganization, or
  centralized UUID/problem handling.
- Deployment, AWS, release automation, coverage thresholds, or artifact upload.
- Replacing GitHub Actions or adding another CI provider.

## Constraints

- Preserve every existing test and verification check; consolidation may not
  weaken or silently drop one.
- Do not make Docker/Testcontainers tests optional in the completion path.
- Do not download or execute unpinned wrapper or formatter versions at runtime.
- Do not print secrets or ignored `.env` values.
- Do not generate tracked build output, test reports, caches, or local logs.
- Keep each application independently buildable and testable outside the root
  verification command.
- Follow red-green-refactor for script decision logic and CI-contract checks.

## Acceptance criteria

- [ ] One documented root command runs every required backend, frontend,
      formatting, build, Compose, no-skips, and diff check.
- [ ] Windows local verification and GitHub Actions invoke the same versioned
      script rather than duplicate command lists.
- [ ] The Maven Wrapper is committed, pinned, and used by both local verification
      and CI.
- [ ] A supported Node.js line is pinned once and consumed by CI.
- [ ] The verification command fails when any backend or frontend test is
      reported skipped.
- [ ] The verification command fails fast with the failed check named and the
      original command output preserved.
- [ ] Java and frontend formatting are checked without unrelated source changes.
- [ ] Docker/Testcontainers remain mandatory and Docker Compose remains limited
      to PostgreSQL.
- [ ] Existing product behavior and all previously completed task baselines pass
      unchanged.
- [ ] CI, quality guidance, README instructions, and project status all describe
      the same authoritative workflow.
- [ ] The final diff contains no secrets, product behavior changes, unrelated
      upgrades, generated output, or disabled tests.

## Test plan

- Add focused tests for prerequisite/version failure, child-command failure,
  skipped-test report detection, and successful ordered execution using injected
  command execution or a non-destructive test mode.
- Prove the script rejects a synthetic Surefire report containing a skip and the
  selected frontend machine-readable result containing a skip.
- Prove the CI workflow invokes the authoritative script and does not duplicate
  its command list.
- Run the authoritative command locally with Docker available, then verify its
  complete success path in GitHub Actions.

## Expected approach

1. Obtain owner approval for this exact contract and resolve the two decisions
   below before editing executable files.
2. Create a new `codex/` branch after the completed evidence changes have a clean
   checkpoint.
3. Write focused red tests for verification ordering, failure propagation, and
   no-skips detection.
4. Add the minimum script and report readers to pass those tests.
5. Add the Maven Wrapper, Node pin, and Java formatting gate.
6. Replace duplicated CI commands with the authoritative invocation.
7. Run the new root command locally, inspect its generated/ignored outputs, and
   confirm CI passes the same path.
8. Review the final diff strictly for development-system scope.

## Decisions needed

- Approve the repository Node.js pin. Recommended: the latest Angular-supported
  Node 22 LTS patch available when implementation begins, pinned to an exact
  version and consumed by CI.
- Approve the Java formatter. Recommended: a pinned Spotless Maven plugin with
  Palantir Java Format, enforced by `mvn verify` and used only for mechanical
  formatting.

## Progress notes

- 2026-08-28: Repository audit identified local/CI verification parity as the
  highest-value Codex development improvement. The completed evidence task was
  archived before this proposal replaced `current.md`. No executable change has
  begun and this contract is not locked until the owner approves it.

## Completion evidence

- Red-phase evidence:
- Green-phase evidence:
- Full verification:
- Documentation updated:
- Remaining limitations:
