# Runtime architecture

The default Windows launcher exports three hash-pinned standalone applications
under `.migration-workspaces/independent-v1/` using the current source allowlists.
Each has its own Maven wrapper, POM and resources. The investigation export also
owns the Angular console. Source inputs are stored once in this repository.

- Investigation API: port 8080; tenant-scoped intake, workflow, MCP evidence,
  PostgreSQL/Flyway, PDF retrieval, Ollama reports, human decisions and audit.
- Console: port 4200; API proxy and explicit operator workflow.
- Source: port 8082; synthetic scenarios, red-button alert generation, MCP evidence
  and generator dashboard. Browser evaluation requests go directly to evaluator.
- Evaluator: loopback port 8083; gated answer reveal, advisory scoring, judge calls,
  local comparison artifacts and GET-only decision-bound investigation reads.

Startup allowlists isolate database settings to investigation and judge settings to
evaluator. Java uses native Maven spring-boot:run, console uses npm start; each
runs in an owning visible terminal. Runtime identity/hash checks protect reuse.
The catalog importer validates the PDF/source hashes and provenance before import.
Historical PDFs are packaged for existing citation routes.

Synthetic caller-supplied identity headers and CORS are not authentication.
There is no production deployment or money movement. Models and database volumes
are separately installed local infrastructure, outside the source checkout.
