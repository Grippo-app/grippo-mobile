---
name: data-layer
description: >-
  Build the data layer for a KMP feature — add a data feature, repository, DTO,
  domain model/enum, persist data, add or extend an endpoint on <Product>Api,
  add/extend a Room entity, DAO, or migration, or store a DataStore preference.
  Use when a task introduces a new domain concept the UI needs as a <X>Feature,
  adds/changes a backend endpoint + DTO, changes a Room schema (new column,
  index, table, type change), persists a key-value preference, or touches
  auth/token storage. Covers the repository + feature-api split, observe/get/
  mutate shape, DTO↔Entity↔Domain boundary, and the migration authorization gate.
---

# Data layer

Operational entrypoint for data-layer work. The UI sees only the `<X>Feature`
interface in `:data-features:feature-api`; the impl, repository, `<Product>Api`,
Room, and DataStore live below it. This is the routing map, not new rules.

## When to use

- Add a new `:data-features:<name>` module (new domain concept the UI consumes).
- Add or extend an endpoint + DTO on the flat `<Product>Api`.
- Add or change a Room entity / DAO, or write a migration (schema delta).
- Persist a small key-value preference via DataStore on the owning Repository.
- Touch token/auth storage (load-bearing — explicit review only).

## Required inputs

- **Task file path** and the **feature name** (domain noun).
- **Domain models** the feature exposes (strict-typed: enums for closed sets,
  `LocalDateTime`/`Duration` for timestamps/numbers — never raw `String`).
- **API surface** (`observe* : Flow`, `get* : Result<Unit>`, verb mutations
  `: Result<T>`) and which **endpoints** are needed.
- **Persistence** — Room entity/DAO? DataStore preference (name, type, owner
  Repository)? Migration (schema delta + `prelaunch` state)?
- For endpoints, the **contract source** (see Step 1 of the endpoint recipe).

If domain shape is unclear, stop and ask — inventing a domain shape is out of
scope.

## Workflow

1. **Server owns the contract.** Confirm the endpoint is live before defining
   the DTO; check `backendContractEnabled` + the inventory snapshot. Mobile does
   not invent endpoints.
2. **No codegen** unless explicitly authorized — DTOs/endpoints are synced by
   hand against the contract.
3. **No speculative entities or writes.** Write to the DAO only inside
   `response.onSuccess { … }`; reconcile ranges (`deleteAllExceptIds`).
4. **Strict typing at the domain edge only.** DTO/Entity keep raw `String?` /
   `String`; promotion to enum / `LocalDateTime` / `Duration` happens in the
   `…→domain` mapper. Coordinate `mapper-builder` per direction.
5. **Migrations are a deliberate, separate task.** Authorization is enforced by
   `task-intake` before `room-migration-builder` runs; `prelaunch: true` skips
   the migration (destructive fallback), `prelaunch: false` requires one. Verify
   both Android and iOS.
6. **Prelaunch / verify gates** — build green on both platforms (Android-only
   when `iosEnabled: false`); commit the new schema JSON with the migration.
7. **No raw `Throwable` leak.** The Repository returns `Result` / lets
   exceptions propagate via `runCatching`; never catch-and-swallow, never return
   a DTO. Error translation is the ViewModel's job (error pipeline).

## Stop and ask

- With gate `auto` or `true`, the validated snapshot or endpoint is missing →
  `BLOCKED` for endpoint/DTO work; never substitute task text.
- A Room migration on a **shipped** (`prelaunch: false`) DB with a risky delta.
- Changing `fallbackToDestructiveMigration`, the `TokenProvider`, or any
  load-bearing auth/refresh infra.
- A domain shape you would have to invent.
- Required reading file missing → `BLOCKED: required reading missing — <list>`.

## References to read

This skill is self-contained — it carries its own rules and reads no external rule docs
at runtime. Read the reference pack under [`references/`](references/index.md). The full
routing table (topic → file) lives in [`references/index.md`](references/index.md).

| Task kind | Read first |
|---|---|
| New data feature | [`references/cookbook-data-feature.md`](references/cookbook-data-feature.md); [`references/repositories.md`](references/repositories.md) |
| Repository shape / data flow | [`references/repositories.md`](references/repositories.md); [`references/module-structure.md`](references/module-structure.md) |
| Endpoint + DTO | [`references/cookbook-endpoint.md`](references/cookbook-endpoint.md); [`references/dtos-and-api.md`](references/dtos-and-api.md), [`references/backend-client.md`](references/backend-client.md) |
| Room entity / DAO / migration | [`references/cookbook-room-migration.md`](references/cookbook-room-migration.md); [`references/persistence-room.md`](references/persistence-room.md) |
| DataStore preference | [`references/datastore.md`](references/datastore.md); [`references/module-structure.md`](references/module-structure.md) |
| Auth / token | [`references/auth-session.md`](references/auth-session.md); [`references/error-pipeline.md`](references/error-pipeline.md) |
| `:data-services:*` rules / anti-patterns | [`references/module-structure.md`](references/module-structure.md); [`references/repositories.md`](references/repositories.md) |

## Validators / gates

- `data-layer-validator` — repository/feature split, observe/get/mutate shape,
  no DTO leak, no speculative DAO writes.
- `architecture-validator` — module dependency direction, no cross-feature import.
- `naming-convention-validator` — `observe*/get*/save*`, `<X>RepositoryImpl`,
  `Migration<N>To<N+1>`, snake_case DataStore keys.
- `build-validator` — both-platform build green.
- `backend-contract-drift` — DTO/endpoint vs. the contract snapshot (when
  `backendContractEnabled`).

## Output contract

Builders return the normalized envelope in
[`orchestrator/contracts/builder-report.md`](../../contracts/builder-report.md)
(`agent`, `status`, `files_touched`, `produced_signatures`, `assumptions`, `blockers`,
`scope_deviations`, `handoff`). When an implementation plan is passed, its
per-builder section (per
[`orchestrator/contracts/planner-output.md`](../../contracts/planner-output.md))
is authoritative for file paths and names; this skill's recipes define the
methodology. Owned builders: `data-feature-builder`,
`data-service-scaffold-builder`, `room-migration-builder` (each `Read, Edit,
Write, Bash, Grep, Glob`, `sonnet`).
