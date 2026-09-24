# Project definition

Last reviewed: 2026-09-24
Owner: Christopher Guzowski
Status: Core MVP and live local demo loop complete; quality hardening prioritized

## One-sentence goal

Build an auditable copilot that helps a payment operations analyst investigate
synthetic payment incidents by gathering fragmented evidence, retrieving
approved operational knowledge, generating a structured report, and requiring
a human decision.

## Problem

Payment incident investigation often requires an operator to move between
alerts, transaction records, gateway responses, service telemetry, deployment
history, runbooks, and policies. The platform should assemble a clear evidence
snapshot without pretending that AI inference is verified fact.

## Primary user

- Role: Payment operations or production-support analyst
- Responsibility: Triage alerts, determine probable cause, and choose the next
  operational action
- Pain: Evidence is fragmented across systems and institutional knowledge
- Need: A fast, sourced, reviewable investigation summary

## Primary scenario

1. A synthetic alert enters the platform.
2. The operator sees it in one incident work queue containing all active work.
3. The operator starts an investigation.
4. The platform calls synthetic operational systems through MCP tools.
5. Relevant runbooks and policies are retrieved from approved knowledge.
6. Evidence is normalized with source and retrieval metadata.
7. The configured Spring AI chat model generates a report matching a
   predefined schema; local development uses Ollama.
8. The operator reviews the evidence, inference, and recommendation.
9. The operator approves or rejects the report and supplies a reason.
10. The platform preserves the complete audit history.

## MVP scope

- One synthetic financial-services tenant, SynTen Inc
- One payment incident family
- Synthetic alert ingestion and one tenant-scoped incident work queue
- Operator-triggered investigation
- A small set of read-only MCP tools
- Repository-owned runbook and policy ingestion; the completed slice uses
  Markdown and the next phase adds a substantial synthetic PDF corpus
- PostgreSQL and pgvector retrieval
- Structured report generation with Ollama locally
- Evidence citations and retrieval status
- Approve/reject human decision
- Immutable-style audit timeline
- Dockerized local development and AWS deployment

## Explicit non-goals

- Processing or moving money
- Real customer, card, bank-account, or transaction data
- Production fraud scoring
- Autonomous remediation or report approval
- Complete payment-platform simulation
- Multiple tenants in the first demonstration
- Kafka, Redis, Kubernetes, or microservices added for hypothetical scale
- Training or fine-tuning a foundation model

## Portfolio signals

- Java 21 and modern Spring Boot development
- Spring AI orchestration with Ollama locally and an optional Bedrock
  production profile near deployment
- MCP tool design and orchestration
- PostgreSQL, pgvector, SQL migrations, and data modeling
- Angular workflow-oriented UI
- Responsible-AI and human-in-the-loop controls
- Auditable evidence and decision provenance
- Docker, CI, testing, and AWS deployment

## Completed MVP and next quality phase

The completed core MVP proves the operator workflow from a generated synthetic
alert through sourced evidence, approved knowledge, a schema- and
citation-constrained live local report, mandatory human review, and an
auditable final decision. The next phase measures and hardens that loop without
changing the single-tenant, single-incident-family product boundary:

1. Complete: define SynTen Inc and an auditable inventory of synthetic runbooks
   and policies under `SynTen Inc/`.
2. Complete: generate and validate a substantial, version-controlled set of
   text-based PDF source documents in that directory.
3. Complete: add PDF-aware extraction and chunk provenance while preserving
   the existing knowledge-catalog boundary.
4. Complete: embed and index the corpus in PostgreSQL/pgvector with the
   configured live local embedding model.
5. Complete: prove cited approved-knowledge retrieval and constrained report
   generation in the live local operator workflow with `nomic-embed-text` and
   `qwen3:8b-q4_K_M`; retain the factual retrieval benchmark FAIL.

The ordered quality priorities are:

1. Evaluation integrity: isolate the answer key from every evaluated workflow
   input and reveal it only after the report and decision under evaluation are
   frozen.
2. Retrieval-quality disposition: meet the fixed benchmark without weakening
   it, or explicitly accept the remaining measured ranking limitation.
3. Automated answer-key grading: reproducibly score report correctness,
   evidence and citation use, unsupported claims, latency, and failures.
4. Complete live-model audit proof: carry one newly generated live report
   through a terminal human decision and verify the full audit timeline.
5. Broader live-model coverage: evaluate representative common, uncommon,
   rare, partial-evidence, and unavailable-evidence scenarios.

## Decisions still to fill in

- SynTen Inc corpus: `synten-auth-knowledge/v1`, comprising 22 runbooks and 8
  policies under `SynTen Inc/`, governed by `synten-pdf-authoring/v1`.
- PDF extraction, source-location, and chunking contract: PDFBox 3.0.8 with
  `pdfbox-text-pages/v1`, 1-based PDF page/block locators, and
  `pdf-page-sections/v1` under ADR-0009.
- Bedrock production-profile details: `[choose during deployment milestone]`
- AWS deployment services: `[choose during deployment milestone]`
- Authentication approach: `[deferred to D2 after the live-model knowledge phase]`
