# Task: Generate live local reports with Qwen3 8B

Status: Complete
Created: 2026-09-24
Owner: Christopher Guzowski

## Goal

Make **Generate proposed report** produce a schema-valid, evidence-linked report
in the one-click local SynTen workflow using the installed Ollama
`qwen3:8b-q4_K_M` model.

## User story

As a payment operations analyst, I want report generation to use the evidence
and approved knowledge already collected for my investigation, so I can review
an attributable proposed report without sending synthetic data to a hosted
model provider.

## Chosen contract

- Local report generation uses Ollama `qwen3:8b-q4_K_M` at
  `http://localhost:11434`; local embeddings remain Ollama
  `nomic-embed-text`.
- Report requests use temperature zero, at most 4,096 output tokens, no tools,
  explicitly disabled thinking, and Ollama-native `report-v1` JSON Schema
  output constraints.
- Application-owned parsing, semantic validation, citation validation,
  immutable attempts, the two-minute total deadline, and human review remain
  authoritative even when the provider constrains output.
- The launcher preflight verifies that Ollama is reachable and that both exact
  local model tags are installed. It does not pull models automatically.
- Automated tests disable live chat and embedding providers and remain
  deterministic and network-free.
- Direct API provider failures remain reviewable `UNAVAILABLE`, `TIMED_OUT`, or
  `MALFORMED` attempts; no cloud fallback is introduced.

## In scope

- Local Spring AI chat configuration and report-model options.
- Passing the existing report schema to the Ollama provider boundary.
- Launcher Ollama/model prerequisite checks and regression coverage.
- A live report smoke and full generated S001 report workflow.
- Relevant ADR, setup, architecture, status, and task documentation.

## Out of scope

- Report schema, persistence schema, HTTP contract, lifecycle, or UI changes.
- Automatic Ollama installation or model downloads.
- Hosted model providers, credentials, cloud fallback, or AWS deployment.
- Knowledge ranking changes, new evidence domains, authentication, or another
  tenant or incident family.

## Constraints

- Follow ADR-0006, ADR-0007, and ADR-0010 while recording the newly selected
  live report model decision.
- Follow red-green-refactor for every executable change.
- Preserve strict evidence and approved-knowledge source validation.
- Never persist or display hidden reasoning output.
- Use only synthetic data and never add credentials.

## Acceptance criteria

- [x] Local configuration enables Ollama chat with exact model ID
      `qwen3:8b-q4_K_M` while retaining `nomic-embed-text` embeddings.
- [x] Each report call explicitly disables thinking, uses temperature zero,
      caps output at 4,096 tokens, provides the exact `report-v1` JSON Schema,
      and registers no tools.
- [x] Automated tests remain network-free and cover unavailable, timeout,
      malformed, schema-invalid, and successful report outcomes.
- [x] Launcher preflight rejects unreachable Ollama or either missing model with
      an actionable command and never downloads a model automatically.
- [x] A live report smoke returns one application-valid `report-v1` document
      using the installed local model.
- [x] A newly generated S001 investigation produces an `AVAILABLE` report,
      moves to `AWAITING_REVIEW`, and displays only persisted source references.
- [x] Focused suites and the authoritative `./verify.ps1` gate pass with zero
      failures, errors, or skips.

## Test plan

1. Change configuration and model-adapter tests first and confirm they fail
   against chat-disabled, unconstrained behavior.
2. Extend launcher contract coverage first and confirm it fails before adding
   Ollama inventory validation.
3. Implement the minimal local configuration, schema-bearing model request,
   and preflight changes.
4. Run focused report/configuration and launcher tests, followed by backend and
   repository scopes.
5. Run the explicit live report smoke and the complete S001 workflow.
6. Run the authoritative gate and review the final diff for secrets, generated
   output, and unrelated changes.

## Progress notes

- 2026-09-24: Diagnosis proved local chat was intentionally disabled with
  `spring.ai.model.chat=none`, so the optional report provider deterministically
  recorded `UNAVAILABLE` while embeddings and retrieval continued to work.
- 2026-09-24: The owner selected fully local Ollama
  `qwen3:8b-q4_K_M`. Local inventory verification found the exact 8.2B Q4_K_M
  tag and `nomic-embed-text` installed.
- 2026-09-24: Red tests captured the previously disabled chat configuration,
  missing Ollama inventory preflight, unconstrained provider call, and report
  schema bounds. The implementation now sends deterministic, no-tool requests
  with thinking disabled and a per-investigation schema whose citation enums
  contain only the persisted evidence and approved-knowledge IDs.
- 2026-09-24: Direct Qwen trials exposed evidence/knowledge ID role mixing.
  Native schema constraints now prevent that at the provider boundary while
  the existing application parser and semantic validator remain authoritative.
- 2026-09-24: After R2 acceptance, the owner closed the core MVP/live local
  demo loop. Follow-on work is quality hardening in this order: evaluation
  integrity; retrieval-quality resolution or explicit acceptance; automated
  answer-key report grading; one complete live-model audit proof; and broader
  live-model scenario coverage. This closure note does not expand the completed
  R2 contract.

## Completion evidence

- 2026-09-24 documentation reconciliation: the implemented and tested output
  cap is 1,536 tokens, matching ADR-0012. The original contract above records
  a 4,096-token ceiling; its acceptance wording is preserved as historical
  evidence rather than silently rewritten. The configured lower cap is not
  evidence of a 4,096-token request.

- Focused configuration/report tests passed 17/17 and the report package passed
  16/16 with no network access; launcher prerequisite and system tests passed.
- `./start-local.bat --CheckOnly` passed with exact local model inventory, and
  the standalone generator launcher contract passed.
- Live generated S001 incident `0e1c29f6-03c4-4dea-8858-cc2323db9c4e`
  produced report attempt `90da759c-263a-4761-b946-b7cc5f422870` as
  `AVAILABLE`/`PROPOSED` with `qwen3:8b-q4_K_M`, `report-prompt/v3`, and
  `report-v1`; the incident moved to `AWAITING_REVIEW`.
- API and browser review confirmed the report displayed only the persisted
  evidence ID `c0d5efde-b545-4a80-a7ed-692bd0e0b905` and approved-knowledge
  snapshot references. The browser showed the report content and audit state.
- `./verify.ps1` passed 290/290 copilot API, 9/9 operations MCP, 17/17
  standalone generator, and 78/78 Angular tests with zero failures, errors, or
  skips. Spotless, Prettier, production builds, Compose validation,
  verification contracts, and `git diff --check` also passed.

## Remaining limitations

- The verified report took about 76 seconds on this machine. It remains inside
  the two-minute deadline but local latency depends on available CPU/GPU and
  memory.
- Live acceptance covers one generated S001 scenario, not a broad report-
  quality benchmark. Automated tests continue to use deterministic doubles.
- Local startup requires Ollama plus the exact `qwen3:8b-q4_K_M` and
  `nomic-embed-text` tags; the launcher intentionally never downloads them.
- The locked frontend install reports three dependency advisories (two
  moderate, one high). They predate and are outside this report-generation
  change; the authoritative repository gate does not currently fail on them.

## Decisions needed

None.
