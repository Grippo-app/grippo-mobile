# `:toolkit:logger` — `AppLogger`

`AppLogger` is the **only** logging facility in the project. Four categories, file-backed, accessible from a debug screen.

## Object signature

```kotlin
public object AppLogger {
    public fun logFileContentsByCategory(): Map<String, List<String>>
    public fun clearLogFile(): Boolean

    public object General {
        public fun error(msg: String): Unit
        public fun warning(msg: String): Unit
    }

    public object Navigation {
        public fun log(msg: String): Unit
    }

    public object Network {
        public fun log(msg: String): Unit
    }

    public object Mapping {
        public fun <T> log(value: T?, msg: () -> String): T?
    }
}
```

## Categories

| Category | Used by |
|---|---|
| `General` | ViewModels, Repositories, the error pipeline (`AppLogger.General.error(exception.stackTraceToString())`) |
| `Navigation` | Decompose-related logs (route push/pop), `RootViewModel` deeplinks |
| `Network` | `ClientLogger` routes Ktor `Logging` here |
| `Mapping` | DTO → Entity / Domain mappers' null logging |

Each category writes to the same file but tags the line with the category name. The debug screen filters by category.

## `Mapping.log` — the null-tracker

```kotlin
public fun <T> log(value: T?, msg: () -> String): T? {
    if (value == null) {
        // file-write "[MAPPING] $msg (file:line)"
    }
    return value
}
```

- Returns the input value verbatim — no side effect on the value flow.
- Logs only when the value is null.
- The `msg` lambda is **lazy** — only invoked when needed.
- Used in `:data-mappers:dto-to-entity` and `:data-mappers:dto-to-domain` for required-field checks. See `07-mappers/03-null-safety.md`.

## File location

| Platform | Path |
|---|---|
| Android | `<System.getProperty("user.home")>/<product>/logs/app.log`, falling back to `<java.io.tmpdir>/<product>/logs/app.log` or `/tmp/<product>/logs/app.log` |
| iOS | `NSTemporaryDirectory()/<product>/logs/app.log` |

The Android implementation reads the JVM `user.home` property and only falls back to `tmpdir` if that's blank. On iOS the file lives in the temporary directory — survives app restarts but the system may purge it under storage pressure.

## File rotation

The current `LogFileWriter` appends without size-based rotation; `AppLogger.clearLogFile()` is the explicit reset path (it deletes the file and re-creates the writer).

## Public read API

```kotlin
public fun AppLogger.logFileContentsByCategory(): Map<String, List<String>>
```

Returns the current log file's contents grouped by category. Used by:

- The debug screen — shows recent logs.
- Bug reports — attach the log file.

```kotlin
public fun AppLogger.clearLogFile(): Boolean
```

Empties the log file. Returns `true` on success.

## Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.logger" }

    sourceSets.commonMain.dependencies {
        implementation(libs.datetime)
    }
}
```

No `koin.annotation.convention` — `AppLogger` is a plain `object` accessed statically, not a `@Single`. No dependency on `:toolkit:context` either; the file path is resolved through JVM/`NSFileManager` system APIs inside the platform `actual`s.

## Usage

### In a ViewModel

```kotlin
// not typical — errors flow through the pipeline automatically
AppLogger.General.warning("User attempted to save with invalid form")
```

The error pipeline calls `AppLogger.General.error(...)` automatically; explicit calls are rare and reserved for unusual diagnostic needs.

### In a Mapper

```kotlin
val id = AppLogger.Mapping.log(dto.id) { "TrainingResponse.id is null" } ?: return null
```

### In `ClientLogger` (Ktor's `Logger`)

```kotlin
@Single
internal class ClientLogger : Logger {
    override fun log(message: String) {
        // ... emoji formatting
        AppLogger.Network.log("$emojiLine HTTP LOG $emojiLine\n$message")
    }
}
```

### Navigation

`RootViewModel` may log when a deeplink fires or when a back navigation cascades:

```kotlin
AppLogger.Navigation.log("Deeplink: ${deeplink.key}")
```

Used sparingly. Default is no Navigation logs — `Navigation` category is opt-in for specific flows.

## Why a single logger object

- **Visible accross all layers.** A Mapper, a ViewModel, and the network layer all use the same call.
- **Centralized file write.** No race conditions between competing writers.
- **Multiplatform.** Built on top of `expect/actual` file handles.

`println(...)` and platform-specific loggers (`android.util.Log`, `NSLog`) are forbidden in production code. Local debugging via `println` is fine while developing — but remove before committing.

## Rules

- **`AppLogger.General.error(...)` is invoked by `BaseViewModel.sendError` automatically.** Don't double-log.
- **Don't log PII.** Truncate access tokens (`bearer.take(25)`), hash user IDs, omit emails / passwords entirely.
- **Don't log inside tight loops** — use `Mapping.log` only at boundary translations.
- **Don't depend on logs for correctness** — logging is observability, not a side-channel for state.

## Anti-patterns

- **`println(...)`** in production code.
- **`System.err.println(...)`** — won't work on iOS.
- **`android.util.Log.e(...)`** in `commonMain` — won't compile.
- **`AppLogger.General.error(...)` after a Ktor exception** — the error pipeline does this. Duplicate Firebase reports.
- **Logging request/response bodies in release builds** without scrubbing.
