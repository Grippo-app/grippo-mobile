---
name: mappers
description: >-
  Add a mapper — DTO↔domain↔state mapping between one of the seven isolated
  `:data-mappers:*` directional modules. Use when a domain area needs a
  DTO→Entity, Entity/Pack→Domain, DTO→Domain, Domain→State, State→Domain,
  Domain→Entity (drafts), or Domain→Body bridge — i.e. "add mapper", "map
  direction", "nullable DTO handling", "DTO↔domain↔state mapping". One mapper
  task = one direction × one area; the orchestrator may run this N times for one
  larger task. Covers the `to<Target>()` / `to<Target>OrNull()` naming, the
  null-and-drop logging rule, type promotion at the domain edge, and the
  no-business-logic boundary.
---

# Mappers

Operational entrypoint for mapper work. Conversions between DTOs / Entities /
Domain / UI State / Bodies live in **seven dedicated modules**, one per
direction — each isolated (no cross-mapper imports, no shared base, no DI).
This is the routing map, not new rules.

## When to use

- Add a new `<Area>Mapper.kt` in one of the seven `:data-mappers:*` modules.
- A new domain area needs DTO→Entity + Entity→Domain (the minimum pair), or a
  form needs State→Domain / Domain→Body / Domain→Entity (drafts).
- A DTO/Entity/Domain shape changed and the existing mapper must follow.

## Required inputs

- **Task file path**.
- **Direction** — one of the seven (table below).
- **Area subpackage** — domain noun (`note`, `tag`, `notifications`, …).
- **Source + target types** — fully-qualified class names.
- **Parent-id parameter** — when the target FK comes from the call site
  (`profileId` for user-scoped entities, `<parent>Id` for draft child rows).

If the domain/DTO shape is unclear, stop and ask — inventing a shape is out of
scope.

## Workflow

The seven directions and which module each lives in:

| Direction | Module |
|---|---|
| DTO → Entity | `:data-mappers:dto-to-entity` |
| Entity / Pack → Domain | `:data-mappers:entity-to-domain` |
| DTO → Domain (no DB) | `:data-mappers:dto-to-domain` |
| Domain → State | `:data-mappers:domain-to-state` |
| State → Domain | `:data-mappers:state-to-domain` |
| Domain → Entity (drafts) | `:data-mappers:domain-to-entity` |
| Domain → DTO Body | `:data-mappers:domain-to-dto` |

1. **One direction × one area per file.** Slashed directory layout
   (`com/<org>/<product>/<from>/<to>/<area>/<Area>Mapper.kt`); `package`
   mirrors it. Edit an existing area file rather than duplicating it.
2. **Null handling = log-and-drop.** DTO-source and entity-relation/enum-parse
   fields use `AppLogger.Mapping.log(value) { msg } ?: return null` — one call
   per required field, specific message. Never `!!`, never placeholder defaults.
   Nullable target fields pass through; non-null collection columns may coalesce
   to `emptyList()`; server-alias coalesce is `(a ?: b)` inside one log call.
   Logging is the mapper's job (`AppLogger.Mapping`), not the consumer. The
   three `:domain-to-*` directions take non-null domain inputs → no log calls.
3. **Type promotion at the domain edge.** DTO/Entity keep raw `String?`/`String`;
   promotion to enum (`<X>Enum.of`), `LocalDateTime`, `Duration` happens in the
   `…→domain` mapper. `domain→dto` reverses via `.key`.
4. **No business logic in mappers.** Pure field translation. Validation,
   derivation, aggregation, side effects belong in `<X>UseCase` / Repository /
   ViewModel — never the mapper. No `@Composable`, no `@Single`/`@Factory`, no
   class, no cross-mapper import (compose at the call site instead).
5. **May run N times.** One larger task spans several directions/areas; the
   orchestrator invokes the builder once per direction × area.

## Stop and ask

- A direction you'd need outside the seven (`state-to-entity`, …) — almost
  certainly compose two existing directions instead.
- A mapper-module `build.gradle.kts` missing a source/target/`:toolkit:logger`
  dep → `BLOCKED: mapper module dependencies missing — <list>` (a new module
  dep is a wider task).
- A domain/DTO shape you would have to invent.
- Required reading file missing → `BLOCKED: required reading missing — <list>`.

## References to read

Self-contained reference pack — read the routing table for your task kind. Full
index: [`references/index.md`](references/index.md).

| Task kind | Read first |
|---|---|
| Add a mapper (full recipe, by-direction patterns) | [`references/cookbook-add-mapper.md`](references/cookbook-add-mapper.md) |
| Seven directions / module layout / build files | [`references/mapper-directions.md`](references/mapper-directions.md) |
| Function naming / signature conventions | [`references/mapping-conventions.md`](references/mapping-conventions.md) |
| Null-safety policy (`OrNull`, log-and-drop) | [`references/null-safety-and-logging.md`](references/null-safety-and-logging.md) |
| Enum / sealed-type round-trip | [`references/mapping-conventions.md`](references/mapping-conventions.md) § "Enum dictionaries" |
| Where mappers sit in the data flow | [`references/mapper-directions.md`](references/mapper-directions.md) § "Mapper layer in the data flow" |
| Anti-patterns / mapper-to-mapper import | [`references/mapper-directions.md`](references/mapper-directions.md) § "Anti-pattern: mapper-to-mapper import" |

## Validators / gates

- `anti-pattern-scanner` — `!!` on DTO fields, placeholder defaults,
  cross-mapper imports, `@Composable`/`@Single` on mappers (grepable shapes).
- `naming-convention-validator` — `to<Target>()` / `to<Target>OrNull()` /
  `List<…>.to<Target>s()` extension-function naming.
- `architecture-validator` — mapper-module isolation, dependency direction.
- `build-validator` — `:data-mappers:<direction>:assemble` (+ iOS XCFramework
  gate when `iosEnabled: true`) build green.
- External reviewer — null-and-drop discipline + no-business-logic (the
  non-greppable rules).

## Output contract

Builders return the normalized envelope in
[`orchestrator/contracts/builder-report.md`](../../contracts/builder-report.md)
(`agent`, `status`, `files_touched`, `produced_signatures`, `blockers`,
`scope_deviations`, `handoff`). When an implementation plan is passed, its
per-builder section (per
[`orchestrator/contracts/planner-output.md`](../../contracts/planner-output.md))
is authoritative for file paths and names; this skill's recipes define the
methodology. Owned builder: `mapper-builder` (`Read, Edit, Write, Bash, Grep,
Glob`, `sonnet`).
