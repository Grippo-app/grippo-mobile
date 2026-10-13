# Shared composition and platform startup

The current source of truth is `shared/src/commonMain/kotlin/com/grippo/shared/Koin.kt`.
`Koin.init` starts one Koin context and returns `ApplicationBootstrap`, whose analytics,
crashReporting and messaging contracts accept platform SDK adapters.

- Register modules explicitly by their generated `.module`. Add the actual shared build dependency
  for each imported implementation. Preserve the existing startup composition/order unless authorized.
- Ordinary binding declaration order is not a reason to reorder modules. Startup registration must
  complete before resolving services that read schemas/configuration; inspect actual eager resolution.
- Analytics implementations depend on API contracts, not UI/domain/SDK transport. Qualified schema
  providers register before analytics core resolution; use the canonical analytics skill.
- Android transport adapters are app-owned in `data-services/firebase/`; only Android app depends
  on that transport. Swift SDK adapters live in `iosApp`. Shared/features use toolkit contracts.
- Inspect `androidApp/src/main/java/com/grippo/android/App.kt`,
  `iosApp/iosApp/AppDeletage.swift` and `shared/.../bootstrap/ApplicationBootstrap.kt` for current
  constructor names and attachment calls rather than retaining copied startup implementations.
- For screen services MUST apply [feature analytics](../../feature-analytics/SKILL.md).
  DI wiring MUST NOT introduce feature-owned lifecycle binding in routes/render.
