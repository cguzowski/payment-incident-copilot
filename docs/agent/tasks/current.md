# Task: Lean Git clone

Status: Complete
Owner: Christopher Guzowski
Created: 2026-10-05

## Goal

Keep only what is necessary to build and run the current system from a Git clone.
No new features, tests or ZIP distribution.

## Chosen contract

Owner overrides prior preservation/test requirements for this cleanup. Delete
local backups, temporary evidence, duplicate migration roots, tests, test tooling,
obsolete implementations and past development/evaluation records. Keep runtime
application inputs, startup scripts, build configuration, public contract pins,
required PDF/source provenance and existing-citation PDFs. Preserve application
behavior and local database/model state. Do not commit or push without a request.

## Acceptance criteria

- [x] Unnecessary files and local generated material removed.
- [x] Current three systems export and package; locked console install/build passes.
- [x] Startup instructions match the lean checkout; secrets and output stay ignored.
- [x] Final size and verification limitations recorded.

## Verification

Build/export/static and read-only runtime checks. Tests are removed at the owner's
explicit request, so the former full test gate is retired. No live model/report,
incident creation, human decision or database mutation is needed for this cleanup.

## Progress notes

Initial folder size: about 4.9 GiB, dominated by tmp,
legacy-preservation, duplicate migration workspaces, dependencies and build output.
The owner clarified that Git cloning is the distribution goal; no ZIP is produced.


## Completion evidence

Fresh exports/hash validation and all three Maven packaging commands pass. Fresh locked npm install and production build pass. Normal launcher retry completes after an initial API readiness timeout; final API/source/evaluator health is UP and both UI probes return HTTP 200. PowerShell syntax, all 584 pinned inputs through Git filters, public contract pin, ignore checks and git diff --check pass. No maintained tests remain. Services are stopped after verification and generated material removed.

## Remaining limitations

No fresh-database knowledge import or live-model quality run was performed. The initial native API launch exceeded the existing readiness deadline; the captured launch and retry passed. npm reports ten existing advisories. Database/model disk space is outside this cleanup. Changes are local; no commit or push.

Final footprint: approximately 12.05 MiB including retained Git metadata, versus about 4.9 GiB initially. Maintained source/runtime inputs are approximately 3.29 MiB across 616 files. Services are stopped; dependencies and exported workspaces will be recreated on startup.
