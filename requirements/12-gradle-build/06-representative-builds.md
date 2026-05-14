# Representative Module Build Files

A `build.gradle.kts` template per module category. Use these as the starting point for new modules.

## Pure KMP module (no UI, no Koin, e.g. a small toolkit)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.<name>" }

    sourceSets.commonMain.dependencies {
        // minimal — only the libraries this toolkit module wraps
        implementation(libs.kotlinx.coroutines.core)
    }
}
```

## KMP module with Koin DI (e.g. a `:data-features:<feature>`)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.data.features.<feature>" }

    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.database)
        implementation(projects.dataServices.backend)
        implementation(projects.dataMappers.dtoToEntity)
        implementation(projects.dataMappers.entityToDomain)
        implementation(projects.dataMappers.domainToDto)
        implementation(projects.toolkit.dateUtils)

        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.datetime)
    }
}
```

## KMP module with Compose UI (no DI, e.g. `:design-system:components`)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.design.system.components" }

    sourceSets.commonMain.dependencies {
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.preview)

        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(libs.immutable.collections)
    }
}
```

## KMP module with Compose UI and Koin (e.g. `:ui-screen-features:<feature>`)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.ui.screen.features.<feature>" }

    sourceSets.commonMain.dependencies {
        implementation(projects.uiCore.foundation)
        implementation(projects.uiCore.state)
        implementation(projects.uiScreenFeatures.screenApi)
        implementation(projects.uiDialogFeatures.dialogApi)
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataMappers.domainToState)
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.components)
        implementation(projects.designSystem.preview)
        implementation(projects.toolkit.dateUtils)

        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(libs.immutable.collections)
        implementation(libs.datetime)
    }
}
```

## Dialog feature module (e.g. `:ui-dialog-features:<picker>`)

Same as the screen feature but typically without `:data-features:feature-api`:

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.ui.dialog.features.<feature>" }

    sourceSets.commonMain.dependencies {
        implementation(projects.uiCore.foundation)
        implementation(projects.uiCore.state)
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.components)
        implementation(projects.designSystem.preview)
        implementation(projects.composeLibs.wheelPicker)   // if it's a picker that uses the wheel

        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(libs.immutable.collections)
    }
}
```

## Data service module with `@Serializable` types (e.g. `:data-services:backend`)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android { namespace = "com.<org>.<product>.data.services.backend" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.toolkit.serialization)
            implementation(projects.toolkit.httpClient)
            implementation(projects.toolkit.logger)
            implementation(projects.toolkit.localization)
            implementation(projects.dataServices.database)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.auth)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.content.negotiation)
        }
    }
}
```

## Data mapper module (e.g. `:data-mappers:dto-to-entity`)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.data.mappers.dto.to.entity" }

    sourceSets.commonMain.dependencies {
        implementation(projects.dataServices.database)
        implementation(projects.dataServices.backend)
        implementation(projects.toolkit.logger)
    }
}
```

## Toolkit `date-utils` module (Compose + Serialization)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.date.utils" }

    sourceSets.commonMain.dependencies {
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.core)
        implementation(projects.toolkit.logger)

        implementation(compose.foundation)
        implementation(libs.datetime)
        implementation(libs.kotlinx.serialization.json)
    }
}
```

## Room database module (`:data-services:database`)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("room.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.data.services.database" }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.context)
        implementation(projects.toolkit.logger)
        implementation(projects.uiCore.error.errorProvider)
    }
}
```

The `room.convention` plugin handles all Room+KSP wiring for Android and every iOS target.

## Compose-libs widget module (e.g. `:compose-libs:wheel-picker`)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.compose.libs.<name>" }

    sourceSets.commonMain.dependencies {
        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(libs.immutable.collections)
    }
}
```

No Koin. No `:design-system:*`. No `:toolkit:*` (except trivial cases). Self-contained widget.

## Patterns

### Module-level `android { namespace = ... }`

```kotlin
kotlin {
    android { namespace = "com.<org>.<product>.<group>.<name>" }
}
```

The `kotlin { android { ... } }` block is the KMP-style configuration. The convention plugin's default `namespace = "com.<org>.<product>"` is overridden here with the full per-module namespace.

### Source-set grouping

For platform-specific deps:

```kotlin
sourceSets {
    commonMain.dependencies { ... }
    androidMain.dependencies {
        implementation("androidx.appcompat:appcompat:1.7.0")
    }
    iosMain.dependencies {
        // iosMain isn't a direct source set; iOS targets inherit
    }
}
```

Most modules only need `commonMain.dependencies`. Android-specific deps go in `androidMain.dependencies`. iOS deps usually come via the platform engine (e.g. `ktor-client-darwin` in `iosMain.dependencies` for `:toolkit:http-client`).

## Per-module checklist

When you create a new module:

- [ ] Module directory exists under the right group.
- [ ] `build.gradle.kts` uses the right convention plugins.
- [ ] `kotlin { android { namespace = "..." } }` is set to the full namespace.
- [ ] `sourceSets.commonMain.dependencies { ... }` lists only what the module actually uses.
- [ ] No redundant deps.
- [ ] No `repositories { ... }` block (forbidden by `FAIL_ON_PROJECT_REPOS`).
- [ ] No version literals (`"2.3.21"`). Use the catalog (`libs.kotlin.gradle.plugin`).
- [ ] Module added to `settings.gradle.kts`.
- [ ] If declares Koin module, added to `:shared/Koin.kt`.
- [ ] If consumed by `:shared`, added to `:shared/build.gradle.kts`.

## Anti-patterns

- **`apply(plugin = "...")`** instead of `plugins { id("...") }`. Use the `plugins {}` DSL.
- **Inline version literals**: `implementation("org.foo:bar:1.0.0")`. Use the catalog.
- **`api(...)` everywhere.** Default is `implementation`; `api` only for genuine API exposure.
- **Compose plugins applied directly** in a module that uses Compose. Use `compose.multiplatform.convention`.
- **Kotlin serialization plugin without `@Serializable` types** in the module. Don't apply the plugin if it's unused.
- **`kotlin { jvmToolchain(19) }`** in a module. Already in the convention.
- **`compileSdk = 36`** in a module. Already in the convention.
