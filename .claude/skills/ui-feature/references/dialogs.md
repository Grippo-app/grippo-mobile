# Dialogs (Bottom Sheets) — Navigation & Recipe

> Note/Tag/AmountPicker/NoteDetail examples are educational, nonexistent APIs. They are not the current route, dependency or source catalog; inspect real callers and signatures before adapting them.

Dialogs (bottom sheets) live in a **parallel** navigation graph, not in the main
screen stack: they can be shown over any screen and dismissed independently of
stack changes.

Current contracts: [DialogController / DialogProvider](../../../../ui-dialog-features/dialog-api/src/commonMain/kotlin/com/grippo/dialog.api/DialogController.kt)
and [DialogConfig](../../../../ui-dialog-features/dialog-api/src/commonMain/kotlin/com/grippo/dialog.api/DialogConfig.kt).
The host [DialogViewModel](../../../../shared/src/commonMain/kotlin/com/grippo/shared/dialog/DialogViewModel.kt)
and [session bridge](../../../../shared/src/commonMain/kotlin/com/grippo/shared/dialog/internal/SessionDialogNavigation.kt)
own session changes and callback ordering.

## Two parallel navigators

```
RootComponent
  ├── stack: StackNavigation<RootRouter>         // screens
  └── dialog: SlotNavigation<DialogSession>      // bottom sheets (one at a time)
```

Both render as siblings inside `RootComponent.Render()` (not nested inside `RootScreen`):

```kotlin
@Composable
override fun Render() {
    // state/loaders collected above; systemIsDark = AppTheme.current, localeTag = AppLocale.current
    AppTheme(darkTheme = systemIsDark, localeTag = localeTag) {
        RootScreen(this, state.value, loaders.value, viewModel)
        dialogComponent.Render()       // overlays on top of the screen stack
    }
}
```

A **slot** navigator shows **at most one** child at a time. The shared
`DialogContentComponent` renders its `StackNavigation<DialogStep>` for in-sheet
push/pop. Both navigators use `serializer = null`: live sessions and callbacks
are retained during ordinary recreation, but are not restored after process death.

## `DialogConfig` — in-memory content and callbacks

Plain sealed class in `:ui-dialog-features:dialog-api` (abridged shape):

```kotlin
public sealed class DialogConfig(
    public open val onDismiss: (() -> Unit)? = null,
    public open val dismissBySwipe: Boolean = true,
) {
    public abstract val title: UiText

    public data class WeightPicker(
        public override val title: UiText,
        val initial: Float?,
        val onResult: (value: Float) -> Unit = {},
    ) : DialogConfig()

    public data class Confirmation(
        override val title: UiText,
        val description: UiText?,
        val onResult: () -> Unit = {},
    ) : DialogConfig()

    public data class ErrorDisplay(
        public override val title: UiText,
        val error: AppErrorState,
        val mode: Mode,
        val onClose: () -> Unit,
    ) : DialogConfig(onDismiss = onClose) {
        public sealed interface Mode {
            public data class Retry(val onRetry: () -> Unit) : Mode
            public data object Inform : Mode
        }
    }
}
```

Rules (MUST):
- **Keep configs and callbacks in memory.** No `@Serializable` / `@Transient` or process-death callback restoration is required; sessions use `serializer = null`.
- **Caller-owned `title: UiText`** supplies the shared host toolbar. Keep input data as ordinary typed fields and callbacks as live closures.
- **Update `hasSameContentAs` for every new subtype/content field.** The existing exhaustive comparison checks title first, compares typed content and excludes callbacks. `Session` navigation and the host's global openings use it to skip duplicate content. `ErrorDisplay` never matches another config, because each error awaits its own decision.
- **Runtime identity belongs to the host.** `DialogSession` / `DialogStep` IDs preserve live child instances; configs have no `key` / `buildKey` contract.
- **`onDismiss` and `dismissBySwipe` are `open val`** in the base class; subtypes supply them through super-class arguments. `ErrorDisplay` wires `onDismiss = onClose` and is currently the only subtype that sets it; its `mode` and `onClose` come from the [error pipeline](error-pipeline.md#retry-and-close), and its `Mode` follows the existing `ExerciseExampleDetails.Mode` pattern. Set swipe policy only when the requested behavior needs it.

## `DialogController` + `DialogProvider`

```kotlin
public interface DialogController {
    public fun open(config: DialogConfig)

    public interface Session {
        public fun push(config: DialogConfig)
        public fun replaceCurrent(config: DialogConfig)
        public fun back()
        public fun close()
    }
}
public interface DialogProvider { public val openings: Flow<DialogConfig> }
```

The internal implementation in `:ui-dialog-features:dialog-api` is
`@Single(binds = [DialogController::class, DialogProvider::class])`, with a lossless
`Channel<DialogConfig>` exposed as `openings`. `DialogModule` is the existing public
`@Module @ComponentScan`, registered through its generated module in shared.

Inject the controller into ordinary caller ViewModels and call
`dialogController.open(DialogConfig.X(...))`. There is **no `dismiss()`** on this
controller. The host handles every opening the same way, by sheet phase, and never
checks a subtype (inspect `DialogViewModel.open`, `onDismiss` and `onRelease`):

- **No sheet (Released):** the opening starts a new session.
- **Sheet presented:** an opening whose content matches a step already in the stack
  is skipped, as in `Session.push`. Otherwise it is pushed onto the session's stack
  above the active step, like an in-sheet step. The step underneath, its ViewModel
  and, for an error, the suspended operation stay alive, so Retry resumes that
  operation in its ViewModel. Back or Retry pop the pushed step and return to the one underneath;
  toolbar Close, a swipe or an outside tap closes the whole session and runs every
  step's `onDismiss`, so a covered error completes as Close. While a step is covered,
  its own `finish`/`back`/`push` calls and an error step's Retry are ignored.
- **Sheet dismissing:** the opening waits in `pending`, first in, first out, unless
  its content matches one already pending. At release, all pending openings start
  the next session as one stack in arrival order (the latest on top). Errors never
  match, so no error is dropped.

The pending queue is live runtime State, discarded on process death like the
sessions it feeds.

The host supplies `Session` bound to the originating session and active step.
Use its methods on the UI thread; calls from inactive/removed steps are ignored.
`push` retains the current step, `replaceCurrent` replaces it, `back` returns to
the previous step or closes at the root, and `close` closes the entire session.

## `DialogComponent` / `DialogContentComponent` shape

Two components, both in `:shared/dialog/...` (inspect `DialogComponent.kt`, `content/DialogContentComponent.kt` and `DialogViewModel.kt`):
1. **`DialogComponent`** owns the **outer** slot: `childSlot(serializer = null, key = "DialogComponent.v3", handleBackButton = false)` over `SlotNavigation<DialogSession>`, holding at most one child (a `DialogContentComponent`). Its retained `DialogViewModel` consumes `DialogProvider.openings` and owns session/stack/phase and pending openings under the retention policy above. Reconciliation is state-driven: a collector of `viewModel.state` activates/dismisses the slot and replaces the inner stack; `DialogComponent.eventListener` is a no-op because the host emits no Directions.
2. **`DialogContentComponent`** renders the **inner** stack: `childStack(serializer = null, key = "DialogContentComponent.v3", handleBackButton = false)` over `StackNavigation<DialogStep>`. Each step has a host-owned runtime ID and config; `navigationFor(step)` supplies the session-bound bridge, and the component's own `BackCallback` calls that bridge's `back()`.

The bridge's internal `finish(pendingResult)` passes a result closure to the owning
`DialogViewModel`. At the root it hides/releases the sheet before running callbacks;
for a nested step it pops that step while the sheet remains visible. In both cases
the removed config's `onDismiss` runs before the pending result. The dialog feature
does not resolve or construct this bridge.

Inspect `shared/.../dialog/content/DialogContentScreen.kt`: its `AnimatedContent`
targets the active stack child, so local expansion does not trigger that step
transition. Local content follows
[disclosure transitions](../../compose-components/references/structure-and-runtime.md#disclosure-transitions).

## In-sheet navigation

Nested opening uses the originating step's `Session.push(config)` or
`replaceCurrent(config)`. For a feature that emits semantic directions, its Component
forwards typed callbacks and the shared host selects the next config/title using that
step's session handle. Do not inject the global controller merely to push a nested step.
Each step remains an independent dialog feature (own Component/ViewModel/Screen).
Single-step dialogs need no step enum; a true multi-mode dialog models its mode in
its own State, separately from navigation.

## Returning a result

Default — **callback in `DialogConfig`** (local, explicit, scoped to one call site):

```kotlin
dialogController.open(
    DialogConfig.WeightPicker(
        title = existingTitle,
        initial = state.value.weight.value,
        onResult = { value ->
            val weight = WeightFormatState.of(value)
            update { it.copy(weight = weight) }
        },
    )
)
```

The picker VM emits a result Direction; its Component forwards the value through
the existing `onResult` callback. The shared child factory translates that callback
into `dialogNavigation.finish { router.onResult(value) }` and plain Back into
`dialogNavigation.finish(null)`. The child factory is the only host code that knows
`ErrorDisplay`. Like `ExerciseExampleDetails`' `onAction`, it passes `mode = router.mode`
and an `onRetry` that, only while its step is active (`stepIsActive()`, the check
`finish` uses), invokes `(router.mode as? Mode.Retry)?.onRetry` and then
`dialogNavigation.finish(null)`. The error decision is completed with Retry first, so
the `onClose` that the removal then runs is ignored (the first completion wins). A
Retry from a step that is no longer active (the sheet is hiding after Back or toolbar
Close, or a newer step covers it) is ignored, so that popup completes as Close. A swipe
or outside tap keeps the sheet `Present` until Material reports it hidden, so a Retry
reaching the button during that hide can still win; this rare case is accepted.
`Mode.Inform` offers only Got it, which closes the popup.

Dialogs keep these typed config callbacks. Separate screen results use
`ResultManager` (`references/results.md`); a dialog result does not require a relay.

## Dismissal behavior

- **Whole-session release:** after hiding, `DialogViewModel.onRelease(session)` invalidates the old session by publishing the next session from the pending openings, or empty State when nothing is pending. It then runs the old stack's `onDismiss` callbacks top-to-bottom and then the top step's pending result. Reentrant openings see the new state and join it. Swipe/scrim or Close supplies no result; `onDismiss` alone does not establish confirmation/cancellation/user exit.
- **Nested Back/result:** the top step is removed, then its `onDismiss` and pending result run in that order. The sheet stays open. `replaceCurrent` runs the replaced config's `onDismiss` after updating the stack and does not create a result.
- **Caller cleanup:** use `onDismiss` for cleanup or returning an observed dismissal fact to its owning VM; it runs whenever its step is removed, before any pending result. Shared host/config plumbing does not construct telemetry; caller-VM-owned result callbacks may observe accepted results under the analytics skill.
- **`dismissBySwipe`:** governs swipe/scrim hiding for the active config. Hardware/toolbar Back still follows the stack; toolbar Close closes the session. It does not turn a generic dismissal into a typed outcome.

## Rules summary (MUST)

- **One `DialogConfig` per dialog flow.** Don't multiplex configs for similar flows.
- **Configs and callbacks stay live in memory; sessions are not restored after process death.**
- **`onResult` callbacks fire only on explicit success.** Dismiss vs result are separate.
- **Cross-dialog flows use the originating step's `Session` and the shared `StackNavigation<DialogStep>`.** Global `open` is handled by sheet phase as described in [DialogController + DialogProvider](#dialogcontroller--dialogprovider).
- **Single-dialog multi-mode** lives in the dialog's own `State` as a `sealed interface`.
- **Scrolling dialog bodies go through `OverlayContainer`** with the navigation inset in its `bottom` slot (see step 3 of the recipe); non-scrolling bodies end with the two spacers directly.
- Dialogs MAY read/write domain data through Feature/UseCase contracts in `:data-features:feature-api`, when their behavior requires it; never access services/DAOs directly. Value pickers return their accepted value through `onResult`; the caller owns its business persistence. A dialog-owned side effect MUST complete through safeLaunch and the shared error pipeline before emitting success.

---

# Recipe — Add a Dialog (e.g. `:ui-dialog-features:tag-picker`)

> Apply [feature-analytics](../../feature-analytics/SKILL.md) for behavior changes.
> Examples below show dialog/MVI mechanics. Open/result facts belong to existing
> owning-VM callbacks; dedicated dialog presentation/close tracking is a separate
> declared scope. Schema availability does not mean automatic instrumentation.


### 1. Add the module to `settings.gradle.kts`
```kotlin
include(":ui-dialog-features:tag-picker")
```

### 2. Create `build.gradle.kts`
```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}
kotlin {
    android {
        namespace = "com.grippo.ui.dialog.features.tag.picker"
    }

    sourceSets.commonMain.dependencies {
        implementation(projects.uiCore.foundation)
        implementation(projects.analytics.analyticsApi)
        implementation(projects.uiCore.state)
        implementation(projects.designSystem.core)
        implementation(projects.designSystem.resources.provider)
        implementation(projects.designSystem.components)
        implementation(projects.designSystem.preview)
        implementation(compose.foundation)
        implementation(compose.material3)
        implementation(libs.immutable.collections)
    }
}
```
Mirror a neighbor such as `ui-dialog-features/weight-picker/build.gradle.kts`; every
dialog module depends on `:analytics:analytics-api`.

### 3. Create the seven MVI files (package `com.grippo.tag.picker`)

Visibility: the dialog Component is `public` and exposes `viewModel`, whose
`state`/`loaders`/`navigator` are public properties, so `State`, `Direction`,
`Loader` and `ViewModel` are `public`; `Contract` stays `internal` (inspect
`ui-dialog-features/weight-picker/`).

`TagPickerState.kt` — `@Immutable public data class TagPickerState(val current: String = "")`. Defaults in ctor; **no `companion object Empty` on State** (Empty belongs on Contract).

`TagPickerDirection.kt`:
```kotlin
public sealed interface TagPickerDirection : BaseDirection {
    public data class BackWithResult(val value: String) : TagPickerDirection
    public data object Back : TagPickerDirection
}
```

`TagPickerLoader.kt` — `@Immutable public sealed interface TagPickerLoader : BaseLoader` (usually empty).

`TagPickerContract.kt` — `@Immutable internal interface` with `onValueChange`/`onApplyClick`/`onCloseClick` + `companion object Empty` of no-ops.

`TagPickerViewModel.kt` — observes its own callbacks through the dialog's
`TagPickerAnalytics` from `:analytics:analytics-api`, the way `WeightPickerViewModel`
emits `SelectionChanged` / `SubmitClicked` / `DismissRequested`; dialogs declare no
`screenPresentation`:
```kotlin
public class TagPickerViewModel(
    initial: String,
    private val analytics: TagPickerAnalytics,
) : BaseViewModel<TagPickerState, TagPickerDirection, TagPickerLoader>(
    TagPickerState(current = initial),
), TagPickerContract {
    override fun onValueChange(value: String) {
        if (value != state.value.current) {
            analytics.trackEvent(TagPickerEvent.SelectionChanged)
        }
        update { it.copy(current = value) }
    }

    override fun onApplyClick() {
        analytics.trackEvent(TagPickerEvent.SubmitClicked)
        navigateTo(TagPickerDirection.BackWithResult(state.value.current))
    }

    override fun onCloseClick() {
        analytics.trackEvent(TagPickerEvent.DismissRequested)
        navigateTo(TagPickerDirection.Back)
    }
}
```

`TagPickerComponent.kt` is a **`public class`**: dialog Components cross the module boundary because the shared `DialogContentComponent` builds them. Its constructor takes `back: () -> Unit` and `onResult: (T) -> Unit`. The `Direction` carries the result as `BackWithResult(value)`; plain `Back` covers dismissals:
```kotlin
public class TagPickerComponent(
    componentContext: ComponentContext,
    private val initial: String,
    private val onResult: (String) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<TagPickerDirection>(componentContext) {
    override val viewModel: TagPickerViewModel = componentContext.retainedInstance {
        TagPickerViewModel(initial = initial, analytics = getKoin().get())
    }
    private val backCallback = BackCallback(onBack = viewModel::onCloseClick)
    init {
        backHandler.register(backCallback)
    }
    override suspend fun eventListener(direction: TagPickerDirection) {
        when (direction) {
            is TagPickerDirection.BackWithResult -> onResult.invoke(direction.value)
            TagPickerDirection.Back -> back.invoke()
        }
    }
    @Composable override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        TagPickerScreen(state.value, loaders.value, viewModel)
    }
}
```

`TagPickerScreen.kt` — **no toolbar or duplicate title**: the shared host renders
`BottomSheetToolbar` using the active config's `title: UiText`. Use
`BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog))`
with `Spacer(AppTokens.dp.dialog.top)` → body → `Spacer(AppTokens.dp.dialog.bottom)`
→ `Spacer(Modifier.navigationBarsPadding())`. Horizontal padding is
`AppTokens.dp.dialog.horizontalPadding`. A dialog whose body scrolls
(`LazyColumn` or `verticalScroll`) MUST wrap the scroll container in
`OverlayContainer` (`overlay = AppTokens.colors.background.dialog`, the
resolved `PaddingValues` applied to the scroll container) and end its `bottom`
slot with the navigation inset (`Spacer(Modifier.navigationBarsPadding())` or
the slot column's `navigationBarsPadding()`); the container's fade gap takes
the place of `dialog.bottom` below the content. A pinned action comes first in
the slot with `dialog.bottom` below it (a trailing
`Spacer(AppTokens.dp.dialog.bottom)` or the action column's bottom padding).
Content then scrolls under the container's bottom overlay edge (see
[OverlayContainer](../../design-system/references/components.md)) instead of
stopping above it; never
`navigationBarsPadding()` on the scroll container itself. Reference:
`ExerciseExampleDetailsScreen.kt`. Include an `@AppPreview private fun
...Preview()` wrapped in `PreviewContainer { ... }` with `Contract.Empty`. Pick a
real input control from `:design-system:components`/`:compose-libs` (the project
uses custom `*WheelPicker`, not Material3 `Slider`). Extracted dialog UI lives in
`components/`, one cohesive composable per file.

### 4. Add the `DialogConfig` subtype in `:ui-dialog-features:dialog-api`
```kotlin
public data class TagPicker(
    public override val title: UiText,
    val initial: String,
    val onResult: (String) -> Unit = { },
) : DialogConfig()
```

Add `is TagPicker -> other is TagPicker && initial == other.initial` to the
existing exhaustive `hasSameContentAs` comparison. Its common title comparison
already runs first; exclude `onResult` and other callbacks from content equality.

### 5. Wire in `DialogContentComponent.createChild` (in `:shared`)
The existing `createChild(step: DialogStep, context)` obtains `router = step.config`
and `dialogNavigation = navigationFor(step)`. Add the branch using that bound
bridge; `finish` removes the active step or closes at the root, then runs the
caller callback after `onDismiss`:
```kotlin
is DialogConfig.TagPicker -> Child.TagPicker(
    TagPickerComponent(
        componentContext = context,
        initial = router.initial,
        onResult = { value -> dialogNavigation.finish { router.onResult.invoke(value) } },
        back = { dialogNavigation.finish(null) },
    )
)
// + a matching `data class TagPicker(override val component: TagPickerComponent) : Child(component)`
```

### 6. Create `:analytics:tag-picker` and register it in `Koin.kt`

Every dialog module has its own `:analytics:<dialog>` implementation module,
registered in `shared/src/commonMain/kotlin/com/grippo/shared/Koin.kt`;
the `TagPickerAnalytics` contract and its `TagPickerEvent` family live in
`:analytics:analytics-api`. Follow the
[new module checklist](../../platform-build-toolkit/references/new-module.md#dialog-feature-ui-dialog-featuresdialog)
for the wiring and [feature-analytics](../../feature-analytics/SKILL.md) for the
events and the coverage matrix; `analytics/weight-picker/` is the neighbor to mirror.

### 7. Add `:ui-dialog-features:tag-picker` to `:shared/build.gradle.kts`
```kotlin
implementation(projects.uiDialogFeatures.tagPicker)
```

### 8. Open a new session from a caller ViewModel
```kotlin
override fun onPickTagClick() {
    dialogController.open(
        DialogConfig.TagPicker(
            title = existingTitle,
            initial = state.value.selectedTagId,
            onResult = { newTagId -> update { it.copy(selectedTagId = newTagId) } }
        )
    )
}
```

### Verify
Update the dialog's coverage matrix (`analytics/<dialog>/COVERAGE.md`) and run the
analytics skill's checks. Run the build checks the
[verification policy](../../../../CLAUDE.md#verification-policy) requires for
the changed files. Manual checks — open the dialog, confirm the result callback fires,
check Back and swipe dismissal — follow
[runtime-verification](../../runtime-verification/SKILL.md).

### What you did NOT have to do
- No Koin module inside the dialog UI module itself: only `:ui-dialog-features:dialog-api`
  (`DialogModule`) and the `:analytics:tag-picker` module from step 6 declare DI.
- No `RootRouter` change (dialogs live in the shared `SlotNavigation<DialogSession>`).
- No UI provider or integration module.

### Common mistakes (MUST avoid)
- **Serializing live dialog callbacks/configs.** Both host navigators use `serializer = null`; preserve ordinary retained ownership without process-death restoration.
- **Missing `title` or typed input fields.** Configs carry caller-owned `UiText` and the current starting values.
- **Forgetting `hasSameContentAs`.** Every new subtype/content field needs a typed comparison excluding callbacks; host runtime IDs remain separate from content.
- **Forgetting to wire in `DialogContentComponent.createChild`.** Showing the config crashes at runtime.
- **Redundant outer `Column`, toolbar or duplicate host title in a leaf dialog.** Use the `BaseComposeScreen(...background.dialog)` + `Spacer`/body/`Spacer` convention. The host owns `BottomSheetToolbar`; leaf horizontal padding is `AppTokens.dp.dialog.horizontalPadding`, not `contentPadding.content`.
- **Using global `open` for nested navigation.** It is not bound to the originating step, so it pushes even after that step was covered or removed; use the originating step's `Session.push` / `replaceCurrent` for in-sheet navigation.
- **Inventing `DialogController.dismiss()` or `DialogContentViewModel.onBack`.** Emit `Direction.BackWithResult(value)` / `Direction.Back`; the existing child factory calls its bound `dialogNavigation.finish` with the pending result or null.
