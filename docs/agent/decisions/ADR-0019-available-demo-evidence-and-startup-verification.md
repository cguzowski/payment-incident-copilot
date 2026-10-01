# ADR-0019: Available demo evidence and verified local provider configuration

Status: Accepted under explicit owner request
Date: 2026-10-01
Decision owner: Christopher Guzowski

## Context

The owner reported recurring unavailable service-error evidence and explicitly
selected always-available evidence for normally generated demo incidents.
The random selector previously included deliberate degraded fixtures. The local
launcher also reused any healthy API regardless of its MCP provider, and its
PowerShell generator mode could start the legacy provider when the generator
was absent.

## Decision

Filter normal weighted selection to AVAILABLE scenarios before applying the
existing 70/25/5 rarity distribution. Preserve every immutable scenario/oracle,
explicit degraded MCP outcome, historical attempt, and evaluation workflow.
All seven incident families remain selectable. An empty eligible rarity bucket
fails startup rather than substituting fabricated evidence.

Start the selected MCP deployable before the API. Verify both newly started and
reused APIs via a sanitized operationsMcp.baseUrl entry on the existing actuator
info endpoint. Derive it from the same effective Spring property used by the
MCP client; omit user info, query and fragment. Mismatched or unverifiable
configuration fails startup with instructions to stop and restart the existing
API. The launcher does not terminate unrelated processes.

## Consequences

Normal demo generation no longer samples simulated evidence degradation.
Explicit evaluation scenarios still exercise insufficient evidence and real
transport failures remain visible. Older running APIs must be restarted once
with the updated build before the launcher can verify and reuse them. HTTP
health and configuration verification do not prove every future request succeeds.
