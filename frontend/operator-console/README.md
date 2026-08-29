# Operator Console

Angular application for payment operations analysts to triage synthetic alerts,
start an investigation, review observed synthetic service-error evidence, and
retrieve approved synthetic runbook and policy excerpts. Approved knowledge is
kept separate from observed evidence; AI-assisted reports and human decisions
remain later milestones.

## Prerequisites

- Node.js `24.14.1`
- npm `10.8.3`
- The copilot API running on `http://localhost:8080`

Install the locked dependencies and start the development server:

```bash
npm ci
npm start
```

Open `http://localhost:4200`. The development server proxies `/api` to the
copilot API through `proxy.conf.json`.

A core interceptor attaches `X-Synthetic-Tenant-Id` to application requests
and `X-Synthetic-Operator-Id` to operator-attributed mutations. Feature API
services do not place tenant or operator identity in URLs or request bodies.
These caller-supplied values are synthetic demonstration context, not
authentication or production authorization.

## Verification

```bash
npm test -- --watch=false
npm run build
npx prettier --check "src/**/*.{ts,html,scss}" "*.json"
```

The tests cover the alert queue, incident and investigation routes, evidence
collection, approved-knowledge retrieval history and failure states, retries,
and centralized synthetic request context. The console uses synthetic data only
and keeps source material separate from future AI inference and recommendation.
