# Task: Safeguard local Bedrock API-key access

Status: Complete
Created: 2026-08-30
Owner: Christopher Guzowski

## Goal

Allow the local copilot API to use the operator's externally stored Amazon
Bedrock API key without copying, rendering, logging, persisting, or otherwise
placing the credential value anywhere in the repository.

The owner authorized implementation on 2026-08-30. This is the active, locked
behavioral contract.

## Chosen contract

- The credential value remains in the Windows environment under
  `AWS_BEARER_TOKEN_BEDROCK`; it is never stored in `.env`, YAML, Java source,
  scripts, documentation, command arguments, test fixtures, or generated files.
- Local startup may resolve the variable from Process, User, or Machine scope,
  but it must never print or return the value. Only the copilot API child process
  receives it; the MCP server and Angular process do not.
- The AWS SDK for Java owns bearer authentication. Application code does not
  construct an `Authorization` header or bind the key as a Spring property.
- Local startup rejects any `AWS_BEARER_TOKEN_BEDROCK` assignment in the ignored
  repository `.env` before loading that file.
- Repository verification scans tracked and non-ignored untracked files, plus
  the local `.env` guard, for likely Bedrock bearer tokens. Failures identify a
  file but never print the matched credential or line content.
- Deterministic tests use synthetic token-shaped strings only. No test or normal
  verification command calls Bedrock.

## In scope

- Local Windows launcher environment handling.
- Repository credential-safety verification and PowerShell regression tests.
- Safe local setup documentation and factual project status.

## Out of scope

- Copying or relocating the owner's real key.
- Printing, measuring, hashing, validating, or making a network request with the
  real key during implementation.
- AWS deployment authentication, IAM-role selection, key rotation automation,
  or changing the P2 report contract.
- Removing unrelated user-owned untracked files.

## Acceptance criteria

- [x] The launcher resolves an externally stored Bedrock bearer token from
      Process, User, or Machine scope without printing its value.
- [x] Only the copilot API child receives the token during launcher startup.
- [x] A token assignment in repository `.env` fails before `.env` import.
- [x] Repository verification rejects token-shaped content without echoing it.
- [x] Spring configuration and application code contain no credential mapping or
      manually constructed bearer header.
- [x] The current key value is absent from tracked, staged, and non-ignored
      untracked project files, and no protected value is added by this task.
- [x] Focused PowerShell tests and repository verification pass.
- [x] Documentation explains Windows environment setup, process refresh, expiry,
      rotation, and the prohibition on repository storage without showing a
      literal key assignment.

## Test plan

- `selectsProcessThenUserThenMachineScopeWithoutReturningTheToken`
- `rejectsBedrockBearerTokenAssignmentInDotEnvWithoutEchoingValue`
- `rejectsTokenShapedRepositoryContentWithoutEchoingValue`
- `includesCredentialSafetyInRepositoryAndFullVerificationPlans`
- Static review confirms the token variable is absent from `.env.example` and
  application configuration and that only the API launch inherits the variable.

## Validation commands

```powershell
./scripts/verification/Verification.Tests.ps1
./verify.ps1 -Scope Repository
git diff --check
```

## Decisions needed

None. The owner explicitly selected a 30-day Bedrock API key stored in the
machine environment and required repository-local secret storage to be
prohibited.

## Progress notes

- 2026-08-30: Owner authorized secure local access to the externally stored
  `AWS_BEARER_TOKEN_BEDROCK` variable and prohibited exposing the value in the
  project directory or GitHub.
- 2026-08-30: Confirmed AWS SDK for Java 2.41.22 contains the Bedrock-specific
  environment-token setting and automatically prefers HTTP bearer
  authentication when the variable is present.
- 2026-08-30: Added a launcher boundary that resolves Process, User, then Machine
  scope and exposes the token only while starting the copilot API child. The MCP
  server and Angular child start with the token cleared.
- 2026-08-30: Added pre-import `.env` rejection and a no-echo repository
  credential gate covering tracked, staged, non-ignored untracked files, and the
  ignored root `.env*` variants other than `.env.example`.

## Completion evidence

- Red-phase evidence: `Verification.Tests.ps1` failed because the repository and
  aggregate plans lacked `credential-safety`; `LocalEnvironment.Tests.ps1`
  failed because the local environment module did not exist.
- Green-phase evidence: `Verification.Tests.ps1` passed seven tests and
  `LocalEnvironment.Tests.ps1` passed both token-scope/API-inheritance and
  `.env`-rejection tests using synthetic values only. The ignored `.env.local`
  regression failed before broader scanning and passed afterward without
  echoing its synthetic value.
- Verification: `./verify.ps1` passed 147 copilot API tests, 9 operations MCP
  tests, and 53 Angular tests with zero failures, errors, or skips. Spotless,
  Prettier, the production build, zero-vulnerability npm audit, Compose
  validation, `credential-safety`, and `git diff --check` also passed.
- Credential review: all edited PowerShell files parsed with zero errors;
  `.env` remains Git-ignored; application configuration and Java source contain
  zero bearer-variable mappings or manually constructed bearer headers; the
  repository credential gate passed without printing candidate content.
- Remaining limitations: the current Codex process cannot see the variable in
  Process, User, or Machine scope, so no live Bedrock request or byte-for-byte
  comparison was performed. A fresh process that inherits the variable is
  required for live use. The user-owned root `package.json` and
  `package-lock.json` were preserved unchanged and passed the credential scan.
