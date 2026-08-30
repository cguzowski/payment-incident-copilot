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
- Ground truth is never sent in the alert or MCP evidence. The generator UI
  keeps a separate answer key that the reviewer may reveal after reviewing the
  proposed report.
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

Double-click `start-local.bat` in the repository root. After starting the
operations MCP server, copilot API, and operator console, the launcher:

1. reuses the generator if it is already healthy;
2. otherwise starts it in a separate PowerShell window;
3. waits up to 60 seconds for the health endpoint; and
4. opens `http://localhost:8082` in the default browser.

The root launcher ensures that the copilot API and PostgreSQL are available so
the red button can add an incident to the Active work queue.

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
through the existing service-error contract. Some uncommon and rare signatures
are intentionally beyond the current approved runbook; a careful report may
correctly return `INSUFFICIENT_EVIDENCE` rather than inventing certainty.
