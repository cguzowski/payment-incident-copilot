# Infrastructure

This directory is reserved for deployment infrastructure selected by an ADR.

## Current state

Local PostgreSQL with pgvector is defined in the repository root
`docker-compose.yml`.

Do not create placeholder directories or add production infrastructure until
the first vertical slice runs locally. Before implementation, an ADR must select
the tooling and services and cover networking, IAM boundaries, cost, and
teardown. Never commit state files, credentials, or private environment values.
