# backend-contract-client — references routing

Self-contained reference pack. These files carry the skill's own rules and read no external rule docs at runtime. Read
only what the task needs.

## Topic → file

| You are doing… | Read |
|---|---|
| Understanding the integration: goal, tri-state gate, golden invariant, OpenAPI-primary vs Postman-enrichment, config fields | `overview.md` |
| Working with the `orchestrator/api-contract/` sidecar: layout, npm scripts, `contract-schemas/`-never-`schemas/` naming, install/run, dependency policy, `.gitignore` | `tooling-sidecar.md` |
| Testing / refreshing the snapshot: typed probe, preview TTL, source/auth/snapshot fences, writer lease, generation pointer and commit policy | `snapshot-pipeline.md` |
| Reading the snapshot: resolver output, inventory + area artifact shapes, area derivation, operationId synthesis, per-consumer protocol, context economy | `endpoint-inventory.md` |
| Auditing for drift: gate ladder, mechanical core, judgement layer, `drift.json` shape, finding kinds + severities, suggestion-only doctrine, drift→backlog, coverage→backlog | `drift.md` |
| Postman enrichment / bootstrap: ingestion, "spec lies" payoff, bootstrap mode, unmatched-request policy, limitations, deferred MCP layer | `postman.md` |
| Secret handling + deferred CI: `.secrets/<environment-id>.token`, public redaction and future CI | `secrets-and-ci.md` |
| The `:data-services:backend` module: `<Product>Api`, `BackendClient`, `TokenProvider`, DTO package + all-nullable doctrine, build block | `data-services-backend.md` |

## Sidecar command surface (quick map)

Plain Node with `ajv` and `yaml`. Live network fetch happens only inside the
exact typed `backend-action.mjs` child. The site server and the
diff/suggest/verify scripts read local JSON only.

| `npm run` | Script | Network? | Does |
|---|---|---|---|
| `contract:probe` | `backend-cli.mjs` | typed sidecar | Read-only validate/fingerprint/diff for the manifest default environment. |
| `contract:refresh-openapi` | `backend-cli.mjs` | typed sidecar | Probe then publish one fenced OpenAPI generation. |
| `contract:refresh-postman` | `backend-cli.mjs` | typed sidecar | Explicit Postman-only bootstrap generation. |
| `contract:paths` | `resolve-current.mjs` | no | Emit validated project-relative paths for the current inventory, areas directory and spec. |
| `contract:verify` | `verify.mjs` | no | Re-validate committed manifests against `contract-schemas/`. **Not green-by-skip for endpoint work.** |
| `contract:diff` | `diff.mjs` | no | Mechanical drift core → `drift.json`; pairs DTO↔schema by name. Run `contract:suggest` separately when coverage planning needs a refresh. |
| `contract:suggest` | `suggest-endpoint-tasks.mjs` | no | Coverage planner → `suggested-endpoints.json`. Suggestion-only, NOT a gate (ADR-8). |
| `contract:doctor` | `doctor.mjs` | no | Snapshot health; warns when older than 14 days. |

Site read endpoints (local JSON only — never the backend): `GET /api/api/overview` ·
`/api/api/endpoints` · `/api/api/changes` · `/api/api/diagnostics`.

## Agents owned

| Agent | Kind | Tools | Model | Contract |
|---|---|---|---|---|
| `endpoint-builder` | builder | `Read, Edit, Write, Bash, Grep, Glob` | sonnet | `contracts/agents/endpoint-builder.md` |
| `backend-contract-drift` | validator | `Read, Bash, Grep, Glob` | sonnet | `contracts/agents/backend-contract-drift.md` |

Source of truth: the `endpoint-builder` capability (data-layer skill),
the `backend-contract-drift` validator (validation-gates skill). `endpoint-builder` is shared with the `data-layer`
skill.
