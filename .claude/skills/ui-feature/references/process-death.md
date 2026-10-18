# Process-Death Restoration

> Note/Tag/AmountPicker/NoteDetail examples are educational, nonexistent APIs. They are not the current route, dependency or source catalog; inspect real callers and signatures before adapting them.

Apply the mandatory [state-preservation gate](../../state-preservation/SKILL.md)
for every new feature or affected existing state owner. This reference defines
the implementation; the linked skill defines the field inventory and completion checks.

Serializable screen routes support restoration when the platform shell supplies
saved navigation state. Android's `MainActivity` creates the root through
`retainedComponent { ... }`; iOS supplies a validated checkpoint after completed
background saving. Do not promise the same stack on every app restart or claim
that loaders or all ViewModel state are automatically serialized. Fresh ViewModels
rebuild state from route inputs, explicitly saved business drafts and data sources.
Dialog sessions and their callbacks are in memory and are not restored after process death.

## The mechanism

Screen `childStack` instances use their Router serializers with Decompose's
`StateKeeper`. When the shell recreates the root with saved navigation state, the
stack is deserialized and Components rebuild their ViewModels:

```
Shell supplies saved archive → StateKeeper.consume() → router stack rebuilt
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

`DateRange`, `StageState` (in `:ui-core:state`, package `com.grippo.core.state.stage`: `Add`, `Edit(id)`, `Draft`), all `*FormatState`, `DateFormat`, `DateRangeKind` — all **are** `@Serializable` in this project. A `ResultKey` field serializes as its address only (`ResultKeySerializer`), so the `Action` protocol it names (`TrainingRouter.Exercise.Action`) is deliberately not `@Serializable`; see [results.md](results.md). `UiText` is **not** (`@Stable` only — it wraps a non-serializable `StringResource` plus `ImmutableList<Any>` args), so don't put a `UiText` in a route payload. Carry a serializable identifier and rebuild localized UiText after restoration; a plain String is only for verbatim, nonlocalized text. Keep rebuilt UiText fields transient with defaults in a serialized draft.

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

`BaseViewModel` does **not** use `SavedStateHandle`. State is rebuilt in `init { }` via re-subscription to data-layer `Flow`s. The excerpt omits the `analytics.trackOperation` wrapper a real startup load carries:

```kotlin
init {
    notesFeature.observeNotes(start = state.value.range.from, end = state.value.range.to)
        .onEach(::provideNotes)
        .safeLaunch()

    safeLaunch(loader = NotesListLoader.LoadingNotes) {
        notesFeature.getNotes(start = state.value.range.from, end = state.value.range.to)
    }
}

private fun provideNotes(value: List<Note>) {
    val items = value.toState()

    update { it.copy(items = items) }
}
```

The observer rebuilds list presentation from the existing data source; the request refreshes that source. Inspect actual cache and Flow behavior before claiming initial timing or availability.

### Editable form drafts

Nonsecret unfinished forms and independent-screen selections must preserve their
latest immutable business draft through the owning Component's existing StateKeeper.
Define `val DRAFT = PersistenceKey("stable address", State.serializer())` in a
private companion object at the bottom of the owning Component.
Restore through protected BaseComponent.restoreState
when constructing a fresh retained ViewModel, pass it as `initialState`, and
register the current VM state through saveState. Restore the parent flow's accepted
data and register its lazy supplier before constructing children that use it. Child ComponentContexts
have separate StateKeepers, so each screen owns its draft locally.

Orientation (inspect the Components): 17 Components define
`val DRAFT = PersistenceKey("<Feature>Draft", <State>.serializer())` in a private
companion and call `restoreState(DRAFT)` plus `saveState(DRAFT) { viewModel.state.value }`.
Seven ViewModels take a nullable `initialState` and receive `restoreState(DRAFT)`
directly; the other ten take a non-null `initialState`, so their Components pass
`restoreState(DRAFT) ?: <State>(...)`. `PersistenceKey<T : Any>(key, serializer)` is
a final class with `internal` properties: a feature constructs it and passes it only
to `restoreState`/`saveState`. Six draft States and `AuthRouter.Onboarding`/`AuthProcess`
carry `@JsonIgnoreUnknownKeys` because older archives may contain removed fields.

A popped child can be destroyed, discarding its local draft. On re-entry, pass the
parent flow's already accepted values as ordinary initial business inputs and
use them only when `restoreState(DRAFT)` returns null. A retained VM wins over
both the local archive and those inputs. The parent holds accepted values,
not unfinished drafts copied from destroyed children; add no global draft store.

```kotlin
// UserComponent.kt — excerpts; the private companion is last in the class.
import com.grippo.core.foundation.models.PersistenceKey

override val viewModel = componentContext.retainedInstance {
    UserViewModel(
        analytics = getKoin().get(),
        dialogController = getKoin().get(),
        logoutUseCase = getKoin().get(),
        initialState = restoreState(DRAFT)
            ?: UserState(),
    )
}

init {
    saveState(DRAFT) { viewModel.state.value }
    backHandler.register(backCallback)
}

// Other Component methods are above this final member.
private companion object {
    val DRAFT = PersistenceKey("UserDraft", UserState.serializer())
}
```

Keep passwords, tokens, provider credentials and other secrets out of the draft:
mark secret inputs transient with safe empty defaults. A serializable formatter
does not authorize storing its secret content. Saved invalid/empty nonsecret input
must take precedence over route prefill; recompute current provider availability.

Keep the typed descriptor private to its owning Component. Do not add separate
metadata files, feature-wide matrices, aliases, extensions, DI or raw draft
StateKeeper.consume/register bindings. PersistenceKey couples the type and
serializer; no reflection or live identity determines an archive address. Preserve
existing addresses when refactoring, without alias/fallback reads. Navigation
childStack keeps its separate navigation key and Router serializer.
Enable the owning module's serialization
plugin; use `@Serializable` draft state and the existing ImmutableListSerializer
for immutable lists. Preserve FormatState values, including invalid display text.
Exclude catalogs, localized presentation data and other rebuilt suggestions with
`@Transient` and a default; re-subscribe to their existing data Flows.
For data-backed forms, follow the
[draft initialization policy](../../state-preservation/SKILL.md#data-backed-form-synchronization):
persist actual values, populate a fresh form once and preserve restored snapshots.
Keep no private runtime flags, previous-source caches or shadow snapshots; later
source updates rebuild presentation without replacing form inputs. An absent
archive and a saved all-default snapshot are distinct; the VM can receive nullable
initialState at this boundary while BaseViewModel always receives a complete state.
Catalog selection must preserve which IDs were known and selected (or excluded),
so the first restored emission cannot treat all known items as newly selected.
An initial empty cache emission must not erase the draft before data arrives.

This is business state ownership, separate from analytics. Add no generic
BaseViewModel save/restore framework, provider, synchronization job or per-input
route replacement. The supplier runs when StateKeeper saves, not on every edit.
Dialog sessions and callbacks remain unsaved. Review save → serialization roundtrip
→ destroy → fresh ComponentContext/InstanceKeeper → restore, including unfinished
edits, Back and final operation inputs; retention alone is insufficient proof.
This preserves drafts when the shell supplies saved state. Android recreation and
iOS background checkpoints have different delivery policies; neither guarantees
a draft on every arbitrary cold start.

### 6. Route inputs seed filters; StateKeeper preserves the latest selection

For supported screen-route restoration, keep a date-range filter in the `<Feature>Router.<Screen>` config (`NoteArchive(val initialRange: DateRange)`). The component pulls it from the config and threads it into the ViewModel via `retainedInstance { <X>ViewModel(initialRange, ...) }`, which stores it in initial state. This supplies the selected input when the shell restores that route; the owning Component still preserves later edits locally.

When the user changes the range/filter, the ViewModel updates `state`; the owning
Component saves the current selection in its local business draft. Restore that
draft before falling back to initial route or accepted parent inputs. Verify
Back/pop → destroyed child → re-entry as well as archive restoration. Do not replace the route
on every edit: a different configuration can recreate the Component/VM and lose
runtime work or duplicate presentations.

### 7. Live dialog selection differs from an accepted screen draft

An unconfirmed selection inside a live dialog is runtime session state; the dialog
closes after process death. An accepted result belongs to its owning screen draft
and must be preserved. Independent selection screens preserve their current
selection even before Next. Do not serialize DialogConfig, callbacks or sessions.

## `retainedInstance` and config changes

`retainedInstance` retains a ViewModel across **configuration changes** (rotation, theme) **without** process death — same instance, `init { }` does not re-run. This also preserves the host's live dialog session. On **process death** the retained instance is discarded; recreated Components use restored screen routes where the shell provides saved state, or initial routes otherwise. `retainedInstance { ... }` creates fresh ViewModels, re-running `init { }`; no dialog session is restored.

## iOS specifics

The current app uses one scene (`UIApplicationSupportsMultipleScenes = false`) and
one lazy root owned by `iosApp/iosApp/AppDeletage.swift` (file name carries a typo;
the class is `AppDelegate`). Keep the existing
ApplicationLifecycle owner; SwiftUI checkpoint handling does not drive another
lifecycle or recreate the root on foregrounding.

Maintained sources:

- `shared/src/commonMain/kotlin/com/grippo/shared/state/ApplicationState.kt` owns
  StateKeeperDispatcher and serializes its latest suppliers. It exposes a fresh
  constructor, a throwing saved-archive constructor and throwing save(); no local
  feature persistence, platform files or recovery lives in the codec.
- `iosApp/iosApp/ApplicationStateStore.swift` owns the native checkpoint. Its
  envelope contains format version, application version, build version and SHA-256
  of the exact UTF-8 state. Save is atomic in Application Support, excluded from
  backup, with completeFileProtectionUntilFirstUserAuthentication so background
  writes remain possible after the phone locks following its first unlock.
- `AppDeletage.swift` consumes the checkpoint once, before archive decoding and
  DefaultComponentContext/RootComponent construction. Its external error boundary
  uses a constant log and fresh ApplicationState for store/codec failures. Root
  construction remains strict. Saving an uninitialized root is a no-op: it must
  not create UI for a background launch.
- `iosApp/iosApp/iOSApp.swift` has the single scenePhase hook: background saves;
  active removes only the disk checkpoint; inactive does nothing. Both saving and
  clearing are no-ops before ApplicationState/root initialization, so active delivery
  cannot discard the checkpoint before lazy restoration. Returning to foreground
  keeps the current root, drafts and retained ViewModels in memory.
  RootView and RootViewController render the supplied root; they add no persistence.

Restoration supports a completed background checkpoint from the same application,
build and archive format. Missing, mismatched or modified checkpoints start fresh;
a consumed checkpoint cannot replay repeatedly after a failed launch. Increment the
build or archive format when serializer, key or route changes make saved data
incompatible: nested Essenty data is decoded lazily, and the envelope is not a
migration framework. Keep existing Component keys unchanged in ordinary refactors.

Abrupt active-process termination, a failed checkpoint write and termination before
background saving completes have no restoration guarantee. Session/authorization
state remains owned by its existing data layer; the archive stores no credentials
and does not authorize navigation. Dialogs, jobs and callbacks remain unsaved.
The checkpoint is a point-in-time UI snapshot. Existing business operations can
finish after it is saved; later changes to tokens, profile data or navigation are
not retroactively included. Do not claim session revalidation or transactional
consistency with the data layer from this bridge alone, and do not put credentials
or session policy in its envelope to compensate.

Verification must distinguish source/codec inspection from actual native shell
execution. Source review does not prove scene delivery, file protection, generated
Swift exports or a process restart. Essenty's JVM and iOS state formats differ;
inspect the actual platform codec rather than assuming the formats are equivalent.
Manual save/restart/restoration scenarios follow
[runtime-verification](../../runtime-verification/SKILL.md); report unrun platform
checks when builds/runtime are outside the user's scope.

## What "Don't Keep Activities" checks

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
- **Relying on dialog sessions or callbacks to persist after process death.** They are in memory; persist required business data independently in a screen route, the owning Component's business draft or the data layer.
- **Adding serialization or callback-default restoration to `DialogConfig`.** The host intentionally uses `serializer = null` for sessions and steps.
- **Implementing custom save/restore in `BaseViewModel`.** Restore via `Flow` subscription in `init { }`.
- **Forgetting `key = "..."`** in `childStack(...)` — duplicate default keys collide in the StateKeeper.
