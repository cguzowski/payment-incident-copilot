# Project definition

Last reviewed: 2026-09-24
Owner: Christopher Guzowski

## Goal

Build an auditable copilot that helps a payment operations analyst investigate
synthetic payment incidents by gathering fragmented evidence, retrieving
approved operational knowledge, generating a structured report, and requiring
a human decision.

## Primary user and problem

A payment operations or production-support analyst must correlate alerts,
service errors, gateway failures, and operational guidance before choosing the
next action. The platform assembles a sourced investigation without presenting
AI inference as verified fact.

## Operator workflow

1. Receive a synthetic alert in the incident work queue.
2. Start an investigation and collect read-only operational evidence.
3. Retrieve approved runbooks and policies.
4. Generate a structured report from the persisted evidence and knowledge.
5. Review observations, inference, source citations, and missing evidence.
6. Approve or reject the report with an attributable reason.
7. Retain the attempt history and final decision in the audit timeline.

## Scope

- One fictional tenant, SynTen Inc, and the authorization-decline incident family.
- Synthetic alert intake, tenant-scoped queue, and operator-triggered investigation.
- Read-only MCP service-error evidence.
- Explicit versioned Markdown and page-aware PDF knowledge ingestion.
- PostgreSQL/pgvector hybrid retrieval with source and model provenance.
- Local Ollama report generation through Spring AI.
- Application-owned schema/citation validation and mandatory human review.
- Independently buildable API, console, and synthetic source systems.
- Local development with PostgreSQL infrastructure through Docker or native
  installation; AWS deployment remains a later milestone.

## Non-goals

- Processing or moving money, or using real customer/card/bank/transaction data.
- Production fraud scoring or complete payment-platform simulation.
- Autonomous remediation, external communication, or report approval.
- Multiple tenants or incident families in the first demonstration.
- Infrastructure added for hypothetical scale, including Kafka, Redis, or Kubernetes.
- Training or fine-tuning a foundation model.

## Engineering objectives

Demonstrate Java 21, Spring Boot, Spring AI, MCP integration, Angular, SQL
migrations, pgvector retrieval, responsible AI controls, CI, and auditable
evidence and decision provenance. Deployment may later introduce an optional
Bedrock profile under an explicit architectural decision.

## Related authorities

- [CONSTRAINTS.md](CONSTRAINTS.md): non-negotiable boundaries.
- [STATUS.md](STATUS.md): implemented behavior and measured limitations.
- [ROADMAP.md](ROADMAP.md): priorities and deferred deployment/identity decisions.
- [SynTen Inc](../../SynTen%20Inc/README.md): corpus ownership and evaluation.
