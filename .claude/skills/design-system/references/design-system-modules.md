# `:design-system:*` modules

The design system is the visual identity layer. It is **pure UI** — it does not depend on
the data layer, the navigation layer, or any feature module. It produces tokens and atomic
components consumed by `:ui-screen-features:*` and `:ui-dialog-features:*`.

## Module list

| Module | Purpose | Convention plugins |
|---|---|---|
| `:design-system:core` | `AppTokens` facade + `AppTheme` composable | KMP + Compose |
| `:design-system:components` | Atomic Composables (`Button`, `Toolbar`, `Input*`, charts, ...) | KMP + Compose |
| `:design-system:preview` | `@AppPreview` annotation + `PreviewContainer` wrapper | KMP + Compose |
| `:design-system:resources:provider` | `Res.*` (Compose Multiplatform Resources) + interfaces (`AppColor`, `AppDp`, `AppTypography`, `AppString`, `AppDrawable`, `AppIcon`) | KMP + Compose |
| `:design-system:resources:provider-impl` | `ResourcesProviderModule` (Koin) + `StringProviderImpl` | KMP + Compose + Koin |

## Internal dependency graph

```
:design-system:components
        ↓
:design-system:core   (provides AppTokens reading from CompositionLocals)
        ↓
:design-system:resources:provider   (defines AppColor/AppDp/.../StringProvider interfaces, owns Res)
        ↑
:design-system:resources:provider-impl   (Koin module + StringProviderImpl)
        ↑
        :shared (composition wires the impl)
```

`:design-system:preview` depends on `:design-system:core` (to use `AppTheme` in previews).

## `:design-system:core`

Exposes:

- `AppTokens` — the `@Stable public object AppTokens` with `colors`, `icons`, `typography`,
  `strings`, `drawables`, `dp` properties backed by `LocalApp*` CompositionLocals.
- `AppTheme(darkTheme, localeTag, content)` — the root composable that provides every
  `Local*` CompositionLocal via `ProvideResources`.

See `tokens.md` and `theme.md` for the full contracts.

### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.design.system.core" }

    sourceSets.commonMain.dependencies {
        implementation(projects.designSystem.resources.provider)
        implementation(compose.foundation)
        implementation(compose.runtime)
    }
}
```

`:design-system:core` consumes `:design-system:resources:provider` via `implementation` —
`Res`, `AppColor`, `AppDp` etc. are referenced internally by `AppTokens`/`AppTheme` and
re-exposed via `CompositionLocal`s, not transitively. Consumers that need `Res.*` directly
add their own `implementation(projects.designSystem.resources.provider)`.

## `:design-system:components`

Houses atomic UI components: `Button`, `Toolbar`, `BottomSheetToolbar`,
`BottomOverlayContainer`, `Input*`, `Chip`, `EmptyState`, `BannerCard`, `LineIndicator`, plus
product-specific composites built on these primitives (`<Entity>Chart`, `<Entity>HistoryCard`,
`<Entity>Heatmap`, etc.).

**Every** screen-side composable that is reused across features lives here. Material3
primitives are wrapped — UI code never imports `androidx.compose.material3.Button` directly
(only design-system components import Material3).

Rules:

- Component composables are `public`.
- Receive an explicit `modifier: Modifier = Modifier` parameter.
- Read tokens via `AppTokens.<...>` — never hardcode `Color(0xFF...)`, `12.dp`, or `sp`.
- Receive callbacks as `(...) -> Unit`, never via reflection or contracts.
- Stateless where possible; state lives in the caller's `State`/`ViewModel`.

### Build

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
        implementation(projects.composeLibs.segmentControl)
        implementation(projects.composeLibs.konfetti)
        implementation(projects.composeLibs.chart)
        implementation(projects.uiScreenFeatures.screenApi)   // for shared route payload types referenced by component params
        implementation(projects.uiCore.state)                  // for State/UiText types passed into composables
        implementation(projects.toolkit.dateUtils)             // for date axis/chart helpers

        implementation(compose.foundation)
        implementation(compose.runtime)
        implementation(compose.material3)

        implementation(libs.immutable.collections)
        implementation(libs.coil.compose)
        implementation(libs.coil.network.ktor)
        implementation(libs.datetime)
    }
}
```

Note: `:design-system:components` deliberately depends on
`:ui-screen-features:screen-api` and `:ui-core:state` — both are pure-type modules with no
UI logic of their own, so they fit the "pure-type back-edge" exemption described in
the platform-build-toolkit skill, references/module-structure.md. Do not extend that exemption to
feature implementation modules.

## `:design-system:preview`

Exposes:

- `@AppPreview` — a multi-preview annotation that expands to two `@Preview` configs (phone in
  a non-default locale small, phone `en` big).
- `PreviewContainer(content)` — wraps content in `AppTheme(darkTheme = true)` + a Coil
  preview handler + a padded `Column`.

See `previews.md` for verbatim code.

### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.design.system.preview" }

    sourceSets.commonMain.dependencies {
        implementation(projects.designSystem.core)
        implementation(compose.foundation)
        implementation(compose.components.uiToolingPreview)
        implementation(libs.coil.compose)
    }
    sourceSets.androidMain.dependencies {
        implementation(compose.uiTooling)
    }
}
```

`compose.uiTooling` is androidMain-only because the JetBrains Compose tooling artifact pulls
in Android-only inspector code.

## `:design-system:resources:provider`

Houses:

- The Compose Multiplatform Resources tree (`commonMain/composeResources/values/strings.xml`,
  drawables, fonts).
- `AppColor`, `AppDp`, `AppTypography`, `AppString`, `AppDrawable`, `AppIcon` interfaces/objects.
- `LocalAppColors`, `LocalAppIcons`, ... `CompositionLocal` declarations.
- `StringProvider` interface.

This module is **public** — every UI module imports it directly or transitively via
`:design-system:core`.

### Compose Resources structure

```
:design-system:resources:provider/
  src/commonMain/composeResources/
    values/strings.xml              // English (default)
    values-<locale>/strings.xml     // one folder per non-default locale
    values/plurals.xml              // optional; created on first plural
    drawable/                       // .webp raster drawables; vector XML/SVG when appropriate
    font/                           // Manrope or product font files
```

Strings are accessed as `Res.string.<key>`. Drawables as `Res.drawable.<key>`. Plurals as
`Res.plurals.<key>`. **Do not** use `androidx.compose.ui.res.stringResource` or
`painterResource(R.drawable...)` — those are Android-only and break the multiplatform
contract.

### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.<org>.<product>.design.resources.provider"
    generateResClass = always
}

kotlin {
    android { namespace = "com.<org>.<product>.design.system.resources.provider" }

    androidLibrary { androidResources.enable = true }   // mandatory — packages composeResources into the APK

    sourceSets.commonMain.dependencies {
        api(compose.components.resources)
        implementation(compose.foundation)
        implementation(compose.materialIconsExtended)
    }
}
```

The `compose.resources` block is required: `publicResClass = true` exposes the generated
`Res.*` class to consumers, `packageOfResClass` controls its FQN, and
`generateResClass = always` ensures the class is generated even when only `expect`-style
resource usage is detected.

The `androidLibrary { androidResources.enable = true }` line is **also mandatory**, for a
different reason: with AGP 9 + `com.android.kotlin.multiplatform.library`, Compose Resources'
`CopyResourcesToAndroidAssetsTask` is skipped unless Android resources are enabled on the
module, so the generated `composeResources` (`.cvr`) never reach the APK and the app crashes
at runtime with `MissingResourceException`. It stays at the call site (not in
`android.library.convention`) because most KMP modules ship no Android resources — full
rationale in `resources.md` § Build requirement.

## `:design-system:resources:provider-impl`

Houses:

- `ResourcesProviderModule` — Koin module that provides `StringProvider` (via
  `StringProviderImpl`) and any other resource-related singletons.
- `StringProviderImpl` (`internal`) — uses `org.jetbrains.compose.resources.getString` /
  `getPluralString` under the hood.

Only `:shared` depends on this module (to register the Koin module). UI modules consume
`StringProvider` via `getKoin().get<StringProvider>()` or by constructor injection in their
ViewModels.

### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.design.system.resources.provider.impl" }

    sourceSets.commonMain.dependencies {
        implementation(projects.designSystem.resources.provider)
        implementation(projects.toolkit.theme)
    }
}
```

Notes:
- This module does **not** apply `compose.multiplatform.convention` — `StringProviderImpl`
  calls into `org.jetbrains.compose.resources.getString(...)` (a suspend function), not into
  `@Composable` APIs, so the Compose Compiler is unnecessary here.
- `:toolkit:theme` is imported because the implementation needs to read the current theme to
  resolve resource variants for the system locale.
- `compose.components.resources` is pulled in transitively through
  `:design-system:resources:provider` (which `api(...)`-exposes it).

## Rules summary

- `AppTokens` is the **only** way Composables read design values.
- `StringProvider` is the **only** way ViewModels read strings.
- `Res.string.*` / `Res.drawable.*` / `Res.plurals.*` is the **only** way to reference
  resources.
- Material3 components are imported only inside `:design-system:components` — outside this
  module, the design-system wrappers are used.
- Adding a new token (a new color, a new dp, a new typography style) requires editing
  `:design-system:resources:provider` and possibly `provider-impl`. Adding a new token
  without a real consumer is forbidden — tokens are demand-driven.
