# Project Vision

## What this architecture describes

A Kotlin Multiplatform (KMP) mobile project that ships an Android app and an iOS
app from a single Kotlin codebase. UI is written once in Compose Multiplatform;
navigation and lifecycle are handled by Decompose; DI by Koin (annotation-driven
via KSP); networking by Ktor; persistence by Room Multiplatform.

The architecture is **product agnostic**: every architectural choice here is
independent of the product domain. Replace the example domain (`Note`, `Tag`, ...)
with the new project's domain — the layers and patterns stay.

## Non-goals

- **No domain/API code generation.** API DTOs are written by hand; mappers are
  written by hand. No OpenAPI/swagger-codegen, no kotlinx-rpc, no kapt/Hilt code
  generators (KSP is used only for Koin and Room). Tooling may still generate
  design tokens or local sidecar reports.
- **No backwards-compatibility shims.** The architecture targets a fresh project.
  New code follows the canonical module and package layout described by the
  skills.
- **Mandatory automated proof per task.** Every new or changed observable
  behavior ships with a behavioral test on its proven lane, every bugfix with a
  regression test; wiring/resources/scaffold changes get structural gates. The
  architecture supports this natively (interfaces between layers, fakeable
  repositories, injected dispatchers/clock) and the single machine authority is
  `orchestrator/tasks/test-policy.json` — no role re-declares the policy.
- **No build optimization tuning.** Convention plugins, `gradle.properties`, and
  Compose stability config are specified once and treated as fixed; tweaking them
  is a separate, deliberate task.

## Architectural principles

1. **One pattern per concern.** MVI for screens, Decompose for navigation,
   Repository for data, Koin annotations for DI. No mixing (no manual
   `module { single { ... } }` DSL for new feature/service modules — see the
   `diHandWrittenModules` allowlist in [`project-config.md`](project-config.md)
   and the `FeatureApiModule` carve-out in the DI rules).
2. **Strictly directional dependency graph.** UI never reaches `:data-services:*`
   directly; the boundary is `:data-features:feature-api`. See
   [`architecture-overview.md`](architecture-overview.md).
3. **Multiplatform by default.** Anything Android-specific lives in `androidMain`;
   iOS-specific in `iosMain`; everything else in `commonMain`. Platform-specific
   behavior is exposed via `expect`/`actual`.
4. **Explicit composition.** All Koin modules are listed by name in
   `:shared/Koin.kt` — no classpath scanning at runtime. The list is the public
   surface of the application's dependency graph.
5. **State is immutable, derived locally.** `@Immutable` data classes,
   `kotlinx-collections-immutable`, `UiText` instead of raw strings in state,
   `*FormatState` for form fields (see [`template-conventions.md`](template-conventions.md)
   §4 for which are required infrastructure vs product examples). UI computes
   derived values via `remember`; state never duplicates data already present in
   a sub-state.

## Out-of-scope vs in-scope for the requirements

| Topic | In scope | Out of scope |
|---|---|---|
| Module layout, dependency graph | ✅ | — |
| MVI contract (7-file template) | ✅ | — |
| Base classes (BaseViewModel etc.) | ✅ | — |
| Design system tokens | ✅ | Specific palette values (define per product) |
| Navigation pattern | ✅ | Specific route names (per product) |
| Data layer pattern | ✅ | Specific API endpoints (per product) |
| DTO conventions | ✅ | Specific DTOs (per product) |
| Room conventions | ✅ | Specific schema (per product) |
| Mappers | ✅ | Specific mappings (per product) |
| Build infrastructure | ✅ | — |
| Testing strategy | (mentioned: opt-in) | Test code (no defaults) |
| CI/CD | — | (per product) |
| Release pipeline | — | (per product) |

## Success criteria

A new project bootstrapped from these requirements should:

1. Build green on Android and iOS (XCFramework) without manual fixes after
   following `orchestrator/launch.md`.
2. Have a working "hello world" screen wired through MVI, Decompose, Koin, and
   the design system.
3. Be ready to add the first product feature using the cookbook recipes
   (the cookbook recipes in the skills’ references).
4. Pass these reviews: package conventions match the design-system skill,
   references/packages.md; module graph matches the platform-build-toolkit skill,
   references/module-structure.md; convention plugins match the platform-build-toolkit
   skill, references/convention-plugins.md.
