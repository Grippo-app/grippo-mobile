---
name: backend-contract-client
description: >-
  Use the real backend contract — the committed OpenAPI/Swagger snapshot,
  optional Postman enrichment, the endpoint inventory + per-area field slices —
  when building or auditing DTOs, endpoints, mappers, or data features so the
  client matches the server instead of guessing. Triggers on "backend contract",
  "OpenAPI", "Swagger", "Postman", "API snapshot", "endpoint inventory",
  "contract drift", "REST/GraphQL/gRPC API shape", "contract:probe / refresh /
  verify / diff / suggest", "silent API contract drift". Server owns the
  contract; the snapshot is the source of truth; drift is suggestion-only.
---

# backend-contract-client

Operational entrypoint for the backend API-contract sidecar
(`orchestrator/api-contract/`). Self-contained — carries its own rules and reads
no external rule docs at runtime. Gated by tri-state
`backendContractEnabled` (default `auto`) in `orchestrator/project-config.md`.

## When to use

- Building or actualizing a DTO / endpoint / mapper / data feature that talks to
  the backend — read the real contract first, do not invent fields or paths.
- Auditing the data layer for divergence between the committed snapshot and the
  Kotlin DTOs / `<Product>Api` (contract drift).
- Testing or refreshing the source (`contract:probe` / `contract:refresh-openapi` /
  `contract:refresh-postman`) or planning
  endpoint coverage (`contract:suggest`).

Do NOT use for design tokens / screens (that is `implement-figma`) or for pure
mapper null-safety mechanics absent a contract question (that is `mappers`).

## Required inputs

- `backendContractEnabled` value (`auto` | `true` | `false`) from project config.
- The validated current generation when present: its compact inventory + the
  **one** area slice for the schemas touched. Resolve their exact paths with
  `cd orchestrator/api-contract && npm run --silent contract:paths`; never
  guess a generation id, bypass the pointer, or load the normalized spec into
  agent context.
- For an endpoint build: the target path + HTTP method (or operationId).

## Workflow

1. **Server owns the contract; the snapshot is the source of truth.** OpenAPI is
   the authoritative structure; Postman only *enriches* it with observed reality
   (`nullable_observed`, `enum_observed`, examples) or *bootstraps* when no spec
   exists. Read the inventory to locate, read one area slice for fields — taken
   verbatim, zero invented fields.
2. **Use the typed sidecar; never let the browser or site server fetch.** Resolve
   sources only from `environments.json` and credentials only through `authRef`.
   - `contract:probe` — read-only fetch, validation, normalization, fingerprint and preview report.
   - `contract:refresh-openapi` — probe, re-fetch, revision fences, writer lease, atomic generation publication.
   - `contract:refresh-postman` — explicit Advanced Postman-only bootstrap through the same fences.
   - `contract:paths` — read-only validated locator for the current inventory, areas directory, and spec artifact.
   - `normalize.mjs` / `import-postman.mjs` — internal staging transformers; never invoke them as workspace writers.
   - `contract:verify` — re-validate committed manifests against `contract-schemas/`.
   - `contract:diff` — mechanical drift core; run `contract:suggest` after when you need the coverage planner.
   - `contract:suggest` — endpoint-coverage planner (build menu, not a gate).
3. **Drift is suggestion-only unless the task authorizes a fix.** Findings carry a
   precise suggested Kotlin diff and are **never auto-applied** (ADR-2: no codegen).
   A fix is a separate backlog task created through the deterministic server
   endpoint — do not edit DTOs/mappers/enums in a drift-audit footprint. Its
   asynchronous shallow AI intake is advisory only and cannot apply the fix.
4. **`verify` must not be green-by-skip for endpoint work.** For endpoint work a
   committed snapshot is required; an empty snapshot under `auto` reports
   `SKIPPED (no snapshot)` and that is **not** acceptable as a pass for endpoint
   builds — publish one through Backend Test + Refresh first. Never replace it
   with an endpoint shape copied from task text.
5. **No codegen unless explicitly authorized.** The snapshot *informs* the
   hand-written all-nullable DTOs; it does not generate them. The defensive DTO
   doctrine (every field nullable + `= null` + `@SerialName`) is untouched.

## Stop and ask

- `backendContractEnabled` is `auto` or `true` and the snapshot or endpoint is
  **absent** → report `BLOCKED` for endpoint/DTO work (do not guess the shape or
  use task text as a substitute). Under `true`, a missing snapshot is also a
  validator `ERROR`; under `auto`, unrelated greenfield work may continue.
- A drift fix would extend an enum dictionary or change a shared DTO
  (mobile ↔ backend ↔ admin) → stop; name the consumers, do not silently sync.
- A breaking contract change (path/method/required-field removed server-side) is
  observed → surface it, list who breaks, do not auto-edit.
- An unmatched Postman request appears against an existing spec → likely a stale
  snapshot; re-pull rather than extending structure from Postman.

## References to read

Self-contained reference pack — read only what the task needs. Start at
`references/index.md` (routing table + sidecar command surface + owned agents):

- `references/index.md` — topic → file routing, command surface, owned agents.
- `references/overview.md` — gate semantics (tri-state), goal, golden invariant,
  OpenAPI-primary / Postman-enrichment, config fields.
- `references/snapshot-pipeline.md` — probe → preview → refresh, TOCTOU fences,
  generation publication, merge stickiness, staleness, commit policy.
- `references/endpoint-inventory.md` — canonical snapshot shapes + how each
  consumer reads them; area derivation; operationId synthesis; context economy.
- `references/drift.md` — the authoritative drift reference (gate ladder, finding
  kinds/severities, suggestion-only doctrine, coverage planner).
- `references/postman.md` — enrichment / bootstrap / unmatched-request policy /
  deferred MCP layer.
- `references/tooling-sidecar.md` — sidecar layout, npm scripts, `contract-schemas/`
  naming, secrets, dependency policy. (`orchestrator/api-contract/README.md` is the
  on-disk quickstart.)
- `references/secrets-and-ci.md` — `.secrets/` discipline; deferred
  CI / coverage-validator / scheduled drift.
- `references/data-services-backend.md` — the `:data-services:backend` module
  (`<Product>Api`, `BackendClient`, `TokenProvider`, all-nullable DTO doctrine).

## Validators / gates

- **`backend-contract-drift`** (validator, `Read, Bash, Grep, Glob`, sonnet) —
  runs `contract:diff`, then corroborates mapper guards vs `required`/observed and
  enum dictionaries vs `enum`/`enum_observed`; suggestion-only. Gate ladder:
  `false` → `SKIPPED`; `auto` + no snapshot → `SKIPPED (no snapshot)`; otherwise
  run. Contract: `contracts/agents/backend-contract-drift.md`.
- **data-layer validators** where the change touches DTOs / repositories / DAOs.
- **build** — the standard android + iOS assemble gate.

See `references/index.md` (command surface) and `references/drift.md` (finding
kinds + severities) for the full surface.

## Output contract

- An endpoint/DTO build emits a builder report per
  `orchestrator/contracts/builder-report.md`; its plan follows
  `orchestrator/contracts/planner-output.md`.
- The `endpoint-builder` agent (`Read, Edit, Write, Bash, Grep, Glob`, sonnet) is
  pinned by `contracts/agents/endpoint-builder.md` — preserve its role,
  BLOCKED message, required reads, and footprint verbatim.
- Drift output is `.cache/api-contract/reports/drift.json`; coverage output is
  `.cache/api-contract/reports/suggested-endpoints.json` (both gitignored). The
  site creates a fix or implementation task through its idempotent deterministic
  backlog endpoint — the contract skill never chooses a number or writes a task
  file directly. Shallow AI intake runs separately and cannot block creation.
