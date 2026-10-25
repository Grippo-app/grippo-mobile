---
name: di-modules
description: Wire and review Grippo Koin annotation bindings, lifetimes, module includes and explicit shared composition. Use when dependencies or DI startup registration change.
---

# Grippo DI

Inspect the owning module, current provider/implementation and
`shared/src/commonMain/kotlin/com/grippo/shared/Koin.kt` before editing.

- MUST declare new production modules/bindings using Koin Annotations + KSP. A module whose
  package holds annotated classes is `@Module @ComponentScan`; a module whose bindings are only
  provider methods on the module class omits `@ComponentScan` (today: `AnalyticsModule`,
  `CrashReportingModule`, `MessagingModule`, `ShareModule`, `GoogleAuthModule`, `AppleAuthModule`).
- MUST give interface implementations explicit `binds`; a provider's lifetime follows the owner
  ([annotation patterns](references/annotations.md)). Singleton state must not accidentally own
  per-VM/per-call state. Resolve ambiguous ownership or duplicate-interface qualification before
  choosing a binding.
- Include dependencies the module actually uses; do not copy BackendModule/DatabaseModule blindly.
  `:shared` registers every generated module explicitly. Composition changes stay inside the
  authorized scope; scope expansion or a locked-decision change needs a question
  ([scope discipline](../../../CLAUDE.md#scope-discipline)).
- Existing `FeatureApiModule` is the sole hand-DSL carve-out. UseCases inside the authorized scope
  are registered there using its existing pattern. MUST NOT create a second hand-written module or
  annotate UseCases merely to bypass the carve-out.
- VMs are constructed in Component `retainedInstance`; dependencies resolve there. MUST NOT perform
  DI lookup in a Composable/Screen or thread a feature analytics parameter through
  Component/factory/route. Apply [feature-analytics](../feature-analytics/SKILL.md) for its
  stateless services and screen hook.
- `Koin.init` returns `ApplicationBootstrap`. Platform shells attach SDK adapters via its typed
  contracts; they do not define feature DI or call startup twice
  ([composition root](references/composition-root.md)).

Before completion MUST review binding types, qualifiers, lifetime, includes, generated-module imports
and all explicit startup registrations. Source review cannot prove runtime resolution: run the
permitted checks under the [verification policy](../../../CLAUDE.md#verification-policy) and
report what stays unverified.

Read [annotation patterns](references/annotations.md) for binding choices and
[composition startup](references/composition-root.md) for app integration.
