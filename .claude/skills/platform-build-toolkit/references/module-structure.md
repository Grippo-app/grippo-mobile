# Module structure — graph & dependency rules

Normative reference for the KMP module graph and the strict directional dependency rules that govern it. Self-contained — apply directly; reads no external rule docs at runtime.

## Module graph

The full module graph is declared in `settings.gradle.kts`. This is the **single source of truth** for the module list; adding a module requires editing this file plus `:shared/Koin.kt` (if it provides DI) and `:shared/build.gradle.kts` (if `:shared` consumes it).

### `settings.gradle.kts` reference

```kotlin
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")

    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Auto-provisions JDK toolchains via the Foojay Disco API (the JDK 21 the screenshot-gate host-test
    // requests, and the app's JDK 19) — no machine-specific JDK paths, works on CI. Inert unless a
    // toolchain is requested. See the implement-figma skill (screenshot-fidelity gate).
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"   // pin to latest at setup time
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement.repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

@Suppress("UnstableApiUsage")
dependencyResolutionManagement.repositories {
    google {
        content {
            includeGroupByRegex("com\\.android.*")
            includeGroupByRegex("com\\.google.*")
            includeGroupByRegex("androidx.*")
        }
    }
    mavenCentral()
}

rootProject.name = "<product>-mobile"

include(":androidApp")
include(":shared")

include(":design-system:preview")
include(":design-system:core")
include(":design-system:resources:provider")
include(":design-system:resources:provider-impl")
include(":design-system:components")

include(":data-services:google-auth")
include(":data-services:apple-auth")
include(":data-services:firebase")
include(":data-services:backend")
include(":data-services:database")
include(":data-services:datastore")

include(":data-features:feature-api")
include(":data-features:authorization")
// ... one per business area

include(":ui-core:foundation")
include(":ui-core:state")
include(":ui-core:error:error-provider-impl")
include(":ui-core:error:error-provider")

include(":ui-screen-features:screen-api")
include(":ui-screen-features:authorization")
include(":ui-screen-features:home")
// ... one per top-level screen flow

include(":ui-dialog-features:dialog-api")
include(":ui-dialog-features:confirmation")
include(":ui-dialog-features:error-display")
// ... one per bottom-sheet flow

include(":toolkit:context")
include(":toolkit:localization")
include(":toolkit:theme")
include(":toolkit:http-client")
include(":toolkit:image-loader")
include(":toolkit:logger")
include(":toolkit:connectivity")
include(":toolkit:serialization")
include(":toolkit:date-utils")
include(":toolkit:link-opener")
include(":toolkit:notification-manager")
include(":toolkit:permission-manager")

include(":compose-libs:wheel-picker")
include(":compose-libs:segment-control")
include(":compose-libs:konfetti")
include(":compose-libs:chart")

include(":data-mappers:entity-to-domain")
include(":data-mappers:dto-to-entity")
include(":data-mappers:dto-to-domain")
include(":data-mappers:domain-to-state")
include(":data-mappers:domain-to-entity")
include(":data-mappers:domain-to-dto")
include(":data-mappers:state-to-domain")
```

**Type-safe project accessors** are enabled via `enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")`. This generates `projects.designSystem.core`, `projects.dataFeatures.notes`, etc. — use these instead of `project(":...")` in module-level `build.gradle.kts`.

### Module count and shape

The generated project starts with the minimum foundation and grows feature
modules only when the product needs them:

- Start with the **infrastructure modules** (everything except the feature folders). All convention plugins, `:shared`, `:design-system:*`, `:ui-core:*`, `:data-services:*` except product-specific ones, `:toolkit:*`, `:data-mappers:*` (empty initially) — these are the foundation.
- Add `:ui-screen-features:screen-api`, `:ui-dialog-features:dialog-api`, `:data-features:feature-api` as empty contract modules.
- Add product feature modules **one at a time** as features are built.

It is OK for `:ui-screen-features:*` and `:data-features:*` to start with fewer modules — they grow with the product. It is **not** OK to skip `:shared`, `:design-system:*`, `:ui-core:*`, `:toolkit:*`, or `:data-services:*` infrastructure — these define the architecture.

- An optional `:tooling` group may hold JVM-only build tools outside the KMP build graph; see "Optional modules" below.

### Mandatory infrastructure modules (minimum viable foundation)

> **`:ui-dialog-features:error-display` is mandatory too** — but it is deliberately *not* in the gitkeep list below. Unlike the empty-contract foundation modules (scaffolded as `.gitkeep` in launch Step 3), it ships real code: it is the render target of the error pipeline (the ui-feature skill, references/error-pipeline.md), so every `safeLaunch` failure routes `DialogConfig.ErrorDisplay` → `DialogContentComponent.createChild` → `ErrorDisplayComponent`. It is built as a unit in launch **Step 7.7**, before the `:shared` wiring (Step 9). If it is missing or stubbed, the first runtime error of any kind crashes with `NotImplementedError`.

```
:androidApp
:shared

:design-system:core
:design-system:components
:design-system:preview
:design-system:resources:provider
:design-system:resources:provider-impl

:ui-core:foundation
:ui-core:state
:ui-core:error:error-provider
:ui-core:error:error-provider-impl

:ui-screen-features:screen-api
:ui-dialog-features:dialog-api

:data-features:feature-api

:data-services:backend
:data-services:database
:data-services:datastore
:data-services:firebase  # if firebaseEnabled

:data-mappers:entity-to-domain
:data-mappers:dto-to-entity
:data-mappers:dto-to-domain
:data-mappers:domain-to-state
:data-mappers:domain-to-entity
:data-mappers:domain-to-dto
:data-mappers:state-to-domain

:toolkit:context
:toolkit:localization
:toolkit:theme
:toolkit:http-client
:toolkit:image-loader
:toolkit:logger
:toolkit:connectivity
:toolkit:serialization
:toolkit:date-utils
:toolkit:link-opener
:toolkit:notification-manager
:toolkit:permission-manager
```

`:iosApp` is an Xcode project, not a Gradle module — see `ios-framework.md`.

### Optional modules (add as needed)

| Group | When |
|---|---|
| `:data-services:google-auth` / `:data-services:apple-auth` | Only if the product uses these auth methods |
| `:compose-libs:*` | Add a module per reusable Compose widget that doesn't fit the design system |
| `:tooling:detekt-rules` | JVM-only Gradle helper hosting custom Detekt rules; consumed by the root `detekt` task. Not part of the KMP build graph. |

### Project-level non-module directories

The reference uses a top-level `config/detekt/detekt.yml` for Detekt configuration. It's consumed by `:tooling:detekt-rules` and the root `detekt` task. The bootstrap creates it when `:tooling:detekt-rules` is opted in (see this skill’s convention-plugins reference for setup); otherwise it remains reference-only.

### Per-module file convention

Every module's `build.gradle.kts` contains **only**:

1. `plugins { id("...convention") }` lines (plus rare `alias(libs.plugins.kotlin.serialization)` when `@Serializable` types are declared).
2. A `kotlin { ... }` block with `android { namespace = "..." }` and `sourceSets.commonMain.dependencies { ... }`.

No `android { compileSdk = ... }`, no `kotlin { jvmToolchain(...) }`, no manual `apply(plugin = ...)`. Everything is in the convention plugins. A handful of modules also need a top-level `compose.resources { ... }` block (`:design-system:resources:provider`) or an `androidLibrary { androidResources.enable = true }` block inside `kotlin { ... }` (`:design-system:resources:provider`, `:toolkit:notification-manager`) — these are module-specific and stay at the call site rather than in a convention plugin (without the resources-provider opt-in, Compose `composeResources` are not packaged into the APK on AGP 9 → runtime `MissingResourceException`; see the design-system skill, references/resources.md § Build requirement).

See `convention-plugins.md` for the convention plugin matrix.

## Dependency rules

The dependency graph is strictly directional. Violations are architecture bugs — they will cause merge conflicts, leak transitive symbols, and eventually break the iOS XCFramework boundary.

### Direction (top to bottom; arrows point at "depends on")

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

### Hard rules

1. **UI modules (`:ui-screen-features:*`, `:ui-dialog-features:*`) MUST NOT depend on `:data-services:*` directly.** The only path from UI to data is via `:data-features:feature-api`. Three narrow carve-outs exist in the template:
   - `:ui-screen-features:authorization` imports `:data-services:google-auth` so the login screen can drive the Google credential flow before a domain user exists. The service doesn't have a domain mirror by design — the ID token it returns is fed straight into `AuthorizationFeature.login(...)`.
   - `:ui-screen-features:authorization` imports `:data-services:apple-auth` so the login screen can drive the Apple credential flow before a domain user exists. The service doesn't have a domain mirror by design — the ID token it returns is fed straight into `AuthorizationFeature.login(...)`.
   - Any UI feature may import `:data-services:firebase` to emit analytics events directly (`FirebaseProvider.logEvent(...)`); the provider is a stateless `object` and bypassing a domain layer for telemetry is intentional.

   The same telemetry carve-out extends to **`:data-features:authorization`**, which imports `:data-services:firebase` to log auth lifecycle events (login success/failure, token refresh failures) at the boundary between the auth state machine and the persistence layer. This is the only `:data-features:*` → `:data-services:firebase` edge in the reference; all other feature modules route telemetry through their UseCases or stay UI-side. Firebase is treated as a cross-cutting sink (logger + analytics + crashlytics), not as a domain service.

   Do not extend this list. Anything that looks like a new exception is a `:data-features:feature-api` UseCase in disguise.
2. **`:data-features:feature-api` MUST NOT depend on `:data-services:*`.** It is pure interfaces + domain models. No DTOs, no entities, no DAOs.
3. **`:data-features:<feature>` (the implementation modules) MUST depend on `:data-features:feature-api` and the relevant `:data-services:*` and `:data-mappers:*` modules.** They are `internal` to the data layer; UI cannot import their classes.
4. **`:data-mappers:*` MUST depend on the two adjacent layers** (e.g. `:dto-to-entity` → `:data-services:backend` + `:data-services:database`) and **MUST NOT depend on UI feature/dialog modules** or on other mapper modules. The narrow exception: `:data-mappers:domain-to-state` and `:data-mappers:state-to-domain` import `:ui-core:state` because that is precisely their target / source — `:ui-core:state` is a pure-type module (`UiText`, `*FormatState`, `@Immutable` UI data classes) with no rendering logic, so it fits the same "pure-type back-edge" exemption used by hard rule 5 (`:toolkit:http-client` → `:ui-core:error:error-provider`) and `:design-system:components` → `:ui-core:state`.
5. **`:toolkit:*` MUST NOT depend on `:data-features:*`, `:data-services:*`, `:ui-screen-features:*`, or `:ui-dialog-features:*`.** Toolkit is the bottom of the dependency stack. Two narrow exceptions are tolerated because the dependencies are on pure-type modules:
   - `:toolkit:http-client` imports `:ui-core:error:error-provider` so the response validator can throw `AppError` subtypes directly.
   - `:toolkit:date-utils` imports `:design-system:resources:provider` and `:design-system:core` to resolve locale-aware month/weekday `Res.string.*` formats and to read theme-derived format tokens.

   Both target modules are deps-free pure types (no `@Composable` UI, no business state). Do not extend this list without a deliberate review.
6. **`:design-system:*` MUST NOT depend on the data layer.** `AppTokens` and components are pure UI primitives.
7. **`:design-system:components` depends on `:design-system:core`; `:design-system:core` depends on `:design-system:resources:provider`.** The implementation `:design-system:resources:provider-impl` is consumed only by `:shared` (DI wire-up). Additionally, `:design-system:components` may depend on `:compose-libs:*` (specialised widgets — segment-control, konfetti, chart) and on `:toolkit:date-utils` (chart date helpers). The `:ui-screen-features:screen-api` and `:ui-core:state` back-edges are covered by the pure-type exemption noted in rule 4 — see the `design-system` skill for the full design-system module edge list.
8. **`:shared` is the only module that imports "everything"** (composition root). `:androidApp`/`:iosApp` reach `RootComponent` and `Koin.init` only through `:shared`.
9. **Cross-feature visibility is via `:ui-screen-features:screen-api`** — public `*Router` sealed classes live there. A screen feature does not import another screen feature.
10. **Dialogs are visible via `:ui-dialog-features:dialog-api`** — `DialogConfig` lives there. A dialog feature does not import another dialog feature.

### What this looks like in `build.gradle.kts`

#### A screen feature

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

#### A data feature

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

#### A toolkit module

```kotlin
kotlin {
    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.context)
        implementation(projects.toolkit.logger)
        // ❌ projects.dataServices.* — forbidden
        // ❌ projects.designSystem.* — forbidden
        // ❌ projects.uiCore.* — forbidden
    }
}
```

### Anti-pattern: hidden transitive `api`

When module A `api`-depends on module B, A's consumers transitively see B's symbols. This creates **hidden coupling** — refactoring B can break A's consumers without changing A.

**Rule:** `api` is used only for dependencies that are part of the module's **public** API (e.g. `:shared` uses `api(libs.decompose.core)` because `RootComponent` exposes Decompose types to `:androidApp`/`:iosApp`). Everything else is `implementation`.

If a module needs symbols from a transitive dep, list that dep in its **own** `build.gradle.kts`. The build system makes you the dep explicitly; do not rely on someone else dragging it in.

### Verifying the rules

There is **no automated enforcement** of these rules at present. Verification is by code review. A future task could add:

- A Gradle convention that fails the build if `:ui-*` modules declare `:data-services:*` deps.
- A static analysis pass on `settings.gradle.kts` + each `build.gradle.kts` to detect violations.

Until then: every PR that touches `build.gradle.kts` is reviewed against this document.
