# Infrastructure

This directory is reserved for deployment infrastructure selected by an ADR.

## Current state

Local PostgreSQL with pgvector is defined in the repository root
`docker-compose.yml`.

AWS deployment remains deferred under [D1](../docs/agent/ROADMAP.md).
Before implementation, an ADR must select tooling and services and cover
networking, IAM boundaries, cost, and teardown. Do not add placeholder
infrastructure. Never commit state files, credentials, or private environment values.
