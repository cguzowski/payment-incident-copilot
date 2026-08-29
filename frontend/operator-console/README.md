# Operator Console

Angular application for payment operations analysts to triage synthetic alerts,
start an investigation, review observed synthetic service-error evidence, and
retrieve approved synthetic runbook and policy excerpts. Approved knowledge is
kept separate from observed evidence; AI-assisted reports and human decisions
remain later milestones.

## Prerequisites

- A Node.js version supported by the Angular version in `package.json`
- The copilot API running on `http://localhost:8080`

Install the locked dependencies and start the development server:

```bash
npm ci
npm start
```

Open `http://localhost:4200`. The development server proxies `/api` to the
copilot API through `proxy.conf.json`.

## Verification

```bash
npm test -- --watch=false
npm run build
npx prettier --check "src/**/*.{ts,html,scss}" "*.json"
```

The tests cover the alert queue, incident and investigation routes, evidence
collection, approved-knowledge retrieval history and failure states, retries,
and tenant-scoped API requests. The console uses synthetic data only and keeps
source material separate from future AI inference and recommendation.
