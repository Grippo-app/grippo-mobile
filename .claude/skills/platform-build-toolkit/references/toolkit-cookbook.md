# Cookbook — extend or add a `:toolkit:*` module

The toolkit-module recipe: when to use a toolkit module, how to extend an existing one (A), how to add a new one (B), how to verify, and the common mistakes.

Toolkit modules are **product-agnostic, platform-aware utilities** at the bottom of the dependency graph. This recipe covers the two situations: **A — extend an existing `:toolkit:<name>` module** (the common case) and **B — add a new one** (rare; check part 0 first).

> **Concrete example.** `clipboard` (new module) and `DateRange` (extension) below are illustrative; the steps apply to any platform-aware utility.

## 0. Is toolkit the right home?

| The code is… | Home |
|---|---|
| A cross-cutting, product-agnostic utility that needs platform APIs (clock, clipboard, connectivity, permissions, logging, locale) | `:toolkit:<name>` — this recipe |
| Talking to the backend, the database, or an SDK that carries domain meaning | `:data-services:*` — NOT toolkit |
| A UI primitive, token, or themed widget | `:design-system:*` |
| An error/state type shared between data and UI | `:ui-core:*` |
| Needed by exactly one feature | keep it inside that feature module — do not generalize prematurely |

Hard boundaries (from dependency rule 5 and `toolkit-modules.md` § Rules):

- Toolkit MUST NOT depend on `:data-features:*`, `:data-services:*`, `:ui-screen-features:*`, or `:ui-dialog-features:*`.
- The two tolerated pure-type exceptions (`:toolkit:http-client` → `:ui-core:error:error-provider`; `:toolkit:date-utils` → `:design-system:resources:provider` + `:design-system:core`) are a **closed list** — do not add a third.
- **No domain types.** No `Note`, no `User`, no product vocabulary anywhere in toolkit code or names.
- `:toolkit:logger` is the conventional logging dependency — depend on it instead of inventing per-module logging.

## A. Extend an existing toolkit module

### 1. Read the module's reference

Each module is documented in `toolkit-modules.md` (e.g. date-utils, logger) describing its public surface and conventions. The new API must read as if it always belonged there.

### 2. Add the API in `commonMain`

Public surface lives in `src/commonMain/kotlin/com/<org>/<product>/toolkit/<name>/`; private helpers go under `internal/` (convention, not a visibility modifier — still mark them `internal` or `private`).

```kotlin
// :toolkit:date-utils — illustrative extension
public fun DateRange.shiftBy(days: Int): DateRange = DateRange(
    from = from.plus(days, DateTimeUnit.DAY),
    to = to.plus(days, DateTimeUnit.DAY),
)
```

### 3. Keep expect/actual parity in the SAME change

A new `expect` declaration in `commonMain` lands its `actual` in **both** `androidMain` and `iosMain` before the task is done. For `interface + per-platform impl` services, extend both impls. A missing `actual` fails the cross-platform build — never ship half.

### 4. DI only if the module already has it

If the module declares `<Service>Module.kt`, register new bindings there. Bare `expect/actual` helpers (`AppTheme.current`, `NativeContext`) need no DI — do not introduce a Koin module just for a new function.

### 5. Update the consumers named in the task

Toolkit changes are usually motivated by a consumer (a screen, a repository). Wire the call sites listed in the task — and nothing beyond them.

## B. Add a new toolkit module

### 1. Confirm no existing module covers it

Check the inventory table in `toolkit-modules.md` § "Modules at a glance". Extending an existing module beats creating a sibling (`date-utils` gets new date API; a second `date-helpers` module is a defect).

### 2. Create the layout

```
:toolkit:<name>/
  build.gradle.kts
  src/
    commonMain/kotlin/com/<org>/<product>/toolkit/<name>/
      <Service>.kt                   // public interface or expect class
      <Service>Module.kt             // Koin module — ONLY if the service is interface+impl with state
      internal/                      // private impls
    androidMain/kotlin/com/<org>/<product>/toolkit/<name>/
      <Service>.android.kt
    iosMain/kotlin/com/<org>/<product>/toolkit/<name>/
      <Service>.ios.kt
```

### 3. Register the module

In root `settings.gradle.kts`, inside the toolkit group (keep the group's ordering style):

```kotlin
include(":toolkit:<name>")
```

### 4. Write `build.gradle.kts`

Start from the representative block in `toolkit-modules.md` § Build:

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    // koin.annotation.convention — ONLY if the module declares a Koin module
    // alias(libs.plugins.kotlin.serialization) — ONLY if it declares @Serializable types
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.<name>" }

    sourceSets.commonMain.dependencies {
        // minimal — only the libraries the toolkit module wraps
    }
}
```

A new third-party library here is a **new dependency** — it needs explicit authorization in the task text (see the validation-gates skill, references/when-to-stop-and-ask.md), not a silent version-catalog addition.

### 5. Pick the implementation shape

Per the table in `toolkit-modules.md` § "Modules at a glance": `expect/actual` for small same-type helpers; `interface + Android impl + iOS impl` for services with several methods and meaningful state.

```kotlin
// expect/actual — illustrative clipboard helper
public expect class Clipboard(context: NativeContext) {
    public fun copy(text: String)
}
```

### 6. Wire DI (only for interface+impl services)

`<Service>Module.kt` with `@Module` + `@ComponentScan`, then register in `Koin.init` next to the other toolkit modules (`ConnectivityModule`, `LinkOpenerModule`, …) per the di-modules skill, references/composition-root.md:

```kotlin
ClipboardModule().module,
```

### 7. Wire at least one consumer

The task that motivated the module supplies the first call site. A toolkit module with zero consumers is dead weight — if the consumer intentionally lands in a later task, say so explicitly in the task outcome.

## Verify

```bash
./gradlew :toolkit:<name>:assemble
IOS_ENABLED=$(rg -m1 '^iosEnabled:' orchestrator/project-config.md | awk '{print $2}')
if [ "$IOS_ENABLED" = "true" ]; then
  IOS_FW=$(rg -m1 '^iosFrameworkName:' orchestrator/project-config.md | awk '{print $2}')
  IOS_FW=${IOS_FW:-shared}
  IOS_FW_PASCAL=$(echo "$IOS_FW" | awk '{print toupper(substr($0,1,1)) substr($0,2)}')
  ./gradlew ":shared:assemble${IOS_FW_PASCAL}DebugXCFramework"
fi
./gradlew :androidApp:assembleDebug
```

All gates (or just the module + `:androidApp` when `iosEnabled: false`) must build green — the iOS gate is what catches a missing `actual`.

## Common mistakes

- `actual` added on one platform only — the XCFramework gate fails late. Add both in the same change.
- A domain type sneaking in (`NoteFormatter` belongs in the feature or `:ui-core:state`, never toolkit).
- A new module duplicating an existing capability — check the `toolkit-modules.md` § "Modules at a glance" table first.
- Extending the rule-5 exception list "just this once" — it is closed.
- Wrapping an SDK with domain meaning as a toolkit module — that is a `:data-services:*` module.
- Forgetting `include(":toolkit:<name>")` — Gradle fails with "project not found" at the first consumer.
- A Koin module for a stateless `expect/actual` helper — needless indirection; toolkit DI is for stateful interface+impl services.
