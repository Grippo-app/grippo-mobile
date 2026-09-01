# Validator — `backend-contract-drift`

A validator (`backend-contract-drift`, validation-gates skill) that surfaces divergence between the
committed contract snapshot and the data layer — **as suggestions, never auto-fix**. This is the
**authoritative reference** the validator cites; it operationalizes the named anti-pattern "silent API
contract drift (backend changed, consumers didn't)".

## Gate ladder (NORMATIVE)

1. `backendContractEnabled: false` → emit no findings, report `SKIPPED`.
2. `auto` and `contract:paths` reports `present:false` → report
   `SKIPPED (no snapshot)`. No hard fail — the greenfield path.
3. Otherwise (`auto` with a snapshot present, or `true`) → run. Under `true` the validator **must**
   run: a missing snapshot is itself an `ERROR` (pull one), not a skip.

## Mechanical core (NORMATIVE)

```bash
cd orchestrator/api-contract && npm run contract:diff   # → the current control/task report scope's drift.json
```

`diff.mjs` pairs each DTO class with its snapshot schema by **schema name** — the authoritative,
global match (the DTO's `dto/<area>/` folder is only a tie-breaker, not a join key:
`endpoint-inventory.md` explains why the folder↔area alignment is unreliable across real projects).
It then diffs the `@SerialName` field sets and declared Kotlin types against the matched slice schema,
plus `<Product>Api` paths/methods against the inventory. Pure local JSON + Kotlin source — it never
touches the network (golden invariant).

## Judgement layer (what the validator adds on top of the report) (NORMATIVE)

The script is mechanical; the validator corroborates and extends it where syntax alone is ambiguous:

- **Mapper guards vs `required`/`nullable_observed`.** Each
  `AppLogger.Mapping.log(field) { … } ?: return null` guard in the dto-to-entity/domain mappers
  (`../../mappers/references/null-safety-and-logging.md`) is checked against the slice: a guard missing on a field the server
  may omit (nullable declared or observed), or an entity hard-requiring a field that is observed null,
  is drift even when the DTO itself matches.
- **Domain enum dictionaries vs `enum`/`enum_observed`.** A server value (declared or observed) absent
  from the mobile dictionary is the cross-repo "shared dictionaries synced manually" rule about to
  break — flag it before a payload carries the new value.
- **`<Product>Api` paths/methods vs the inventory.** A method whose `path`/`HttpMethod` no longer
  matches any inventory endpoint (`../../data-layer/references/dtos-and-api.md` fixes the flat
  one-method-per-endpoint shape, so this pairing is deterministic).

## Report — scoped `drift.json`

```jsonc
// control: .cache/api-contract/reports/drift.json
// task:    .cache/api-contract/reports/executions/<worktreeId>/<runId>/drift.json
{
  "schemaVersion": 1,
  "checkedAt": ISO,
  "specHash": "sha256:…" | null,
  "committedGenerationId": "gen-…",
  "contractHash": "sha256:…",
  "environmentId": str,
  "projectCodeRevision": "sha256:…",
  "analyzerVersion": str,
  "limitations": [str],
  "summary": { "errors": int, "warnings": int, "infos": int },
  "findings": [{
    "severity": "ERROR|WARNING|INFO",
    "kind": "dto-field-unknown|server-field-missing-in-dto|type-mismatch|endpoint-missing-server-side|enum-new-value|nullability-mismatch|info",
    "area": str | null, "schemaRef": str | null,
    "operationId": str | null, "field": str | null,
    "dtoFile": str | null, "message": str, "suggestion": str | null
  }]
}
```

Written to the gitignored control report directory when invoked outside a task. A manager-issued
task execution writes to its exact `executions/<worktreeId>/<runId>/` child so concurrent runs cannot
replace each other's evidence. Task-scoped consumers must resolve that namespace from the manager
binding; a raw environment path is not authority. The API workbench reads the control projection
through `GET /api/api/changes`; the Changes tab renders the findings and the Diagnostics tab owns the
explicit control refresh actions.

## Finding kinds + severities (NORMATIVE)

The canonical `kind` values (above) are closed — new categories require updating this reference and
`diff.mjs` together. Typical severity mapping:

- `ERROR` — the client mis-reads the server or calls a dead endpoint:
  `endpoint-missing-server-side` (a `<Product>Api` method whose path/method vanished from the spec),
  `type-mismatch`, `dto-field-unknown` (a DTO `@SerialName` the schema no longer carries — usually a
  server-side rename/removal; the all-nullable DTO will not crash, it will silently read `null`, which
  is exactly the drift this gate exists to catch).
- `WARNING` — divergence that loses data or precision but does not break:
  `server-field-missing-in-dto` (new server field the DTO drops on the floor), `enum-new-value`,
  `nullability-mismatch` when the mapper guard set disagrees with required/observed reality (the
  validator's judgement layer).
- `INFO` — deliberate omissions and notes (`info`), e.g. a server field the project chose not to
  consume; `nullability-mismatch` as the slice-internal declared-vs-observed note (the spec lies,
  the defensive all-nullable DTO is justified — no mapper involved, nothing to fix).

Severity is per finding, not per kind — the validator may downgrade (e.g. a `deprecated` endpoint
still present) or upgrade with stated reasoning, but never invent kinds.

**Observation-gated kinds (dormant on an OpenAPI-only snapshot).** The two *slice-internal* checks
`diff.mjs` emits — `enum-new-value` and the slice-internal `nullability-mismatch` — read only the
Postman-derived `enum_observed` / `nullable_observed` columns (`postman.md`). On a snapshot that has
never been enriched by a Postman collection those columns are `null` (never observed), so **these two
kinds produce zero findings**: an OpenAPI-only run reports *structural* drift only
(`dto-field-unknown`, `type-mismatch`, `server-field-missing-in-dto`, `endpoint-missing-server-side`).
Refreshing an OpenAPI environment with configured Postman enrichment is what activates the enum/nullability reality
checks. (The validator's *judgement-layer* `nullability-mismatch` — mapper guards vs required/observed
— is a separate, agent-emitted finding and is likewise only as good as the observed data it has.)

## Suggestion-only doctrine (MUST)

Every finding should carry a `suggestion` that is a **precise suggested Kotlin diff** — e.g. add
`@SerialName("priority") val priority: String? = null` to `NotificationResponse`, or update the
`getNotifications` path `/notifications` → `/v2/notifications` — and it is **never auto-applied**
(ADR-2: no codegen). The defensive DTO doctrine — every field nullable + `= null` + `@SerialName`
(`../../data-layer/references/dtos-and-api.md`) — is untouched: the validator suggests, a human/builder
applies.

## Drift → backlog (NORMATIVE)

The Site projects current drift findings as mismatch sources in the API
workbench. A user may select at most 25 current change/mismatch source ids and
preview or create focused follow-up work. The server re-resolves every source,
generation/report/task revision, task body, provenance, fingerprint and dedup
decision before it reserves `TASK_<N>` and publishes through the canonical
backlog owner. There is no report-wide Drift tab or one-task-per-report action,
and neither this skill nor `contract:diff` writes task files directly.

## Coverage → backlog (suggestion-only, NOT a gate) (NORMATIVE)

Drift is client-anchored: it audits the endpoints the client *already* references and stays silent
about endpoints in the snapshot the client has not built. The **coverage planner**
(`scripts/suggest-endpoint-tasks.mjs`, npm `contract:suggest`) answers the reverse question. It walks
the generation-aware inventory returned by `contract:paths`, reconciles each endpoint against recognized
client calls (the request wrapper or a supported direct Ktor client call) and the `drift.json` from the
same report scope (by area), and writes a
PLAN — `suggested-endpoints.json` beside that drift report — classifying every endpoint:

- `available-to-build` — no recognized client call → an optional **implementation** task
  (route `endpoint-builder`).
- `drift` — implemented, but its area carries an ERROR/WARNING drift finding → an **actualize** task.
- `implemented` — a call is observed and a current exact drift report is clean → no task.
- `observed-call` — a call is observed, but drift has not been checked against the same generation and project revision → no correctness claim and no task.

Coverage matching is **method + path-shape only**. A matching call proves only that the endpoint was
observed. It becomes `implemented` only when a complete drift report is current for the same generation,
spec, analyzer and project revision and carries no ERROR/WARNING for its area. A capped or stale drift
report leaves otherwise clean calls as `observed-call`; DTO correctness is never inferred from the call.

This is a build menu, **not a gate, and not the rejected `backend-contract-coverage` validator**
(`secrets-and-ci.md`): an available-to-build endpoint is **never a defect**. The mobile client consumes a
*subset* of the backend by design (ADR-8) — the planner only surfaces what is available to build and
the human chooses; it flags nothing, fails nothing, and creates nothing on its own. Like the drift
report it is suggestion-only and never calls the backend. The bounded JSON plan is a CLI artifact under
the current report scope; the Site does not project it or provide task buttons. Refresh it with
`contract:suggest` after `contract:diff` when it should reflect the latest complete drift report.

## Not in scope (MUST)

No auto-editing of DTOs, mappers, or enum dictionaries. No live API probing — the validator reads
committed JSON + Kotlin source only; refreshing the snapshot is a separate typed probe + guarded refresh act
(`snapshot-pipeline.md`). Coverage is **suggestion-only** (above): it proposes implementation tasks for
not-yet-built endpoints but never gates on them — a coverage *validator* stays rejected
(`secrets-and-ci.md`, ADR-8).
