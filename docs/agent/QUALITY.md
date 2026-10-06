# Verification for the runtime-only repository

The owner explicitly requested removing tests and development verification tooling.
No new tests are added for this cleanup. Validate the distribution through fresh
hash-checked exports, Maven packaging of all three applications, locked npm install,
Angular production build, PowerShell syntax checks and git diff --check.

Perform read-only health/UI probes where a local environment is available. Check
that generated dependencies, .env, output and local state are ignored by Git.
Verify all allowlisted inputs exist and their hashes survive Git newline handling.
Do not claim a new live-model quality measurement or a fresh-database end-to-end
run from compilation and health probes alone. Record factual completion evidence
and limitations in STATUS.md and tasks/current.md.
