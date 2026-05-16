# Dependency Rules

The dependency graph is strictly directional. Violations are architecture bugs — they will cause merge conflicts, leak transitive symbols, and eventually break the iOS XCFramework boundary.

## Direction (top to bottom; arrows point at "depends on")

```
:androidApp / :iosApp
        ↓
     :shared
        ↓
┌──────────────────────────────────────────────────────────────┐
│ :ui-screen-features:* → :ui-dialog-features:*               │
│        ↓                       ↓                            │
│ :ui-core:foundation, :ui-core:state, :ui-core:error         │
│        ↓                       ↓                            │
│ :design-system:components → :design-system:core             │
│                              ↓                              │
│              :design-system:resources:provider              │
└──────────────────────────────────────────────────────────────┘
        ↓
:data-features:feature-api    (UI sees ONLY this from the data layer)
        ↓
:data-features:<feature>      (Repository / FeatureImpl, internal)
        ↓
:data-services:{backend, database, datastore, firebase, *-auth}
        ↓
:toolkit:*                    (http-client, logger, serialization, ...)
```

## Hard rules

1. **UI modules (`:ui-screen-features:*`, `:ui-dialog-features:*`) MUST NOT depend on `:data-services:*` directly.** The only path from UI to data is via `:data-features:feature-api`. Three narrow carve-outs exist in the reference repo:
   - `:ui-screen-features:authorization` imports `:data-services:google-auth` and `:data-services:apple-auth` so the login screen can drive the platform credential flow before a domain user exists. These services don't have a domain mirror by design — the ID token they return is fed straight into `AuthorizationFeature.login(...)`.
   - Any UI feature may import `:data-services:firebase` to emit analytics events directly (`FirebaseProvider.logEvent(...)`); the provider is a stateless `object` and bypassing a domain layer for telemetry is intentional.

   Do not extend this list. Anything that looks like a new exception is a `:data-features:feature-api` UseCase in disguise.
2. **`:data-features:feature-api` MUST NOT depend on `:data-services:*`.** It is pure interfaces + domain models. No DTOs, no entities, no DAOs.
3. **`:data-features:<feature>` (the implementation modules) MUST depend on `:data-features:feature-api` and the relevant `:data-services:*` and `:data-mappers:*` modules.** They are `internal` to the data layer; UI cannot import their classes.
4. **`:data-mappers:*` MUST depend on the two adjacent layers** (e.g. `:dto-to-entity` → `:data-services:backend` + `:data-services:database`) and **MUST NOT depend on UI feature/dialog modules** or on other mapper modules. The narrow exception: `:data-mappers:domain-to-state` and `:data-mappers:state-to-domain` import `:ui-core:state` because that is precisely their target / source — `:ui-core:state` is a pure-type module (`UiText`, `*FormatState`, `@Immutable` UI data classes) with no rendering logic, so it fits the same "pure-type back-edge" exemption used by hard rule 5 (`:toolkit:http-client` → `:ui-core:error:error-provider`) and `:design-system:components` → `:ui-core:state`.
5. **`:toolkit:*` MUST NOT depend on `:data-features:*`, `:data-services:*` (except `:data-services:firebase` for crash logging), `:ui-screen-features:*`, or `:ui-dialog-features:*`.** Toolkit is the bottom of the dependency stack. Two narrow exceptions are tolerated because the dependencies are on pure-type modules:
   - `:toolkit:http-client` imports `:ui-core:error:error-provider` so the response validator can throw `AppError` subtypes directly.
   - `:toolkit:date-utils` imports `:design-system:resources:provider` and `:design-system:core` to resolve locale-aware month/weekday `Res.string.*` formats and to read theme-derived format tokens.

   Both target modules are deps-free pure types (no `@Composable` UI, no business state). Do not extend this list without a deliberate review.
6. **`:design-system:*` MUST NOT depend on the data layer.** `AppTokens` and components are pure UI primitives.
7. **`:design-system:components` depends on `:design-system:core`; `:design-system:core` depends on `:design-system:resources:provider`.** The implementation `:design-system:resources:provider-impl` is consumed only by `:shared` (DI wire-up).
8. **`:shared` is the only module that imports "everything"** (composition root). `:androidApp`/`:iosApp` reach `RootComponent` and `Koin.init` only through `:shared`.
9. **Cross-feature visibility is via `:ui-screen-features:screen-api`** — public `*Router` sealed classes live there. A screen feature does not import another screen feature.
10. **Dialogs are visible via `:ui-dialog-features:dialog-api`** — `DialogConfig` lives there. A dialog feature does not import another dialog feature.

## What this looks like in `build.gradle.kts`

### A screen feature

```kotlin
kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.uiCore.foundation)
        implementation(projects.uiCore.state)
        implementation(projects.dataMappers.domainToState)    // OK: domain → state
        implementation(projects.uiDialogFeatures.dialogApi)   // OK: shows dialogs
        implementation(projects.uiScreenFeatures.screenApi)   // OK: cross-feature routers
        implementation(projects.dataFeatures.featureApi)      // OK: only path to data
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.components)
        implementation(projects.designSystem.preview)
        // ❌ projects.dataServices.backend — forbidden
        // ❌ projects.dataFeatures.notes — forbidden (use featureApi)
    }
}
```

### A data feature

```kotlin
kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.database)
        implementation(projects.dataServices.backend)
        implementation(projects.dataMappers.domainToEntity)
        implementation(projects.dataMappers.entityToDomain)
        implementation(projects.dataMappers.dtoToEntity)
        implementation(projects.dataMappers.domainToDto)
        implementation(projects.toolkit.dateUtils)
        // ❌ projects.uiCore.* — forbidden
        // ❌ projects.designSystem.* — forbidden
    }
}
```

### A toolkit module

```kotlin
kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.context)
        implementation(projects.toolkit.logger)
        // ❌ projects.dataServices.* — forbidden (except :data-services:firebase)
        // ❌ projects.designSystem.* — forbidden
        // ❌ projects.uiCore.* — forbidden
    }
}
```

## Anti-pattern: hidden transitive `api`

When module A `api`-depends on module B, A's consumers transitively see B's symbols. This creates **hidden coupling** — refactoring B can break A's consumers without changing A.

**Rule:** `api` is used only for dependencies that are part of the module's **public** API (e.g. `:shared` uses `api(libs.decompose.core)` because `RootComponent` exposes Decompose types to `:androidApp`/`:iosApp`). Everything else is `implementation`.

If a module needs symbols from a transitive dep, list that dep in its **own** `build.gradle.kts`. The build system makes you the dep explicitly; do not rely on someone else dragging it in.

## Anti-pattern: feature-to-feature cross-import

Two `:ui-screen-features:*` modules cannot import each other. If feature A needs to navigate to a screen in feature B:

1. The route is declared in `:ui-screen-features:screen-api` (public `<B>Router` sealed class).
2. `RootDirection` adds an entry; `RootViewModel` exposes a callback (`fun toB(...)`).
3. Feature A's `RootComponent` receives the callback via its constructor (threaded from `:shared`'s `RootComponent.createChild`).
4. Feature A's ViewModel calls the callback; `RootComponent.eventListener` translates `RootDirection.B` into `navigation.push(RootRouter.B(...))`.

Direct symbol import between feature modules is **forbidden**.

## Anti-pattern: mapper-to-mapper import

Mapper modules `:data-mappers:dto-to-entity` and `:data-mappers:entity-to-domain` cannot import each other. Each direction is isolated. If a chain conversion is needed (e.g. DTO → Domain), either:

- Use `:data-mappers:dto-to-domain` directly (a separate direction), or
- Compose two calls in the consumer (Repository or VM): `dto.toEntity().toDomain()`.

## What `:shared` depends on

`:shared` is the composition root: it imports every other module that needs to be wired into Koin or referenced by `RootComponent`. See `02-module-structure/04-shared-composition-root.md`.

## Verifying the rules

There is **no automated enforcement** of these rules at present. Verification is by code review. A future task could add:

- A Gradle convention that fails the build if `:ui-*` modules declare `:data-services:*` deps.
- A static analysis pass on `settings.gradle.kts` + each `build.gradle.kts` to detect violations.

Until then: every PR that touches `build.gradle.kts` is reviewed against this document.
