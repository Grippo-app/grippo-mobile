---
name: di-modules
description: Wire and review Grippo Koin annotation bindings, lifetimes, module includes and explicit shared composition. Use when dependencies or DI startup registration change.
---

# Grippo DI

Inspect the owning module, current provider/implementation and
`shared/src/commonMain/kotlin/com/grippo/shared/Koin.kt` before editing.

- MUST declare new production modules/bindings using Koin Annotations + KSP. Feature modules
  normally use `@Module @ComponentScan`; provider-only modules omit scanning where appropriate.
- MUST give interface implementations explicit `binds`; providers MUST use the lifetime required by the owner.
  Singleton state must not accidentally own per-VM/per-call state. Resolve ambiguous ownership
  or duplicate-interface qualification before choosing a binding.
- Include dependencies the module actually uses; do not copy BackendModule/DatabaseModule blindly.
  `:shared` explicitly registers generated modules. MUST keep composition changes within the task;
  ask only for scope expansion or locked-decision changes under `CLAUDE.md`.
- Existing `FeatureApiModule` is the sole hand-DSL carve-out. Authorized UseCases may be registered
  there using its existing pattern. MUST NOT create a second hand-written module or annotate UseCases
  merely to bypass the carve-out.
- VMs are constructed in Component retainedInstance; dependencies resolve there. MUST NOT perform DI lookup in
  Composable/Screen, no feature analytics parameter threaded through Component/factory/route.
  Apply [feature-analytics](../feature-analytics/SKILL.md) for its stateless services and screen hook.
- `Koin.init` returns ApplicationBootstrap. Platform shells attach SDK adapters via its typed contracts;
  they do not define feature DI or call startup twice.

Before completion MUST review binding types, qualifiers, lifetime, includes, generated-module imports
and all explicit startup registrations. Source review cannot prove runtime resolution.
MUST apply `CLAUDE.md` Verification policy and report unresolved runtime limitations.

Read [annotation patterns](references/annotations.md) for binding choices and
[composition startup](references/composition-root.md) for app integration.
