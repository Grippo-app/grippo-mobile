# `:toolkit:*` — Overview

The toolkit holds **platform-aware utilities** sitting at the bottom of the dependency graph. Every other layer (UI, data features, design system) may depend on toolkit modules. The toolkit may only depend on **other toolkit modules**, with two narrow exceptions in the reference repo: `:toolkit:http-client` reads `AppError` from `:ui-core:error:error-provider`, and `:toolkit:date-utils` reads localized weekday/month resources from `:design-system:resources:provider` + `:design-system:core`. No toolkit module depends on `:data-services:*`.

## Modules at a glance

| Module | Purpose |
|---|---|
| `:toolkit:context` | `NativeContext` (expect/actual platform handle) |
| `:toolkit:http-client` | Base Ktor `HttpClient` + `ApiErrorParser` |
| `:toolkit:serialization` | `Json` provider |
| `:toolkit:logger` | `AppLogger` (file-backed, categorized) |
| `:toolkit:date-utils` | `DateTimeUtils`, `DateRange`, `DateRangeKind`, `DateRangePresets`, `DateFormat`, `DateFormatting` |
| `:toolkit:theme` | `AppTheme.current` — system dark/light theme |
| `:toolkit:localization` | `AppLocale.current` — system BCP-47 language tag |
| `:toolkit:connectivity` | `Connectivity.statusUpdates` — online/offline |
| `:toolkit:notification-manager` | Local notifications (`AppNotification`, `NotificationKey`) |
| `:toolkit:permission-manager` | System permission requests |
| `:toolkit:link-opener` | Open URLs in system browser |
| `:toolkit:image-loader` | Coil 3 + Ktor 3 image loader |

## Shape of a toolkit module

```
:toolkit:<name>/
  build.gradle.kts                  // KMP convention + Koin if it declares a module
  src/
    commonMain/kotlin/com/<org>/<product>/toolkit/<name>/
      <Service>.kt                  // public interface or expect class
      <Service>Module.kt             // public Koin module (optional)
      internal/                      // private impls (convention only)
    androidMain/kotlin/com/<org>/<product>/toolkit/<name>/
      <Service>.android.kt           // actual or impl class
    iosMain/kotlin/com/<org>/<product>/toolkit/<name>/
      <Service>.ios.kt
```

## When to use `expect/actual` vs `interface + per-platform impl`

| Choice | When |
|---|---|
| `expect/actual` | Small helpers, single-method classes (e.g. `NativeContext`, `AppTheme.current`, `AppLocale.current`). Type is the same on both platforms; only the body differs. |
| `interface + Android impl + iOS impl` | Large services with multiple methods and meaningful state (e.g. `Connectivity`, `NotificationManager`, `PermissionManager`). |

The reference repo uses a mix; for the new project, pick the right tool per service.

## Common patterns

### Empty iOS implementation

When a service is Android-only by intent (e.g. Firebase Analytics is set up natively on iOS in Swift), expose an interface in `commonMain` and implement it as a no-op on iOS:

```kotlin
// commonMain
public interface FirebaseAnalyticsService {
    public fun logEvent(name: String, params: Map<String, String>)
}

// iosMain
internal class IosFirebaseAnalytics : FirebaseAnalyticsService {
    override fun logEvent(name: String, params: Map<String, String>) {
        // no-op; iOS uses native SDK
    }
}
```

### Lazy platform setup

```kotlin
// :toolkit:notification-manager
public interface NotificationManager {
    public fun show(notification: AppNotification, delay: Duration = Duration.ZERO): NotificationKey
    public fun cancel(id: NotificationKey)
    public suspend fun isPending(id: NotificationKey): Boolean
}
```

Implementation registers the channel/notification center lazily on first `show(...)`. Avoids cold-start cost.

### File-backed services

`AppLogger` writes to an append-only file. `:toolkit:logger` keeps the writer behind an internal `expect class`:

```kotlin
internal expect class LogFileWriter {
    val location: String
    fun append(text: String)

    companion object {
        fun create(fileName: String = DEFAULT_FILE_NAME): LogFileWriter
        fun deleteFile(path: String): Boolean
        val DEFAULT_FILE_NAME: String
    }
}
```

The platform `actual` resolves the base directory inside a private `resolveBaseDirectory()` helper (no separate `LogFileLocation` type) and returns a writer pointed at `<baseDir>/<fileName>`. The `LogDispatcher` in `commonMain` decides which category each entry belongs to and asks the writer to `append(text)` — the writer itself is category-agnostic.

## Rules

1. **No business logic.** Toolkit is utility-only. No domain types (`Note`, `Tag`).
2. **No design-system dependency.** Toolkit doesn't read `AppTokens`.
3. **No data-feature dependency.** Toolkit doesn't import `<X>Feature`.
4. **No cross-feature toolkit imports.** `:toolkit:notification-manager` doesn't import `:toolkit:date-utils` unless it really needs to.
5. **Public interfaces** — every consumer-facing type is `public`. Implementations are `internal`.
6. **Koin module per service** — `<Name>Module` registered in `:shared/Koin.kt`.

## When to create a new toolkit module

A new module is justified when:

- The service has platform-specific impls.
- The service is consumed by **multiple** layers (UI + data + screen).
- The service is **infrastructure** (timing, IO, system services) not business logic.

Don't create a toolkit module for:

- A single feature's helper. Put it in the feature's module.
- A wrapper around a single Kotlin stdlib feature. Use stdlib directly.
- A "string utilities" module. Use Kotlin's stdlib extensions.

## Where each toolkit module sits in the graph

```
:design-system:* / :ui-core:* / :data-features:* / :data-services:*
        ↓ ↓ ↓ ↓
:toolkit:*           (depends only on other :toolkit:* — plus the two narrow
                      exceptions noted above: :toolkit:http-client uses
                      :ui-core:error:error-provider; :toolkit:date-utils uses
                      :design-system:core + :design-system:resources:provider)
        ↓
(kotlinx stdlib, Ktor, Room, Coil, etc.)
```

A toolkit module's `build.gradle.kts` typically imports a few sibling `:toolkit:*` modules and the library it wraps.

## Anti-patterns

- **Toolkit module that depends on `:design-system:*`.** Forbidden.
- **Toolkit module that depends on `:data-features:*`.** Forbidden.
- **Toolkit module that depends on `:ui-core:*`.** Forbidden.
- **Toolkit module with `@Composable` functions** that read `AppTokens`. Compose Composables in a toolkit module is fine; reading design tokens is not.
- **Concrete platform classes in `commonMain`.** Use `expect/actual` or `interface + per-platform impl`.
- **Static accessors in `commonMain`** for things that vary by platform (`Context`, `NSBundle`). Wrap behind `NativeContext` from `:toolkit:context`.
