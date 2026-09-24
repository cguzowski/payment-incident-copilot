# Operator Console

Angular UI for the synthetic incident work queue, investigations, observed
evidence, approved knowledge, advisory reports, human decisions, and audit
history. Active and Completed views retain work across its lifecycle.

## Development

Use Node.js 24.14.1 and npm 10.8.3. With the API running on port 8080:

```bash
npm ci
npm start
```

Open http://localhost:4200. The development server proxies `/api` through
`proxy.conf.json`. For the complete environment and initial knowledge
preparation, follow the [root README](../../README.md).

## Request identity and review

A core interceptor attaches `X-Synthetic-Tenant-Id` to application requests
and `X-Synthetic-Operator-Id` to operator-attributed mutations. These are
synthetic context, not authentication or production authorization.

Observed evidence, retrieved guidance, and AI inference remain distinct.
Approval and rejection require an explicit human decision and a reason;
neither executes the report recommendation.

## Verification

From the repository root, run `./verify.ps1 -Scope Frontend` for locked
installation, tests, no-skips enforcement, formatting, and production build.
Use [QUALITY.md](../../docs/agent/QUALITY.md) for the full completion policy.

Tests cover queue/navigation, evidence and knowledge outcomes, report failures,
decision states, audit history, and centralized request context.
