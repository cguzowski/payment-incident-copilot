# Synthetic Incident Generator

This directory is an independently runnable synthetic test system. It is not a
module of the root Maven reactor and it does not modify or share production code
with the payment incident copilot.

## Behavioral contract

- A deliberately red button selects one weighted AVAILABLE-evidence scenario
  from the reviewed catalog across seven incident families, preserving the
  70/25/5 common/uncommon/rare distribution. The complete 48-scenario catalog
  retains explicit degraded fixtures for tests and evaluations; normal generation
  excludes them. Real transport outages still fail evidence collection.
- The generator sends only the existing alert-system payload to the copilot
  intake API: opaque external alert ID, explicit incidentType, severity, detected time,
  title and description. Tenant context is carried in the existing synthetic header.
- The copilot API owns the database insert and idempotency behavior; the
  generator never writes into another system's tables.
- The opaque alert ID lets this service reconstruct the selected scenario and
  deterministic evidence through the existing read-only
  `getRecentServiceErrors` MCP contract.
- Alert intake, MCP evidence, and generation responses omit answer-key fields.
  The browser requests the oracle only after the copilot API confirms that the
  exact incident is `APPROVED` or `REJECTED`. Successful reveals record the
  synthetic operator, terminal state, scenario code, oracle version, and reveal
  time in a truth-free structured audit log.
- Observable scenarios and `scenario-oracle/v1` are separate resources. Only
  the post-decision reveal service loads the oracle; evidence, generation,
  retrieval evaluation, and corpus generation consume observable fields.
- A report should be approved only when its probable cause, disposition,
  confidence, cited evidence signature, and safe recommendation satisfy the
  answer key. Otherwise it should be rejected. The rule makes the expected
  human decision deterministic without allowing the generator to approve a
  report itself.

## Acceptance-test map

| Behavior | Automated coverage |
|---|---|
| Broad, reviewed observable scenario catalog | `ClasspathScenarioCatalogTest` |
| Complete separately loaded oracle | `ClasspathScenarioOracleCatalogTest` |
| Weighted common/uncommon/rare selection | `WeightedScenarioSelectorTest` |
| Opaque, unique, restart-safe references | `AlertReferenceCodecTest` |
| Sparse alert payload with no leaked truth | `IncidentGenerationServiceTest` |
| Exact intake HTTP contract and tenant header | `CopilotAlertHttpClientTest` |
| Terminal decision required before answer-key reveal | `AnswerKeyRevealServiceTest` |
| Tenant-scoped authoritative incident lookup | `CopilotIncidentReviewHttpClientTest` |
| Reveal HTTP and safe error contracts | `IncidentGenerationControllerTest` |
| Deterministic time-aligned MCP evidence | `RecentServiceErrorsToolTest` |
| Live MCP v1 discovery and invocation | `GeneratorMcpContractTest` |
| Clearly separate red-button UI | `StaticUiContractTest` |
| Deterministic comparison rubric and text-only judge | `ComparisonRubricTest`, `ComparisonServiceTest` |
| Exact tenant-scoped decision/report/evidence binding | `FrozenOutcomeHttpClientTest` |
| Bounded Ollama output, timeout and prompt isolation | `OllamaTextJudgeTest` |
| Comparison request IDs and no client-supplied answers | `ComparisonControllerTest` |
| Automatic comparison, independent cards, retry and stale results | `ComparisonUiBehaviorTest` (seven Node UI cases) |

## Automatic post-reveal comparison

Revealing a terminal incident's answer key automatically starts a comparison
underneath it. Four 0-100 gauges compare disposition, confidence, root cause and
recommendation. Only cause/recommendation text is scored by an independently
prompted local LLM; disposition and confidence are exact 0/100 checks.
INSUFFICIENT_EVIDENCE null assertions are scored directly without a model call.

The report card shows the rounded equal-weight average: green Good >=80,
yellow OK 50-79, red Bad <50. The separate human-decision card compares the
recorded outcome with the deterministic rubric: expected APPROVED requires
exact disposition/confidence matches and both text scores >=80; otherwise
expected REJECTED. Correctly rejecting a poor report therefore earns a good
decision score. This post-decision advisory rubric does not amend the recorded
decision or the offline evaluation benchmark. It is a bounded comparison of
four fields, not a complete proof of the decision rule's evidence semantics.

The UI calls `POST /api/generations/{incidentId}/comparison` with
`X-Synthetic-Operator-Id` and no body after reveal. The endpoint repeats the
authoritative terminal gate and selects the exact report referenced by the final
decision through tenant-scoped GET requests. Model failures retry once, then
display Not scored; a UI retry starts a new retained attempt.

Configuration is environment-only:

```text
OLLAMA_BASE_URL=http://localhost:11434
COMPARISON_CHAT_MODEL=qwen3:8b-q4_K_M
COMPARISON_TIMEOUT=2m
COMPARISON_ARTIFACT_DIRECTORY=./tmp/comparisons
```

The judge defaults to REPORT_CHAT_MODEL when set, otherwise Qwen above; no
model downloads occur. Text scores are AI-assessed and can be incorrect or
variable. Use a separately installed judge model when desired. Each unique,
tenant-scoped local JSON artifact retains full inputs, prompt, model responses,
IDs, versions, timestamps and hashes. Back up this ignored directory if retained
demo comparisons must survive local cleanup. Artifact write failure prevents
returning a successful comparison. See
[ADR-0021](../docs/agent/decisions/ADR-0021-post-decision-text-comparison.md).

The standalone generator test suite now also requires Node.js (repository-pinned
version) for its deterministic UI behavior harness; it requires no npm install
or live model provider.

## Integration shape

The generator defaults to `http://localhost:8080` for alert intake and serves
its UI and MCP endpoint on port `8082`. Point the copilot API at this standalone
evidence source before starting it:

```powershell
$env:OPERATIONS_MCP_BASE_URL='http://localhost:8082'
```

Configuration is environment-only and contains no secrets:

```text
COPILOT_API_BASE_URL=http://localhost:8080
SYNTHETIC_TENANT_ID=8b860d80-d17f-4e6b-8c48-af35f26a4d61
```

## One-click Windows startup

Double-click `start-local.bat` in the repository root. The launcher:

1. reuses the generator if it is already healthy;
2. otherwise starts it in a separate PowerShell window;
3. waits up to 60 seconds for the health endpoint;
4. configures and starts the copilot API against the generator MCP endpoint,
   verifying the endpoint before reusing an already-running API;
5. starts the operator console; and
6. opens `http://localhost:8082` in the default browser.

The database must already be running. The launcher checks reachability and
starts the API; it does not provision PostgreSQL. For prerequisites and first-run
knowledge preparation, follow the [root README](../README.md).

Build and test this system independently from the repository root:

```powershell
./mvnw.cmd -f syntheticIncidentGenerator/pom.xml clean verify
./mvnw.cmd -f syntheticIncidentGenerator/pom.xml spring-boot:run
```

Then open `http://localhost:8082` and use the red button. The generated alert
will appear in the copilot's Active work queue when the copilot API and its
PostgreSQL database are running.

## Deliberate compatibility limit

The current copilot investigates one incident family and retrieves one evidence
domain. This generator therefore creates many real-world causes of an
authorization-decline-rate spike and expresses their observable signature
through the existing service-error contract. The corpus maps all 36 scenarios,
but authored coverage does not guarantee successful retrieval or sufficient
observed evidence. Partial and unavailable evidence can still require
`INSUFFICIENT_EVIDENCE`; see [corpus results](../SynTen%20Inc/README.md).

## Additional incident families

The original 36 authorization-decline fixtures and sealed oracle retain their
exact bytes. Maven additionally packages
[SynTen multi-incidents v1](../SynTen%20Inc/multi-incidents/v1/README.md): complete
and degraded scenarios for authorization timeouts, capture/refund failures,
settlement delays, webhook delivery failures and reconciliation mismatches.
The same random button selects from the composed catalog. Only generation sends
the explicit incidentType; MCP remains the immutable aggregate-service-errors
v1 contract. Run `./start-local.bat -PrepareKnowledge` from the repository root
and restart old API/generator processes to use the new sources and implementation.
