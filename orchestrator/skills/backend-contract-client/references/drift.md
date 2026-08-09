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
cd orchestrator/api-contract && npm run contract:diff   # → .cache/api-contract/reports/drift.json
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

## Report — `.cache/api-contract/reports/drift.json`

```jsonc
// .cache/api-contract/reports/drift.json
{ "schemaVersion":1, "checkedAt":ISO, "specHash":str|null, "summary":{errors,warnings,infos}, "findings":[ { "severity":"ERROR|WARNING|INFO", "kind":"dto-field-unknown|server-field-missing-in-dto|type-mismatch|endpoint-missing-server-side|enum-new-value|nullability-mismatch|info", "area":str|null, "schemaRef":str|null, "operationId":str|null, "field":str|null, "dtoFile":str|null, "message":str, "suggestion":str|null } ] }
```

Written to `orchestrator/.cache/api-contract/reports/` (gitignored contents); the current API
workbench reads it through `GET /api/api/changes`. The Changes tab renders the findings and the
Diagnostics tab owns the explicit refresh actions.

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

A drift finding can spawn follow-up work through the site's deterministic,
idempotent backlog endpoint. The server — not this skill or a Claude session —
reserves `TASK_<N>`, publishes the Markdown and regenerates `INDEX.json`; do not
write task files directly. The new task is tagged with its drift origin (kind +
operationId/schemaRef), so the fix traces back to the finding. Its shallow AI
intake runs afterwards as advisory triage and can never apply the suggestion or
make the task runnable.

The site offers the same path with one click: the API panel's Drift tab renders a **"Create fix task"**
button next to the report summary. It builds a backlog task from the report (ERROR/WARNING findings
verbatim with their suggestions, INFO collapsed to per-kind counts,
`.cache/api-contract/reports/drift.json` cited as the primary Input) and submits
it through the same deterministic endpoint with a stable idempotency key — one
task per report (re-enabled when a newer report lands).

## Coverage → backlog (suggestion-only, NOT a gate) (NORMATIVE)

Drift is client-anchored: it audits the endpoints the client *already* references and stays silent
about endpoints in the snapshot the client has not built. The **coverage planner**
(`scripts/suggest-endpoint-tasks.mjs`, npm `contract:suggest`) answers the reverse question. It walks
the generation-aware inventory returned by `contract:paths`, reconciles each endpoint against the client's
`<Product>Api.request()` calls and the latest `.cache/api-contract/reports/drift.json` (by area), and
writes a PLAN — `.cache/api-contract/reports/suggested-endpoints.json` — classifying every endpoint:

- `not-implemented` — in the snapshot, no client call → a ready-to-create **implementation** task
  (route `endpoint-builder`).
- `drift` — implemented, but its area carries an ERROR/WARNING drift finding → an **actualize** task.
- `implemented` — implemented + clean → no task.

Coverage matching is **method + path-shape only**: an endpoint counts as `implemented` the moment a
`<Product>Api.request()` call shares its HTTP method and path template — the planner does **not** check
that the call's DTOs, fields, or types are *correct* (that is drift's job, surfaced separately via the
area's findings). So `implemented` means "a call exists", not "the implementation is right". This is by
design — coverage is a **menu of what is available to build, not a correctness signal**.

This is a build menu, **not a gate, and not the rejected `backend-contract-coverage` validator**
(`secrets-and-ci.md`): a not-implemented endpoint is **never a defect**. The mobile client consumes a
*subset* of the backend by design (ADR-8) — the planner only surfaces what is available to build and
the human chooses; it flags nothing, fails nothing, and creates nothing on its own. Like the drift
report it is suggestion-only and never calls the backend. The site delivers it on the API panel's
**Coverage** tab: per-endpoint **"Create implementation task"** / **"Create actualize task"** buttons,
each submitting through the same deterministic backlog endpoint with a stable
per-suggestion idempotency key. The same per-endpoint state also badges
each row of the **Endpoints** tab as a read-only indicator (the actionable buttons stay on Coverage).
The plan is refreshed by `contract:suggest`; run it after `contract:diff` when coverage should reflect
the latest drift report.

## Not in scope (MUST)

No auto-editing of DTOs, mappers, or enum dictionaries. No live API probing — the validator reads
committed JSON + Kotlin source only; refreshing the snapshot is a separate typed probe + guarded refresh act
(`snapshot-pipeline.md`). Coverage is **suggestion-only** (above): it proposes implementation tasks for
not-yet-built endpoints but never gates on them — a coverage *validator* stays rejected
(`secrets-and-ci.md`, ADR-8).
