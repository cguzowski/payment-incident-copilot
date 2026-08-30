# Project definition

Last reviewed: 2026-08-30
Owner: Christopher Guzowski
Status: Foundation

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

- One synthetic financial-services tenant
- One payment incident family
- Synthetic alert ingestion and one tenant-scoped incident work queue
- Operator-triggered investigation
- A small set of read-only MCP tools
- Markdown runbook and policy ingestion
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

## Decisions still to fill in

- Bedrock production-profile details: `[choose during deployment milestone]`
- AWS deployment services: `[choose during deployment milestone]`
- Authentication approach: `[defer until the core flow works]`
