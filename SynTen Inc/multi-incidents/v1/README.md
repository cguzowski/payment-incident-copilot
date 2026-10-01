# SynTen additional incident families v1

All content is synthetic. This additive package leaves the 36-scenario
observable/oracle authorities and authorization PDF corpus unchanged.

| Family | Service | Available | Degraded | Approved guidance |
|---|---|---|---|---|
| AUTHORIZATION_TIMEOUT_SPIKE | payment-authorization | S301 | S311 PARTIAL | authorization-timeout-runbook.md / authorization-timeout-policy.md |
| CAPTURE_FAILURE_SPIKE | payment-capture | S302 | S312 UNAVAILABLE | capture-failure-runbook.md / capture-failure-policy.md |
| REFUND_FAILURE_SPIKE | payment-refund | S303 | S313 PARTIAL | refund-failure-runbook.md / refund-failure-policy.md |
| SETTLEMENT_DELAY | settlement-sim | S304 | S314 UNAVAILABLE | settlement-delay-runbook.md / settlement-delay-policy.md |
| WEBHOOK_DELIVERY_FAILURE | webhook-dispatcher | S305 | S315 PARTIAL | webhook-delivery-runbook.md / webhook-delivery-policy.md |
| RECONCILIATION_MISMATCH | reconciliation-sim | S306 | S316 UNAVAILABLE | reconciliation-mismatch-runbook.md / reconciliation-mismatch-policy.md |

## Authorities and provenance

`catalog.json` owns observable alert fields, explicit incident family, aggregate
error evidence and source availability. `oracle.json` supplies expected outcomes
only to the generator's terminal human-decision answer-key reveal. The v1 wire
schema is retained; this package has a separate content identity and manifest.
Neither the API runtime nor its tests package this oracle. The original offline
36-case evaluator still covers authorization declines only.

The twelve maintained Markdown documents are approved synthetic version 1.0.0,
effective 2026-09-30 UTC, with stable IDs and tenant/approval metadata in front
matter. `manifest.json` fixes exact catalog, oracle and guidance hashes. Changed
approved content requires new document versions, not silent replacement. No PDFs
are added; existing Markdown ingestion and line provenance apply.

## Preparing and using the application

E3 supersedes the following historical Markdown preparation procedure for new
operational retrieval. Use `./start-local.bat -PrepareKnowledge` to import/embed
both accepted PDF catalogs, including the frozen payment library. New searches
are PDF-only; twelve Markdown inputs and previous citations remain intact. See
[ADR-0020](../../../docs/agent/decisions/ADR-0020-accepted-payment-pdf-catalogs.md).
The procedure below records the original v1 package preparation.

Build the API and standalone generator, then restart them (API port 8080,
generator/MCP port 8082). The API must use the generator's MCP endpoint, as in
`start-local.bat`; the legacy operations MCP fixtures do not own these alerts.
Run `./start-local.bat -PrepareKnowledge` to import the approved Markdown
sources after PDF catalog/embedding preparation, using the configured database
and Ollama embedding model. Ordinary startup continues to reuse the prepared
catalog; preparation is explicit. Close old API/generator processes before relaunching
so the new binaries are used. For manual setup, import with the existing command:

```powershell
./mvnw.cmd -pl backend/copilot-api spring-boot:run '-Dspring-boot.run.arguments=--spring.main.web-application-type=none --spring.ai.model.chat=none --app.knowledge.ingestion.enabled=true'
```

Use the existing environment/database configuration. This manual import exits without binding an HTTP port. The import adds twelve documents to the two
legacy Markdown sources; a repeat skips identical versions and leaves the PDF
catalog intact. Normal startup does not import these sources automatically.
Restart normally after preparation. Rebuild the Angular console for production;
a local dev server reloads the changed labels automatically.

Generate an incident in the generator UI. Random selection now includes all
48 scenarios; common/uncommon/rare weighting remains 70/25/5. Start investigation,
collect evidence, retrieve knowledge, generate the advisory report, review exact
sources and approve or reject with a reason. Repeated random generation may
still produce authorization declines. Each new family has its own distinct title.

## Evidence and quality limits

MCP v1 provides aggregate errors in a five-minute observation window. Error
counts do not prove unique payment outcomes, delay durations, percentage rates,
settled balances or financial losses. Alert descriptions remain unverified
signals. Runbooks call out absent independent confirmation records. No payment
stage runs and no money moves. No retry/replay/remediation is executed.

Deterministic PostgreSQL tests cover all twelve new scenarios from typed intake
through evidence, family-scoped retrieval, schema-valid cited reports, explicit
human decision and audit. Provider doubles establish application contracts,
not Qwen report quality. A new live-model benchmark has not been performed.
PARTIAL/unavailable evidence preserves LOW confidence, null conclusions and
explicit gaps. Oracle root causes remain scenario-specific evaluation labels,
not retrievable guidance or verified conclusions.
