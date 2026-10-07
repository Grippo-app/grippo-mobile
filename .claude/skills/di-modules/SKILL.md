---
name: di-modules
description: >-
  Wire Koin dependency injection for a KMP feature or service — add a DI module,
  provide a single/factory/viewModel binding, compose modules with includes, and
  register the module in :shared/Koin.kt. Use when a task introduces a new Koin
  `@Module @ComponentScan`, a `@Single`/`@Factory` impl (with `binds =
  [Interface::class]`), an inline provider method, or needs a new module added to
  the composition root. Covers Koin Annotations + KSP, the annotation-vs-hand-DSL
  rule (and the `FeatureApiModule` carve-out), the no-`getKoin()`-in-composable
  rule, and module wiring.
---

# DI modules

Operational entrypoint for Koin dependency-injection wiring. DI is **Koin
Annotations + KSP** — every new module/binding is annotated; KSP generates the
wiring at build time. This is the
routing map, not new rules.

## When to use

- Add a new `@Module @ComponentScan` class for a feature/service module.
- Provide a `@Single` / `@Factory` (with `binds = [Interface::class]` when an
  impl backs an interface), or an inline `@Single internal fun provide…`.
- Compose modules via `@Module(includes = [Other::class])`.
- Register a new module in `:shared/Koin.kt`'s `modules(...)` list.

## Required inputs

- **Task file path** and the **module name** (`<X>Module` / `<X>FeatureModule`,
  matching the module folder; plural only when the folder/domain is plural).
- The **bindings** to provide and their **lifetime** (`@Single` vs `@Factory`)
  and **interface targets** (`binds`).
- The **includes** the module needs (the data-service/toolkit modules it reads).
- Where it registers — the live `:shared/Koin.kt` composition root.

If a binding's lifetime or interface target is ambiguous, stop and ask — guessing
singleton-vs-factory state is out of scope.

## Workflow

1. **Annotations, not hand-DSL.** New modules are `@Module @ComponentScan public
   class <X>Module`; impls are `@Single`/`@Factory`. No hand-written
   `module { single { … } }` for new code.
2. **Per-layer convention.** Feature modules are empty + `@ComponentScan` (KSP
   scans the package). Service modules that adapt a `NativeContext` factory or a
   builder use inline `@Single internal fun provide…` providers. Providers-only
   auth modules (`GoogleAuthModule`, `AppleAuthModule`) omit `@ComponentScan`.
3. **`binds` on interface impls.** `@Single(binds = [NoteRepository::class])` —
   without it the interface isn't wired and `inject<Interface>()` fails.
4. **Compose with `includes`.** List every module whose providers this one needs;
   transitive includes resolve automatically. Don't mechanically copy
   `[BackendModule, DatabaseModule]` — list what the feature actually reads.
5. **Register in `:shared/Koin.kt`.** Add `<X>Module().module` to the explicit
   `modules(...)` list — a missing entry = runtime "no definition found".
6. **No `getKoin().get()` in a Composable.** Deps flow Component → ViewModel;
   `BaseViewModel` field-injects only its infrastructure deps. Composables read
   `AppTokens.*` and `viewModel.state` — no `koinInject` in screens.

## Stop and ask

- The single hand-DSL exception, `FeatureApiModule`, would be touched or a second
  `module { … }` site introduced — it is the only sanctioned hand-DSL module.
- A `@Single` on a class that holds per-call/per-VM state (likely `@Factory`),
  or two impls binding the same interface without a `named(...)` qualifier.
- A binding lifetime / interface target you would have to invent.
- Required reading file missing → `BLOCKED: required reading missing — <list>`.

## References to read

This skill is **self-contained** — its normative rules live in
[`references/`](references/index.md), and it reads no external rule docs at
runtime. Read the routing table there for your task kind.

| Task kind | Read first |
|---|---|
| Routing table (all task kinds) | [`references/index.md`](references/index.md) |
| Koin mental model / what's annotated / consumes-Koin | [`references/koin-overview.md`](references/koin-overview.md) |
| Annotation conventions (`@Single`/`@Factory`/`@InjectedParam`, includes, KSP config) | [`references/annotations.md`](references/annotations.md) |
| `FeatureApiModule` hand-DSL carve-out | [`references/annotations.md`](references/annotations.md) § "FeatureApiModule — the one hand-DSL exception" |
| Composition root / `Koin.init` / module order / add-a-module checklist | [`references/composition-root.md`](references/composition-root.md) |
| `:shared` composition root, `Koin.kt` responsibilities, `RootComponent`/`RootViewModel`, `:shared/build.gradle.kts` | [`references/shared-module.md`](references/shared-module.md) |

## Validators / gates

- `di-validator` — verifies new `@Single`/`@Factory` are wired, impls declare
  `binds = [Interface::class]`, new modules appear in `:shared/Koin.kt`, and no
  new hand-DSL was introduced. **Owned by the `validation-gates` skill** (DI has
  no owned builders — it is the gate for DI work). Contract:
  [`orchestrator/contracts/agents/di-validator.md`](../../contracts/agents/di-validator.md).
- `architecture-validator` — the missing-module / dependency-direction check.
- `build-validator` — both-platform build green (KSP generates the `.module`).

## Output contract

Builders return the normalized envelope in
[`orchestrator/contracts/builder-report.md`](../../contracts/builder-report.md)
(`agent`, `status`, `files_touched`, `produced_signatures`, `blockers`,
`scope_deviations`, `handoff`). When an implementation plan is passed, its
per-builder section (per
[`orchestrator/contracts/planner-output.md`](../../contracts/planner-output.md))
is authoritative for file paths and names; this skill's recipes define the
methodology. DI work has no owned builders — Koin wiring is produced by the
feature/service builders and gated by `di-validator`.
