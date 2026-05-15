# Process-Death Restoration

The app survives process death (Android: low-memory kill, Don't Keep Activities; iOS: backgrounded long enough to be reaped). The user returns to **the same screen, same router stack, same loaders/state where possible** — without the app reloading from scratch.

This is **not** automatic. It requires the project to be careful about what it stores in routers, what is `@Serializable`, and what is held in transient memory.

## The mechanism

Decompose's `StateKeeper` serializes the entire router stack to a `Bundle` (Android) / iOS state archive on process death. When the process is restarted, the same root component is reconstructed and the stack is deserialized.

```
Process death → StateKeeper.save() → bytes in OS-managed Bundle
Process restart → StateKeeper.consume() → bytes decoded → router stack rebuilt
                                       ↓
              childStack(serializer = RootRouter.serializer(), ...) restores all configs
                                       ↓
              childFactory(config, ctx) recreates each Component
                                       ↓
              Each Component re-creates its ViewModel via retainedInstance { ... }
                                       ↓
              ViewModel.init { } runs again — re-subscribes to Flows, re-fetches if needed
```

## Hard rules

### 1. All `*Router` classes MUST be `@Serializable`

```kotlin
@Serializable
public sealed class RootRouter : BaseRouter {
    @Serializable public data object Home : RootRouter()
    @Serializable public data class Profile(val value: ProfileRouter) : RootRouter()
    // ...
}

@Serializable
public sealed class ProfileRouter : BaseRouter {
    @Serializable public data object Body : ProfileRouter()
    @Serializable public data class WorkoutHistory(val initialRange: DateRange) : ProfileRouter()
}
```

Forgetting `@Serializable` on a subtype crashes at runtime when `kotlinx-serialization` tries to write the stack.

### 2. All payloads in routes MUST be `@Serializable`

```kotlin
@Serializable
public data class WorkoutHistory(
    val initialRange: DateRange,         // DateRange is @Serializable in :toolkit:date-utils
) : ProfileRouter()
```

`DateRange`, `StageState`, all `*FormatState`, `DateFormat`, `DateRangeKind` — all **are** `@Serializable` in this project. `UiText` is **not** (`@Stable` only — it wraps a non-serializable `StringResource` plus `ImmutableList<Any>` args), so don't put a `UiText` in a route payload. Resolve it to a `String`/`Res` key before navigating, or keep it inside `State` (which is rebuilt in `init { }` after process death, not restored from the StateKeeper).

### 3. No lambdas in route configs

```kotlin
// FORBIDDEN
@Serializable
data class BadRoute(val onResult: (Foo) -> Unit) : SomeRouter()
```

Lambdas don't serialize. If a route needs a result-back path, use **`ResultManager`** (see `03-architecture-patterns/04-cross-component-results.md`) — sender is the producing screen; consumer subscribes in `init { }` of the receiving component.

### 4. All `DialogConfig` subtypes MUST be `@Serializable`; callbacks MUST be `@Transient`

```kotlin
@Serializable
public data class WeightPicker(
    val initial: Float?,
    @Transient public val onResult: (Float) -> Unit = { },
) : DialogConfig() {
    override val key: String get() = buildKey("WeightPicker", initial)
}
```

After process death, the dialog is restored with the saved `initial` value but the `onResult` lambda is the default no-op (because `@Transient`). The picker still functions; selecting a value just doesn't propagate back. This is intentional — see `03-architecture-patterns/03-dialog-navigation.md`.

### 5. ViewModel state is **not** restored automatically

`BaseViewModel` does not use `SavedStateHandle`. State is rebuilt in `init { }` via re-subscription to `Flow`s from the data layer:

```kotlin
internal class TrainingsListViewModel(...) : BaseViewModel<...>(...) {

    init {
        trainingsFeature.observeTrainings(state.value.range.from, state.value.range.to)
            .map { it.toState() }
            .onEach { items -> update { it.copy(items = items) } }
            .safeLaunch()

        safeLaunch(loader = TrainingsListLoader.LoadingTrainings) {
            trainingsFeature.getTrainings(state.value.range.from, state.value.range.to).getOrThrow()
        }
    }
}
```

The first `onEach` populates the list immediately from Room cache (no network). The `safeLaunch` block refreshes the cache. On a healthy connection, the user perceives the screen as "instantly populated".

### 6. Range/filter state lives in the route, not the ViewModel

If a screen has a date range filter and the user backgrounds with "Last 30 days" selected, the screen should restore to "Last 30 days" — not to a default.

**Solution:** put the range in the `<Feature>Router.<Screen>` config:

```kotlin
@Serializable
public data class WorkoutHistory(
    val initialRange: DateRange,
) : ProfileRouter()
```

The component pulls it from the config and threads it into the ViewModel:

```kotlin
internal class WorkoutHistoryComponent(
    componentContext: ComponentContext,
    initialRange: DateRange,
    private val back: () -> Unit,
) : BaseComponent<WorkoutHistoryDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        WorkoutHistoryViewModel(initialRange, getKoin().get())
    }
    // ...
}
```

`WorkoutHistoryViewModel(initialRange, ...)` stores `initialRange` in its initial state.

**When the user changes the range**, the ViewModel updates `state` — but does **not** push a new route. The change is invisible to the stack. On process death + restart, the screen restores to `initialRange`, not the last-selected range.

If the last-selected range must be restored, the screen calls `navigation.replaceCurrent(ProfileRouter.WorkoutHistory(initialRange = newRange))` on every change. This makes the stack the source of truth. **Use sparingly** — frequent `replaceCurrent` calls flood the StateKeeper.

### 7. Ephemeral selection state can be lost

A multi-select picker that allows the user to tap items doesn't usually persist intermediate selection state through process death. The user re-opens the picker, makes the selection again. This is acceptable.

If preservation is needed, store the selection in the router (as above) — accept the StateKeeper cost.

## Decompose `retainedInstance` and Android config changes

`retainedInstance` is a Decompose helper that retains a ViewModel across **configuration changes** (rotation, theme change, dark/light) **without** process death. The ViewModel is the same instance; the `init { }` block does not re-run.

On **process death**, the retained instance is discarded. The Component is reconstructed via the router stack, and `retainedInstance { ... }` creates a fresh ViewModel — re-running `init { }`.

## iOS specifics

iOS doesn't have Android's "killable Activity" model, but apps can be terminated by the system after long backgrounding. The same StateKeeper mechanism applies: Decompose serializes via `StateKeeper` to a Kotlin/Native-managed buffer, which is persisted to disk (managed by the Decompose iOS lifecycle integration).

`iosApp/iOSApp.swift` should wire the lifecycle so that:
- `applicationDidEnterBackground` → save state.
- `applicationWillEnterForeground` → restore.

The exact mechanism depends on the `iosMain/RootViewController.kt` glue.

## What "Don't Keep Activities" tests

Enable "Don't Keep Activities" in Android Developer Options. Then:

1. Navigate to a deep screen (e.g. Profile → WorkoutHistory).
2. Press Home.
3. Wait a few seconds.
4. Re-open the app.

The user **must** return to the same screen with the same `initialRange` selected, the same data visible (loaded from Room cache), and the same dialog open (if applicable).

If this breaks: check the router serialization (a route or payload is missing `@Serializable`).

## Anti-patterns

- **Storing user input in a transient ViewModel `var` outside `state`.** It will be lost. Use `update { it.copy(...) }` only.
- **Caching network responses in ViewModel memory.** Cache in Room (DAO), observe from there. ViewModel state derives from observation.
- **Putting non-`@Serializable` types in routes.** Crash on backgrounding.
- **Relying on dialog callbacks to persist after process death.** They're `@Transient`. Either persist the value in the screen's route or use `ResultManager`.
- **Implementing custom save/restore in `BaseViewModel`.** It's not the pattern; restore via `Flow` subscription in `init { }`.
- **Forgetting `key = "..."`** in `childStack(...)` — multiple children with the same default key collide in the StateKeeper.
