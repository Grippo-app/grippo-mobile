# Process-Death Restoration

Serializable screen routes support restoration when the platform shell supplies
saved navigation state. Android's `MainActivity` creates the root through
`retainedComponent { ... }`; the iOS shell currently supplies no saved
StateKeeper state. Do not promise the same stack on every app restart or claim
that loaders and ViewModel state are serialized. Fresh ViewModels rebuild state
from route inputs and data sources. Dialog sessions and their callbacks are in
memory and are not restored after process death.

## The mechanism

Screen `childStack` instances use their Router serializers with Decompose's
`StateKeeper`. When Android recreates the root with saved navigation state, the
stack is deserialized and Components rebuild their ViewModels:

```
Android recreation with saved state → StateKeeper.consume() → router stack rebuilt
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

### 4. Dialog sessions are NOT serialized or restored

```kotlin
// An ordinary screen ViewModel opens a live session.
dialogController.open(
    DialogConfig.WeightPicker(
        title = UiText.Res(Res.string.weight_picker_title),
        initial = state.value.weight.value,
        onResult = { value -> update { it.copy(weight = WeightFormatState.of(value)) } },
    )
)
```

`DialogConfig` is a plain runtime sealed class with `title: UiText` and live
typed callbacks. Do not add `@Serializable`, `@Transient`, a config `key` or
`buildKey`. Its `hasSameContentAs` comparison checks typed content and titles,
excluding callbacks.

The host uses `SlotNavigation<DialogSession>` and an internal
`StackNavigation<DialogStep>`, both with `serializer = null`. The retained host
ViewModel preserves the live session during ordinary component recreation;
process death discards the session, steps and callbacks. There is no restored
picker with a default no-op result callback. See [dialogs.md](dialogs.md) for
session navigation and result delivery.

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

For supported screen-route restoration, keep a date-range filter in the `<Feature>Router.<Screen>` config (`NoteArchive(val initialRange: DateRange)`). The component pulls it from the config and threads it into the ViewModel via `retainedInstance { <X>ViewModel(initialRange, ...) }`, which stores it in initial state. This supplies the selected input when the shell restores that route; it does not add iOS persistence.

When the user changes the range, the ViewModel updates `state` but does **not** push a new route — so on restart the screen restores to `initialRange`, not the last-selected range. If the last-selected range must be restored, call `navigation.replaceCurrent(ProfileRouter.NoteArchive(initialRange = newRange))` on every change (makes the stack the source of truth). **Use sparingly** — frequent `replaceCurrent` floods the StateKeeper.

### 7. Ephemeral selection state can be lost (SHOULD-accept)

A multi-select picker doesn't usually persist intermediate selection through process death — the user re-opens and re-selects. Acceptable. If preservation is needed, store the selection in a serialized screen route (accept the StateKeeper cost), not in a dialog config.

## `retainedInstance` and config changes

`retainedInstance` retains a ViewModel across **configuration changes** (rotation, theme) **without** process death — same instance, `init { }` does not re-run. This also preserves the host's live dialog session. On **process death** the retained instance is discarded; recreated Components use restored screen routes where the shell provides saved state, or initial routes otherwise. `retainedInstance { ... }` creates fresh ViewModels, re-running `init { }`; no dialog session is restored.

## iOS specifics

iOS apps can be terminated after long backgrounding. The current shell in
`iosApp/iosApp/AppDeletage.swift` constructs `DefaultComponentContext` with
`stateKeeper: nil` and `instanceKeeper: nil`. It does not supply a persisted
Decompose archive or background/foreground save/restore bridge.
`shouldSaveSecureApplicationState` / `shouldRestoreSecureApplicationState`
returning `true` does not provide that bridge. `RootViewController.kt` renders
the supplied root; it does not persist navigation state. A new iOS process builds
a fresh navigation tree, with ViewModels rebuilding domain-backed data. Keep
screen routes serializable, but do not claim iOS process-death stack restoration
is implemented.

## What "Don't Keep Activities" tests

This Android option destroys Activities when they leave the foreground; it does
not necessarily kill the process. Navigate deep → press Home → re-open to inspect
Activity recreation and saved screen routes. Check route inputs such as
`initialRange` and data rebuilt from Room independently of live dialog retention.
A dialog survives ordinary recreation only while its retained host ViewModel
remains alive. This check alone does not prove process-death restoration or
callback persistence. After an actual process restart, dialog sessions are
closed; inspect saved screen stack restoration separately. For screen-route
failures, check the Router and payload serializers.

## Anti-patterns (MUST avoid)

- **Storing user input in a transient ViewModel `var` outside `state`.** It will be lost. Use `update { it.copy(...) }` only.
- **Caching network responses in ViewModel memory.** Cache in Room (DAO), observe from there.
- **Putting non-`@Serializable` types in routes.** Crash on backgrounding.
- **Relying on dialog sessions or callbacks to persist after process death.** They are in memory; persist required business data independently in the screen route or data layer.
- **Adding serialization or callback-default restoration to `DialogConfig`.** The host intentionally uses `serializer = null` for sessions and steps.
- **Implementing custom save/restore in `BaseViewModel`.** Restore via `Flow` subscription in `init { }`.
- **Forgetting `key = "..."`** in `childStack(...)` — duplicate default keys collide in the StateKeeper.
