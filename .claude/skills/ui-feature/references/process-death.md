# Process-Death Restoration

The app survives process death (Android: low-memory kill, Don't Keep Activities;
iOS: backgrounded long enough to be reaped). The user returns to **the same
screen, same router stack, same loaders/state where possible** — without
reloading from scratch. This is **not** automatic; it requires care about what is
stored in routers and what is `@Serializable`.

## The mechanism

Decompose's `StateKeeper` serializes the entire router stack to a `Bundle`
(Android) / iOS state archive on process death. On restart the same root component
is reconstructed and the stack deserialized:

```
Process restart → StateKeeper.consume() → router stack rebuilt
  → childStack(serializer = RootRouter.serializer(), ...) restores all configs
  → childFactory(config, ctx) recreates each Component
  → each Component re-creates its ViewModel via retainedInstance { ... }
  → ViewModel.init { } runs again — re-subscribes to Flows, re-fetches if needed
```

## Hard rules

### 1. All `*Router` classes MUST be `@Serializable`

```kotlin
@Serializable
public sealed class RootRouter : BaseRouter {
    @Serializable public data object Home : RootRouter()
    @Serializable public data class Profile(val value: ProfileRouter) : RootRouter()
}
```

Forgetting `@Serializable` on a subtype crashes at runtime when `kotlinx-serialization` writes the stack.

### 2. All payloads in routes MUST be `@Serializable`

`DateRange`, `StageState`, all `*FormatState`, `DateFormat`, `DateRangeKind` — all **are** `@Serializable` in this project. `UiText` is **not** (`@Stable` only — it wraps a non-serializable `StringResource` plus `ImmutableList<Any>` args), so don't put a `UiText` in a route payload. Resolve it to a `String`/`Res` key before navigating, or keep it inside `State` (rebuilt in `init { }`, not restored from the StateKeeper).

### 3. No lambdas in route configs

```kotlin
// FORBIDDEN
@Serializable data class BadRoute(val onResult: (Foo) -> Unit) : SomeRouter()
```

Lambdas don't serialize. If a route needs a result-back path, use **`ResultManager`** (`references/results.md`) — sender is the producing screen; consumer subscribes in `init { }`.

### 4. All `DialogConfig` subtypes MUST be `@Serializable`; callbacks MUST be `@Transient`

```kotlin
@Serializable
public data class AmountPicker(
    val initial: Double?,
    @Transient public val onResult: (Double) -> Unit = { },
) : DialogConfig() {
    override val key: String get() = buildKey("AmountPicker", initial)
}
```

After process death the dialog is restored with the saved `initial` but the `onResult` lambda is the default no-op. The picker still functions; selecting a value just doesn't propagate back. Intentional.

### 5. ViewModel state is NOT restored automatically

`BaseViewModel` does **not** use `SavedStateHandle`. State is rebuilt in `init { }` via re-subscription to data-layer `Flow`s:

```kotlin
init {
    notesFeature.observeNotes(state.value.range.from, state.value.range.to)
        .map { it.toState() }
        .onEach { items -> update { it.copy(items = items) } }
        .safeLaunch()

    safeLaunch(loader = NotesListLoader.LoadingNotes) {
        notesFeature.getNotes(state.value.range.from, state.value.range.to).getOrThrow()
    }
}
```

The first `onEach` populates the list immediately from Room cache (no network); the `safeLaunch` refreshes it. On a healthy connection the screen appears "instantly populated".

### 6. Range/filter state lives in the route, not the ViewModel

If a screen has a date-range filter and the user backgrounds with "Last 30 days", it MUST restore to "Last 30 days". **Solution:** put the range in the `<Feature>Router.<Screen>` config (`NoteArchive(val initialRange: DateRange)`); the component pulls it from the config and threads it into the ViewModel via `retainedInstance { <X>ViewModel(initialRange, ...) }`, which stores it in initial state.

When the user changes the range, the ViewModel updates `state` but does **not** push a new route — so on restart the screen restores to `initialRange`, not the last-selected range. If the last-selected range must be restored, call `navigation.replaceCurrent(ProfileRouter.NoteArchive(initialRange = newRange))` on every change (makes the stack the source of truth). **Use sparingly** — frequent `replaceCurrent` floods the StateKeeper.

### 7. Ephemeral selection state can be lost (SHOULD-accept)

A multi-select picker doesn't usually persist intermediate selection through process death — the user re-opens and re-selects. Acceptable. If preservation is needed, store the selection in the router (accept the StateKeeper cost).

## `retainedInstance` and config changes

`retainedInstance` retains a ViewModel across **configuration changes** (rotation, theme) **without** process death — same instance, `init { }` does not re-run. On **process death** the retained instance is discarded; the Component is reconstructed via the router stack and `retainedInstance { ... }` creates a fresh ViewModel, re-running `init { }`.

## iOS specifics

iOS apps can be terminated after long backgrounding. The same StateKeeper mechanism applies (serialized via a Kotlin/Native buffer persisted to disk by the Decompose iOS lifecycle integration). `iosApp/iOSApp.swift` wires the lifecycle: `applicationDidEnterBackground` → save; `applicationWillEnterForeground` → restore. The exact mechanism depends on the `iosMain/RootViewController.kt` glue.

## What "Don't Keep Activities" tests

Enable it in Developer Options, then: navigate deep → press Home → wait a few seconds → re-open. The user **must** return to the same screen with the same `initialRange` selected, the same data visible (from Room cache), and the same dialog open. If it breaks, check router serialization (a route or payload is missing `@Serializable`).

## Anti-patterns (MUST avoid)

- **Storing user input in a transient ViewModel `var` outside `state`.** It will be lost. Use `update { it.copy(...) }` only.
- **Caching network responses in ViewModel memory.** Cache in Room (DAO), observe from there.
- **Putting non-`@Serializable` types in routes.** Crash on backgrounding.
- **Relying on dialog callbacks to persist after process death.** They're `@Transient`. Persist the value in the route or use `ResultManager`.
- **Implementing custom save/restore in `BaseViewModel`.** Restore via `Flow` subscription in `init { }`.
- **Forgetting `key = "..."`** in `childStack(...)` — duplicate default keys collide in the StateKeeper.
