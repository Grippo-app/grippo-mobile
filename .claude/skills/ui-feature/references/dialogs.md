# Dialogs (Bottom Sheets) — Navigation & Recipe

Dialogs (bottom sheets) live in a **parallel** navigation graph, not in the main
screen stack: they can be shown over any screen and dismissed independently of
stack changes.

## Two parallel navigators

```
RootComponent
  ├── stack: StackNavigation<RootRouter>         // screens
  └── dialog: SlotNavigation<DialogConfig>       // bottom sheets (one at a time)
```

Both render as siblings inside `RootComponent.Render()` (not nested inside `RootScreen`):

```kotlin
@Composable
override fun Render() {
    AppTheme(darkTheme = systemIsDark, localeTag = systemLocaleTag) {
        RootScreen(this, state.value, loaders.value, viewModel)
        dialogComponent.Render()       // overlays on top of the screen stack
    }
}
```

A **slot** navigator shows **at most one** child at a time. Multi-step bottom
sheets manage their own internal stack (push/pop without dismissing the sheet).

## `DialogConfig` — the dialog identity

`@Serializable sealed class` in `:ui-dialog-features:dialog-api`:

```kotlin
@Serializable
public sealed class DialogConfig(
    @Transient public open val onDismiss: (() -> Unit)? = null,
    public open val dismissBySwipe: Boolean = true,
) {
    public abstract val key: String

    protected fun buildKey(vararg parts: Any?): String = buildString {
        parts.forEachIndexed { index, part ->
            if (index > 0) append('|')
            val value = part?.toString() ?: "<null>"
            append(value.length); append(':'); append(value)
        }
    }

    @Serializable
    public data class AmountPicker(
        val initial: Double?,
        @Transient val onResult: (value: Double) -> Unit = {},
    ) : DialogConfig(onDismiss = null, dismissBySwipe = true) {
        override val key: String get() = buildKey("AmountPicker", initial)
    }

    @Serializable
    public data class Confirmation(
        val title: String,
        val description: String?,
        @Transient val onResult: () -> Unit = {},
    ) : DialogConfig(onDismiss = null, dismissBySwipe = false) {
        override val key: String get() = buildKey("Confirmation", title, description)
    }

    @Serializable
    public data class ErrorDisplay(
        val error: AppErrorState,
        @Transient val onClose: () -> Unit = {},
    ) : DialogConfig(onDismiss = onClose, dismissBySwipe = true) {
        override val key: String get() = buildKey("ErrorDisplay", error)
    }
    // ... one per dialog flow
}
```

Rules (MUST):
- **`@Serializable` on the sealed class and every subtype.** `SlotNavigation` serializes the active config on process death.
- **`@Transient` on every callback** (`onResult`, `onConfirm`, `onCancel`, `onDismiss`). Lambdas cannot serialize; they're rehydrated to no-ops on process death — intentional.
- **`key: String`** uniquely identifies the dialog instance. `buildKey(...)` is length-prefixed (`"3:Foo|5:hello"`) to avoid `|` collisions; `null` parts → `<null>` (length 6). `buildKey` is a `protected` member — call it from subtype `get()` only.
- **No `@Transient` on data fields.** Inputs (`initial: Double?`, `title: String`) are serialized so the dialog can restart after process death with the same starting values.
- **`onDismiss` and `dismissBySwipe` are `open val`** in the base class; subtypes override them via super-class arguments (`ErrorDisplay` wires `onDismiss = onClose`; `Confirmation` sets `dismissBySwipe = false`).

## `DialogController` + `DialogProvider`

```kotlin
public interface DialogController { public fun show(config: DialogConfig) }
public interface DialogProvider { public val dialog: Flow<DialogConfig> }
```

Impl in `:ui-dialog-features:dialog-api` (`@Single(binds = [DialogController::class, DialogProvider::class])`), using a buffered `Channel<DialogConfig>` to relay `show(...)` to the consumer. There is **no `dismiss()`** on the controller — dismissal is owned by `DialogComponent`/`DialogContentComponent` via their own back/`onBack`. Do not invent a `dismiss()`.

The controller is injected into ViewModels via Koin; a VM calls `dialogController.show(DialogConfig.X(...))`.

## `DialogComponent` / `DialogContentComponent` shape

Two components, both in `:shared/dialog/...`:
1. **`DialogComponent`** owns the **outer** slot (`SlotNavigation<DialogConfig>`), holding at most one child (a `DialogContentComponent`). Its `DialogViewModel` consumes `DialogProvider.dialog` and updates `DialogState.sessionConfig` / `innerConfigs`; a reconciliation loop activates/dismisses the slot to match state.
2. **`DialogContentComponent`** owns the **inner** stack (`StackNavigation<DialogConfig>`) powering multi-step bottom sheets — opening a second dialog from inside a first pushes onto this inner stack instead of closing the sheet.

Pending callbacks (`router.onResult(value)`) are captured into a `pendingResult` lambda that `DialogContentViewModel.onBack(pendingResult)` invokes through the outer `DialogViewModel` — guaranteeing the sheet is dismissed **before** the callback runs.

## In-sheet navigation

A dialog flow may "open another dialog" without closing the sheet: the consumer VM calls `dialogController.show(<NextConfig>)`, `DialogViewModel.innerConfigs` grows, and the reconcile pushes onto `DialogContentComponent.navigation`. Each step is a fully independent dialog feature (own `Component`/`ViewModel`/`Screen` under `:ui-dialog-features:<x>`). Single-step dialogs don't model a "step" enum. If a dialog truly is multi-mode (radio-button-like, no nested navigation), model the mode inside the dialog's own `State` as a `sealed interface` — separate from cross-dialog navigation.

## Returning a result

Default — **callback in `DialogConfig`** (local, explicit, scoped to one call site):

```kotlin
dialogController.show(
    DialogConfig.AmountPicker(
        initial = 72.0,
        onResult = { value -> update { it.copy(amount = AmountFormatState.of(value)) } }
    )
)
```

`AmountPickerViewModel.onApplyClick()` calls `onResult(value)` then `navigateTo(AmountPickerDirection.Close)`.

When the caller and responder are in **different lifecycle scopes**, use `ResultManager` (`references/results.md`).

## Dismissal behavior

- `onDismiss` — invoked from `DialogViewModel.onRelease(config)` once the sheet finishes hiding, **however** it closed. Use for analytics/cleanup that must run exactly once per session. The success callback (`onResult`/`onConfirm`) runs **before** `onDismiss` on the same release tick; on a swipe-away only `onDismiss` fires.
- `dismissBySwipe` — set `false` for confirmation-style dialogs where dismissal must be explicit. Controls both the swipe gesture and the back-press capture inside `ModalBottomSheetProperties`.

## Rules summary (MUST)

- **One `DialogConfig` per dialog flow.** Don't multiplex configs for similar flows.
- **All inputs serializable, all callbacks `@Transient`.**
- **`onResult` callbacks fire only on explicit success.** Dismiss vs result are separate.
- **Cross-dialog flows use `DialogContentComponent`'s inner `StackNavigation<DialogConfig>`.** Re-issuing `dialogController.show(<NextConfig>)` from an active sheet pushes onto the inner stack.
- **Single-dialog multi-mode** lives in the dialog's own `State` as a `sealed interface`.
- **Dialogs do not invoke `:data-features:feature-api`** for the data they collect — they build a value and hand it back via `onResult`; the caller persists it.
- **Exception**: confirmation dialogs that invoke a side effect themselves (e.g. delete confirmation calling `feature.delete()` before dismissing) depend on `:data-features:feature-api`. Document the exception in the dialog's package-level comment.

---

# Recipe — Add a Dialog (e.g. `:ui-dialog-features:tag-picker`)

> Figma-enabled (`figmaEnabled: true`) + a `## Design` bullet: the design-cache gate applies BEFORE any file is written (see SKILL.md "Stop and ask").

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
    android { namespace = "com.<org>.<product>.ui.dialog.features.tag.picker" }
    sourceSets.commonMain.dependencies {
        implementation(projects.uiCore.foundation)
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

### 3. Create the seven MVI files (package `com.<org>.<product>.tag.picker`)

`TagPickerState.kt` — `@Immutable internal data class TagPickerState(val current: String = "")`. Defaults in ctor; **no `companion object Empty` on State** (Empty belongs on Contract).

`TagPickerDirection.kt`:
```kotlin
internal sealed interface TagPickerDirection : BaseDirection {
    data class BackWithResult(val value: String) : TagPickerDirection
    data object Back : TagPickerDirection
}
```

`TagPickerLoader.kt` — `@Immutable internal sealed interface TagPickerLoader : BaseLoader` (usually empty).

`TagPickerContract.kt` — `@Immutable interface` with `onValueChange`/`onApplyClick`/`onCloseClick` + `companion object Empty` of no-ops.

`TagPickerViewModel.kt`:
```kotlin
internal class TagPickerViewModel(initial: String) :
    BaseViewModel<TagPickerState, TagPickerDirection, TagPickerLoader>(
        TagPickerState(current = initial),
    ), TagPickerContract {
    override fun onValueChange(value: String) { update { it.copy(current = value) } }
    override fun onApplyClick() { navigateTo(TagPickerDirection.BackWithResult(state.value.current)) }
    override fun onCloseClick() { navigateTo(TagPickerDirection.Back) }
}
```

`TagPickerComponent.kt` — **`public class`** (dialog Components cross the module boundary; the shared `DialogContentComponent` builds them). Declares `back: () -> Unit` and `onResult: (T) -> Unit`; `Direction` carries the result via `BackWithResult(value)`, plain `Back` covers dismissals:
```kotlin
public class TagPickerComponent(
    componentContext: ComponentContext,
    private val initial: String,
    private val onResult: (String) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<TagPickerDirection>(componentContext) {
    override val viewModel: TagPickerViewModel = componentContext.retainedInstance {
        TagPickerViewModel(initial = initial)
    }
    private val backCallback = BackCallback(onBack = viewModel::onCloseClick)
    init { backHandler.register(backCallback) }
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

`TagPickerScreen.kt` — **no toolbar** (neither `Toolbar` nor `BottomSheetToolbar`). Use `BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog))` and lay out the `Column`: `Spacer(AppTokens.dp.dialog.top)` → centered title `Text` (style `h2()`, `textAlign = Center`) → body → `Spacer(AppTokens.dp.dialog.bottom)` → `Spacer(Modifier.navigationBarsPadding())`. Horizontal padding is `AppTokens.dp.dialog.horizontalPadding`. Include an `@AppPreview private fun ...Preview()` wrapped in `PreviewContainer { ... }` with `Contract.Empty`. Pick a real input control from `:design-system:components`/`:compose-libs` (the template uses custom `*WheelPicker`, not Material3 `Slider`). Dialog sub-composables that break out go in a `components/` subfolder, one per file.

### 4. Add the `DialogConfig` subtype in `:ui-dialog-features:dialog-api`
```kotlin
@Serializable
public data class TagPicker(
    val initial: String,
    @Transient val onResult: (String) -> Unit = { },
) : DialogConfig(onDismiss = null, dismissBySwipe = true) {
    override val key: String get() = buildKey("TagPicker", initial)
}
```

### 5. Wire in `DialogContentComponent.createChild` (in `:shared`)
`DialogContentComponent` exposes **no `dismiss()`**. Close from a child via `viewModel.onBack(pendingResult)` — non-null lambda fires the host callback after dismissal, `null` for silent dismissal:
```kotlin
is DialogConfig.TagPicker -> Child.TagPicker(
    TagPickerComponent(
        componentContext = context,
        initial = router.initial,
        onResult = { value -> viewModel.onBack { router.onResult.invoke(value) } },
        back = { viewModel.onBack(null) },
    )
)
// + a matching `data class TagPicker(override val component: TagPickerComponent) : Child(component)`
```

### 6. Add `:ui-dialog-features:tag-picker` to `:shared/build.gradle.kts`
```kotlin
implementation(projects.uiDialogFeatures.tagPicker)
```

### 7. Use the dialog from any ViewModel
```kotlin
override fun onPickTagClick() {
    dialogController.show(
        DialogConfig.TagPicker(
            initial = state.value.selectedTagId,
            onResult = { newTagId -> update { it.copy(selectedTagId = newTagId) } }
        )
    )
}
```

### Verify
```bash
./gradlew :shared:assembleSharedDebugXCFramework
./gradlew :androidApp:assembleDebug
```
Run, trigger the dialog, confirm the result callback fires.

### What you did NOT have to do
- No new Koin module (dialog features don't usually declare DI).
- No `:shared/Koin.kt` edit.
- No `RootRouter` change (dialogs live in `SlotNavigation<DialogConfig>`).

### Common mistakes (MUST avoid)
- **`onResult` not `@Transient`.** Lambdas can't serialize.
- **`initial` not a regular field.** Inputs are serialized so the picker resumes with the same starting value.
- **`buildKey` collisions.** Two configs with the same key are treated as the same dialog. Include enough discriminator data (`buildKey("TagPicker", initial, screenId)`).
- **Forgetting to wire in `DialogContentComponent.createChild`.** Showing the config crashes at runtime.
- **Wrapping in `Column { ... }` or using `Toolbar`/`BottomSheetToolbar`.** No dialog ships a toolbar — use the `BaseComposeScreen(...background.dialog)` + `Spacer`/title/body/`Spacer` convention. Horizontal padding is `AppTokens.dp.dialog.horizontalPadding`, not `contentPadding.content`.
- **Inventing a `DialogController.dismiss()`.** It only exposes `show(config)`. Emit `Direction.BackWithResult(value)` / `Direction.Back`; the `createChild` wiring translates that into `viewModel.onBack { router.onResult.invoke(value) }`.
