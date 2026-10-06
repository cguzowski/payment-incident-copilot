# Independent runtime systems

`start-local.bat` exports investigation (API and console), source (generator/MCP),
and evaluator into `.migration-workspaces/independent-v1/`. Each exported system
owns its Maven wrapper, POM, runtime resources and configuration; no sibling
checkout is needed to build it. Investigation also includes its Angular console.

The exporter verifies hash-pinned source allowlists and the public contract release.
Only runtime build inputs are exported. Tests and development archives are omitted.
Database settings belong to investigation; judge settings belong to evaluator.
Browser reveal/scoring calls evaluator directly. CORS is not authentication.

To export a system into a new directory:

```powershell
./scripts/independent/export.ps1 -Application investigation -Destination NEW_INVESTIGATION_PATH
./scripts/independent/export.ps1 -Application source -Destination NEW_GENERATOR_PATH
./scripts/independent/export.ps1 -Application evaluator -Destination NEW_EVALUATOR_PATH
```

From an exported directory, build with `./mvnw.cmd -Dmaven.test.skip=true package`.
The investigation console uses `npm ci`, `npm run build` and `npm start` from
`frontend/operator-console`. Configure each system's environment explicitly when
running its packaged JAR independently. See the root README for the local launcher.

Normal startup uses each system's native Maven `spring-boot:run` and console
`npm start` in separate visible terminals. Closing one stops its service;
repeated startup reuses matching services. `--CheckOnly` opens no terminals.
Generated workspaces and dependencies are disposable once their services stop.
