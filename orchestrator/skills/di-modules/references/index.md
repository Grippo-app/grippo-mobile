# DI-modules references — routing table

Self-contained reference pack for the `di-modules` skill. These files carry the
skill's own normative rules, so a builder reads here at runtime and needs no
external rule docs.

Route by task kind; each row lists the file to read first, then any supporting files.

| Task kind | Read first | Also |
|---|---|---|
| Koin mental model, what gets annotated, what stays out of Koin | [`koin-overview.md`](koin-overview.md) | [`annotations.md`](annotations.md) |
| Feature module shape (empty `@Module @ComponentScan`) | [`koin-overview.md`](koin-overview.md) § "How a feature module looks" | [`annotations.md`](annotations.md) |
| Service module shape (inline `@Single fun provide…` providers) | [`koin-overview.md`](koin-overview.md) § "How a service module looks" | [`annotations.md`](annotations.md) |
| `@Module` / `@ComponentScan` / includes propagation | [`annotations.md`](annotations.md) § "@Module and @ComponentScan" | — |
| `@Single` / `@Factory` / `@Scoped` / `binds` | [`annotations.md`](annotations.md) § "@Single", "@Factory" | [`koin-overview.md`](koin-overview.md) |
| `@InjectedParam` + `parametersOf(...)` | [`annotations.md`](annotations.md) § "@InjectedParam" | — |
| Module includes — what's typical per layer | [`annotations.md`](annotations.md) § "Module includes — what's typical" | — |
| KSP / `KOIN_CONFIG_CHECK` convention-plugin config | [`annotations.md`](annotations.md) § "Configuration" | — |
| `FeatureApiModule` hand-DSL exception (verbatim template) | [`annotations.md`](annotations.md) § "FeatureApiModule — the one hand-DSL exception" | — |
| Composition root, `Koin.init`, explicit module listing, order | [`composition-root.md`](composition-root.md) | [`shared-module.md`](shared-module.md) |
| Adding a module — checklist / wire a feature in `:shared` | [`composition-root.md`](composition-root.md) § "Adding a module — checklist" | [`shared-module.md`](shared-module.md) |
| When tests want a different module / multiple Koin contexts | [`composition-root.md`](composition-root.md) § "When tests want a different module" | — |
| How `BaseViewModel` / `RootViewModel` / `RootComponent` consume Koin | [`koin-overview.md`](koin-overview.md) § "How BaseViewModel itself uses Koin" | [`shared-module.md`](shared-module.md) |
| `:shared` composition root, `Koin.kt` responsibilities, `RootComponent`/`RootViewModel`/`RootScreen` | [`shared-module.md`](shared-module.md) | [`composition-root.md`](composition-root.md) |
| Why every module is listed in `:shared`, `:shared/build.gradle.kts` | [`composition-root.md`](composition-root.md) § "Why explicit listing" / [`shared-module.md`](shared-module.md) § "`:shared/build.gradle.kts`" | [`composition-root.md`](composition-root.md) |

## File map

| File | Covers |
|---|---|
| [`koin-overview.md`](koin-overview.md) | Why Koin Annotations, mental model, annotation table, what gets annotated, feature vs service module shapes, how `BaseViewModel`/`RootViewModel`/`RootComponent` consume Koin, what stays out of Koin, anti-patterns |
| [`annotations.md`](annotations.md) | KSP/`KOIN_CONFIG_CHECK` config, `@Module`/`@ComponentScan`, `@Single`/`@Factory`/`@Scoped`/`@InjectedParam`, includes-per-layer table, `Koin.init` setup, anti-patterns, `FeatureApiModule` hand-DSL exception |
| [`composition-root.md`](composition-root.md) | `:shared/Koin.kt` file, Android/iOS `Koin.init` call sites, module order, add-a-module checklist, why explicit listing, what `Koin.init` doesn't do, multiple contexts, test overrides, anti-patterns |
| [`shared-module.md`](shared-module.md) | `:shared` responsibilities, file layout, `Koin.kt`, `RootComponent`/`RootViewModel`/`RootScreen` shapes, `:shared/build.gradle.kts`, adding-to-`:shared` |

## Out of this skill's scope (handed off)

- **Forbidden DI patterns** (the consolidated anti-pattern list) →
  the `validation-gates` skill (`../../validation-gates/references/forbidden-patterns.md`,
  DI section). The DI-specific anti-patterns a builder must honor are embedded in each
  file's "Anti-patterns" section here.
- **`di-validator` gate contract** → `validation-gates` skill /
  `orchestrator/contracts/agents/di-validator.md`. DI work has no owned
  builders; Koin wiring is produced by feature/service builders and gated there.
