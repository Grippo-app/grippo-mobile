# Endpoint inventory + area slices — the snapshot the agents read

When no snapshot exists, endpoint/DTO consumers stop rather than infer a server
shape from task text. This is the canonical contract for the two committed artifacts — every writer (`normalize.mjs`,
`import-postman.mjs`) and every reader (the agents) honors these shapes, and `contract-schemas/`
encodes them for ajv.

Before reading, run `cd orchestrator/api-contract && npm run --silent
contract:paths`. Use its `inventory` and `areasDir` values. Treat a nonzero
exit as an invalid snapshot; never reconstruct a generation path by name.

## Inventory artifact — the compact index

```jsonc
// path returned as `inventory` by contract:paths
{ "schemaVersion":1,
  "source": { "kind":"openapi"|"postman"|"merged", "openApiUrl":str|null, "openApiVersion":str|null, "title":str|null, "specHash":"sha256:…"|null, "fetchedAt":ISO|null, "postmanImportedAt":ISO|null },
  "stats": { "endpoints":N, "areas":N, "schemas":N },
  "areas": { "<area>": ["<operationId>", …] },
  "endpoints": [ { "operationId":str (synthesized "<method><PathCamel>" when spec omits it), "method":"GET|POST|PUT|PATCH|DELETE", "path":"/x/{id}", "area":str (first OpenAPI tag kebab-cased, else first path segment), "summary":str|null, "auth":"bearer"|"none"|str|null, "deprecated":bool,
      "request": { "pathParams":[{name,type,required}], "query":[{name,type,required}], "body":{"schemaRef":str,"contentType":str}|null },
      "response": { "<status>": { "schemaRef":str|null, "array":bool } },
      "errors": ["401",…],
      "examples": { "request":bool, "response":bool } } ] }
```

`source.kind` records which sources have contributed: `openapi` (spec only), `postman` (bootstrap
mode, no spec — `postman.md`), `merged` (both). `schemaRef` strings are **not inlined** — they point
into the area slices, which keeps the inventory small enough to read whole.

## `<areasDir>/<area>.json` — the per-area field slice

```jsonc
// join contract:paths.areasDir with "<area>.json"
{ "schemaVersion":1, "area":str,
  "schemas": { "<SchemaRef>": { "fields": [ { "name":str, "jsonName":str, "type":"string|integer|number|boolean|object|array|ref:<SchemaRef>", "itemsRef":str|null, "format":str|null, "required":bool, "nullable_declared":bool|null (null = unknown, postman-bootstrapped), "nullable_observed":bool|null (null = never observed), "enum":[…]|null, "enum_observed":[…]|null, "example":any|null } ] } } }
```

`nullable_declared` vs `nullable_observed` is the precision feature: declared comes from the spec,
observed from real example payloads (`postman.md`). `nullable_observed: true` on a field declared
non-null means **the spec lies** — the all-nullable DTO is justified by data and the mapper keeps its
guard. `null` always means *unknown*, never *false*.

## Area derivation (NORMATIVE)

`area` = the endpoint's first OpenAPI tag (`@ApiTags`), kebab-cased (`Push Tokens` → `push-tokens`);
when the spec carries no tag, the first path segment (`/auth/login` → `auth`). Areas *usually* line up
with the DTO package layout `dto/<area>/` and the `<Product>Api` section comments
(`../../data-layer/references/dtos-and-api.md`), and that is the convention to follow — but the drift
validator does **not** rely on that folder↔area alignment as a join key. Real projects diverge (plural
or compound DTO folders, `dto/<a>/<b>/` nesting), so the folder name is not a reliable index into the
slices. The drift validator therefore pairs a DTO class to a snapshot schema by **schema name**
(global, authoritative), using the DTO's folder only as a tie-breaker when a name is otherwise
ambiguous (`drift.md`).

## operationId synthesis (NORMATIVE)

When the spec omits `operationId`, the normalizer synthesizes `<method><PathCamel>` — lowercase HTTP
method + the path camel-cased with parameter braces folded in: `GET /notifications` →
`getNotifications`, `PUT /notifications/{id}/read` → `putNotificationsIdRead`. Synthesis is
deterministic so `areas` arrays, drift findings and cross-references keep stable anchors across
re-pulls.

## How each consumer reads it

- **`endpoint-builder` — Step 1 "Confirm the contract".** Look the endpoint up in
  the resolved inventory (by path + method, or operationId), then read the
  **one** `<areasDir>/<area>.json` slice for the `schemaRef`s it touches. Field names, types, `required`,
  `nullable_declared`/`nullable_observed`, enums — taken verbatim, zero invented fields. Gate `true` +
  endpoint absent from the inventory → report `BLOCKED` instead of guessing.
  The same applies to gate `auto` with no snapshot: publish a generation through
  Backend Test + Refresh before endpoint/DTO work.
- **`mapper-builder` — null-guard derivation.** The per-field
  `AppLogger.Mapping.log(field) { … } ?: return null` set (`../../mappers/references/null-safety-and-logging.md`) is
  *derived* from the slice, not guessed: a field that is `required` and never observed null is a guard
  candidate the domain can rely on; a field nullable (declared **or** observed) keeps the defensive
  treatment regardless of what the domain would prefer. The DTO itself stays all-nullable either way
  (ADR-2).
- **`data-feature-builder` — availability check.** Before planning a feature's data flow, confirm in
  the inventory that every endpoint the feature assumes actually exists, with the response shape it
  expects. A missing endpoint is a scope decision (cut, stub, or escalate per the gate), made *before*
  building, not a runtime surprise.
- **`context-finder` — Tier-0 lookup.** Answer "does
  `POST /workouts` exist? what is its shape?" from the inventory before any grep. The inventory is a
  validated contract of record. On a miss, prepend `CONTRACT_MISS: <what was missing>` and recommend
  Backend Test + Refresh; do not manufacture a server shape from client code or task text.
- **`implementation-planner` — contract citation.** The per-file contract for a DTO/endpoint/mapper
  file names its `schemaRef` + the exact field list from the area slice, pinning the builder's source
  of truth inside the plan itself.

## Context economy (MUST)

Read the **resolved inventory** to locate; read **one**
`<areasDir>/<area>.json` slice for fields. Never load the `spec` artifact into
agent context — the raw document (potentially multi-MB) exists for
re-normalization and human review only. This is the same economy as `.arch-map.json` and Figma's
per-widget matrix: a compact committed index instead of re-reading the world per task.
