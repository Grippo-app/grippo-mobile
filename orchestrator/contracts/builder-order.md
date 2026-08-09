# Contract: builder-order

Frozen builder ordering / dependency rules. The `task-prep` / `task-orchestrator` skills MUST preserve these; a wrong order lands generated code in the wrong module or stalls a builder.

Source of truth: the `task-prep` skill (builder order).

## Canonical order (low layer → high layer)

1. `toolkit-builder` / `compose-lib-builder` — FIRST (bottom of the dependency graph).
2. `data-service-scaffold-builder` — when `<apiClassName>` / `Database` absent (bootstrap prereq).
3. `data-feature-builder`.
4. `ui-core-state-builder` — before `mapper-builder` (`:data-mappers:domain-to-state`) and before `screen-builder` (supplies the `*State` they consume).
5. `mapper-builder` — after source/target types exist; may run N times.
6. `feature-module-scaffold-builder` — before `screen-builder`/`dialog-builder` in a missing feature module.
7. `screen-builder` / `dialog-builder` — after scaffold/state/mapper prerequisites.
8. `cross-feature-nav-builder` — after route/component targets exist.
9. `resource-builder` — before the first consumer validation that needs the resource.
10. `app-shell-builder` — LAST (consumes `:shared` + the `Deeplink` key from `cross-feature-nav-builder`).

## Hard-ordering rules (grep-pinned to source)

- `feature-module-scaffold-builder` MUST run before `screen-builder`. Pin: ``feature-module-scaffold-builder` MUST run before `screen-builder``.
- `ui-core-state-builder` MUST run before `mapper-builder` / `screen-builder`. Pin: ``ui-core-state-builder` MUST run before `mapper-builder``.
- Toolkit first. Pin: `it runs FIRST`.
- App-shell last. Pin: `it runs LAST`.

## Prerequisite-prepend rules (frozen)

- `screen-builder` needs the feature module + `<X>Feature` → prepend `feature-module-scaffold-builder` if missing.
- `mapper-builder` (`domain-to-state`) needs the `*State` → prepend `ui-core-state-builder` if missing.
- `endpoint-builder` / `room-migration-builder` need `<apiClassName>` / `Database` → prepend `data-service-scaffold-builder` if missing.
- `room-migration-builder` additionally requires explicit user authorization in the task text.

This frozen record covers: missing backend/database scaffold prereq; multiple mappers; screen with missing feature module + ui-core-state; endpoint + data feature; room migration auth; resource consumed by UI; app-shell-last.
