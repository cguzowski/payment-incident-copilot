# Operator Console

Angular application for payment operations analysts to triage synthetic alerts
and, in later milestones, review evidence and record human decisions.

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

The alert queue tests cover loading, empty, populated, error/retry, sorting,
and the tenant-scoped API request. The console uses synthetic data only and
keeps the human-review requirement visible in the application shell.
