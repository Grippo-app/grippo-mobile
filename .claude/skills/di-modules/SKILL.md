---
name: di-modules
description: Wire and review Grippo Koin annotation bindings, lifetimes, module includes and explicit shared composition. Use when dependencies or DI startup registration change.
---

# Grippo DI

Inspect the owning module, current provider/implementation and
`shared/src/commonMain/kotlin/com/grippo/shared/Koin.kt` before editing.

- New production modules/bindings use Koin Annotations + KSP. Feature implementation modules
  normally use `@Module @ComponentScan`; provider-only modules omit scanning where appropriate.
- Interface implementations declare `binds`; providers have the actual lifetime required by the owner.
  Singleton state must not accidentally own per-VM/per-call state. Resolve ambiguous ownership
  or duplicate-interface qualification before choosing a binding.
- Include dependencies the module actually uses; do not copy BackendModule/DatabaseModule blindly.
  `:shared` explicitly registers generated modules; confirm authorization before changing its composition.
- Existing `FeatureApiModule` is the sole hand-DSL carve-out. Authorized UseCases may be registered
  there using its existing pattern. Do not create a second hand-written module or annotate UseCases
  merely to bypass the carve-out.
- VMs are constructed in Component retainedInstance; dependencies resolve there. No DI lookup in
  Composable/Screen, no feature analytics parameter threaded through Component/factory/route.
  Apply [feature-analytics](../feature-analytics/SKILL.md) for its stateless services and screen hook.
- `Koin.init` returns ApplicationBootstrap. Platform shells attach SDK adapters via its typed contracts;
  they do not define feature DI or call startup twice.

Before completion review binding types, qualifiers, lifetime, includes, generated-module imports
and all explicit startup registrations. Source review cannot prove runtime resolution. No test
infrastructure; run permitted checks/builds only within user scope and report limitations.

Read [annotation patterns](references/annotations.md) for binding choices and
[composition startup](references/composition-root.md) for app integration.
