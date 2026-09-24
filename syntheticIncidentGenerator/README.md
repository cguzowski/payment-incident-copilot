# Synthetic Incident Generator

This directory is an independently runnable synthetic test system. It is not a
module of the root Maven reactor and it does not modify or share production code
with the payment incident copilot.

## Behavioral contract

- A deliberately red button selects one weighted scenario from a reviewed
  catalog of common, uncommon, and rare payment-authorization incidents.
- The generator sends only the existing alert-system payload to the copilot
  intake API: opaque external alert ID, severity, detected time, title, and
  description. Tenant context is carried in the existing synthetic header.
- The copilot API owns the database insert and idempotency behavior; the
  generator never writes into another system's tables.
- The opaque alert ID lets this service reconstruct the selected scenario and
  deterministic evidence through the existing read-only
  `getRecentServiceErrors` MCP contract.
- Alert intake and MCP evidence omit dedicated answer-key fields. The
  generation response nevertheless sends `answerKey` to the browser immediately;
  the UI only collapses it. This is not a protected or auditable reveal boundary.
  The corpus also contains oracle-derived causes; see
  [evaluation limitations](../docs/agent/STATUS.md).
- A report should be approved only when its probable cause, disposition,
  confidence, cited evidence signature, and safe recommendation satisfy the
  answer key. Otherwise it should be rejected. The rule makes the expected
  human decision deterministic without allowing the generator to approve a
  report itself.

## Acceptance-test map

| Behavior | Automated coverage |
|---|---|
| Broad, reviewed scenario catalog | `ClasspathScenarioCatalogTest` |
| Weighted common/uncommon/rare selection | `WeightedScenarioSelectorTest` |
| Opaque, unique, restart-safe references | `AlertReferenceCodecTest` |
| Sparse alert payload with no leaked truth | `IncidentGenerationServiceTest` |
| Exact intake HTTP contract and tenant header | `CopilotAlertHttpClientTest` |
| Deterministic time-aligned MCP evidence | `RecentServiceErrorsToolTest` |
| Live MCP v1 discovery and invocation | `GeneratorMcpContractTest` |
| Clearly separate red-button UI | `StaticUiContractTest` |

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
4. configures and starts the copilot API against the generator MCP endpoint;
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
