# Module Graph

The full module graph is declared in `settings.gradle.kts`. This is the **single source of truth** for the module list; adding a module requires editing this file plus `:shared/Koin.kt` (if it provides DI) and `:shared/build.gradle.kts` (if `:shared` consumes it).

## `settings.gradle.kts` reference

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

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

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

rootProject.name = "<your-project-name>"   // e.g. "<product>-mobile"

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

## Module count and shape (reference)

The reference repo contains ~85 modules (`grep -c '^include' settings.gradle.kts` to confirm; the count grows as `:ui-dialog-features:*` proliferate — currently 25 dialog modules carry most of the weight). For a new project:

- Start with the **infrastructure modules** (everything except the feature folders). All convention plugins, `:shared`, `:design-system:*`, `:ui-core:*`, `:data-services:*` except product-specific ones, `:toolkit:*`, `:data-mappers:*` (empty initially) — these are the foundation.
- Add `:ui-screen-features:screen-api`, `:ui-dialog-features:dialog-api`, `:data-features:feature-api` as empty contract modules.
- Add product feature modules **one at a time** as features are built.

It is OK for `:ui-screen-features:*` and `:data-features:*` to start with fewer modules — they grow with the product. It is **not** OK to skip `:shared`, `:design-system:*`, `:ui-core:*`, `:toolkit:*`, or `:data-services:*` infrastructure — these define the architecture.

## Mandatory infrastructure modules (minimum viable foundation)

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
:data-services:firebase

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

`:iosApp` is an Xcode project, not a Gradle module — see `12-gradle-build/07-ios-swiftpackage.md`.

## Optional modules (add as needed)

| Group | When |
|---|---|
| `:data-services:google-auth` / `:data-services:apple-auth` | Only if the product uses these auth methods |
| `:compose-libs:*` | Add a module per reusable Compose widget that doesn't fit the design system |

## Per-module file convention

Every module's `build.gradle.kts` contains **only**:

1. `plugins { id("...convention") }` lines (plus rare `alias(libs.plugins.kotlin.serialization)` when `@Serializable` types are declared).
2. A `kotlin { ... }` block with `android { namespace = "..." }` and `sourceSets.commonMain.dependencies { ... }`.

No `android { compileSdk = ... }`, no `kotlin { jvmToolchain(...) }`, no manual `apply(plugin = ...)`. Everything is in the convention plugins. A handful of modules also need a top-level `compose.resources { ... }` block (`:design-system:resources:provider`) or an `androidLibrary { androidResources.enable = true }` block inside `kotlin { ... }` (`:design-system:resources:provider`, `:toolkit:notification-manager`) — these are module-specific and stay at the call site rather than in a convention plugin.

See `12-gradle-build/01-convention-plugins.md` for the convention plugin matrix.
