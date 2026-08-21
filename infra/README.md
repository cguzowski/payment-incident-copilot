# Infrastructure

This directory will contain local and AWS infrastructure definitions.

## Current state

Local PostgreSQL with pgvector is defined in the repository root
`docker-compose.yml`.

## Future structure

```text
infra/
├── local/       Local-only support configuration
└── aws/         Terraform or AWS CDK after an ADR selects the approach
```

Do not add production infrastructure until the first vertical slice runs
locally. Never commit state files, credentials, or private environment values.
