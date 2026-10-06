# Runtime constraints

- Synthetic data only. No payment processing or automated operational actions.
- Reports are advisory. Human approval/rejection requires an explicit reason.
- Keep evidence citations, missing/contradictory evidence and model/prompt provenance.
- Carry tenant identity through persistence and retrieval.
- Java 21, Spring Boot/Spring AI, Angular, PostgreSQL/pgvector and local Ollama.
- Investigation, generator and evaluator export/build independently.
- Keep PDF sources/manifests required for hash-validated catalog import, and older
  PDFs needed for persisted citations. Do not alter frozen runtime asset bytes.
- Do not track credentials, installed dependencies, generated builds/workspaces,
  model weights, database state, local logs or comparison output.
- Owner-authorized repository cleanup removes tests and historical development
  records. Application behavior, scoring, prompts and persisted database history
  remain unchanged.
