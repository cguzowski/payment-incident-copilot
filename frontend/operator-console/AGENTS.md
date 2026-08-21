# Operator console instructions

These instructions extend the repository root `AGENTS.md` for work under the
Angular application.

## Application responsibility

The SPA helps an operations analyst triage an alert, follow evidence gathering,
review an AI-assisted report, and record an approval or rejection. It must make
system actions and evidence provenance understandable.

## Angular rules

- Use strict TypeScript and the Angular version pinned in `package.json`.
- Prefer standalone components and feature-oriented directories.
- Keep domain and orchestration logic out of presentation components.
- Use typed API contracts and centralize HTTP error handling.
- Every asynchronous view must provide loading, empty, success, and error
  states.
- Preserve keyboard access, visible focus, semantic labels, and adequate color
  contrast.
- Do not rely on color alone for severity or decision state.

## UX rules

- Keep the alert queue scannable and sortable by severity and age.
- Clearly separate observed evidence, AI inference, and recommendation.
- Show evidence source and retrieval status near each claim.
- Require an explicit operator action before an investigation is accepted.
- Rejection must capture a reason.
- Do not imply the AI report is authoritative or automatically executed.

## Testing

- Unit-test state transformations and meaningful component behavior.
- Cover loading, empty, partial-data, failure, approved, and rejected states.
- Add an end-to-end happy-path test after the first vertical slice is stable.
