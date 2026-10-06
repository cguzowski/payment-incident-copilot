# Current project status

Last updated: 2026-10-05

The owner ended feature development and requested a lean repository for Git clones.
The current investigation API/console, synthetic generator and evaluator remain.
Tests, obsolete legacy provider/launchers, evaluation archives, completed task/ADR
history, migration experiments, temporary evidence and preservation backups are
removed. Runtime PDF sources/manifests and citation-compatible older PDFs remain.

Fresh exports of all three systems pass their input and exported-file hash checks.
Each exported system passes `./mvnw.cmd -Dmaven.test.skip=true package`.
The console passes a fresh `npm ci --no-audit --no-fund` and `npm run build`
(404.95 kB initial production bundle). The first sandboxed npm install failed with
EPERM while spawning a child; rerunning outside the sandbox passed.
`./start-local.bat --CheckOnly`, PowerShell syntax parsing, public contract pin
validation, Git exclusion checks and `git diff --check` pass. All 584 distinct
pinned source/contract inputs survive Git filters byte-for-byte; exact-byte Git
attributes prevent clone-time newline changes from breaking export hashes.
Normal startup from removed/recreated workspaces packages all systems. The initial API startup attempt exceeded the launcher readiness deadline; a captured native Maven launch started the API in 4.264 seconds, and a normal launcher retry completed. All three final actuator health responses are UP; console and generator UI return HTTP 200. PostgreSQL remains at Flyway V12 with no migration required.

No new tests are added or run; their removal is explicitly owner-authorized.
No new model-quality measurement, fresh-database preparation, incident creation,
report generation, human decision or comparison is claimed. Local database
history and installed models are retained separately from repository cleanup.

Known runtime limitations remain: no production authentication, advisory scores
and bounded citation validation do not establish correctness, model throughput
can cause timeouts/malformed reports, and live semantic quality remains limited.
The normal console install still reports ten advisories (three moderate, five high, two critical). This cleanup does not remediate dependency vulnerabilities or certify production
security. Downloads, generated build/dependency output and model/database storage
will increase disk usage when a user runs the project.

Changes are local and uncommitted; the remote repository has not been updated.

Final footprint: approximately 12.05 MiB including retained Git metadata, versus about 4.9 GiB initially. Maintained source/runtime inputs are approximately 3.29 MiB across 616 files. Services are stopped; dependencies and exported workspaces will be recreated on startup.
