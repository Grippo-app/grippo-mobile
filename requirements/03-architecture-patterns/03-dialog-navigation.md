# Dialog Navigation

Dialogs (bottom sheets) live in a **parallel** navigation graph, not in the main screen stack. The motivation: dialogs can be shown over any screen and dismissed independently of stack changes.

## Two parallel navigators

```
RootComponent
  ├── stack: StackNavigation<RootRouter>         // screens
  └── dialog: SlotNavigation<DialogConfig>       // bottom sheets (one at a time)
```

Both are rendered as siblings inside `RootComponent.Render()` (not nested inside `RootScreen`):

```kotlin
@Composable
override fun Render() {
    AppTheme(darkTheme = systemIsDark, localeTag = systemLocaleTag) {
        RootScreen(this, state.value, loaders.value, viewModel)
        dialogComponent.Render()       // overlays on top of the screen stack
    }
}
```

`RootScreen` itself only renders the `ChildStack`. The dialog overlay lives one composable layer up so it stays unaffected by screen transitions.

A **slot** navigator shows **at most one** child at a time. Multi-step bottom sheets manage their own internal stack inside their `State` (push/pop without dismissing the sheet).

## `DialogConfig` — the dialog identity

`DialogConfig` is a `@Serializable sealed class` in `:ui-dialog-features:dialog-api`:

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
        val initial: Float?,
        @Transient val onResult: (value: Float) -> Unit = {},
    ) : DialogConfig(onDismiss = null, dismissBySwipe = true) {
        override val key: String get() = buildKey("AmountPicker", initial)
    }

    @Serializable
    public data class Confirmation(
        val title: String,
        val description: String?,
        @Transient val onResult: () -> Unit = {},
    ) : DialogConfig(onDismiss = null, dismissBySwipe = true) {
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

Rules:

- **`@Serializable` on the sealed class and every subtype.** Decompose's `SlotNavigation` serializes the active config on process death.
- **`@Transient` on every callback** (`onResult`, `onConfirm`, `onCancel`, `onDismiss`). Lambdas cannot serialize — they're rehydrated to no-ops on process death. This is **intentional**: if the user backgrounds while a picker is open, the picker should still close on dismiss; the result simply won't fire (UI is expected to handle this by, e.g. not relying on a callback to re-fetch).
- **`key: String`** uniquely identifies the dialog instance. Decompose uses it to de-dup. `buildKey(...)` is length-prefixed (`"3:Foo|5:hello"` for parts `"Foo"`, `"hello"`) to avoid `|` collisions. `null` parts are written as `<null>` (length 6). It is a `protected` member of `DialogConfig`, so subtypes can call it from their own `key` getter.
- **No `@Transient` on data fields.** Inputs like `initial: Float?`, `title: String` are serialized so the picker can restart after process death with the same starting values.
- **`onDismiss` and `dismissBySwipe` are `open val`** in the base class, so subtypes can override them via super-class arguments (e.g. `ErrorDisplay` wires `onDismiss = onClose`; `Confirmation` sets `dismissBySwipe = false`).

## `DialogController` + `DialogProvider`

```kotlin
public interface DialogController {
    public fun show(config: DialogConfig)
}

public interface DialogProvider {
    public val dialog: Flow<DialogConfig>
}
```

The implementation lives in `:ui-dialog-features:dialog-api` (Koin `@Single(binds = [DialogController::class, DialogProvider::class])`) and uses a buffered `Channel<DialogConfig>` to relay `show(...)` calls to whichever component subscribes via `DialogProvider.dialog`. There is no `dismiss()` on the controller — dismissal is owned by `DialogComponent`/`DialogContentComponent` through their own back / `onBack` callbacks.

The controller is injected into ViewModels via Koin:

```kotlin
internal class FooViewModel(
    private val dialogController: DialogController,
    // ... other deps
) : BaseViewModel<...>(...) {

    fun onChangeAmountClick() {
        dialogController.show(
            DialogConfig.AmountPicker(
                initial = (state.value.amount as? AmountFormatState.Valid)?.value,
                onResult = { newValue ->
                    update { it.copy(amount = AmountFormatState.of(newValue)) }
                }
            )
        )
    }
}
```

The controller is decoupled from the dialog UI: it pushes the config onto a buffered channel; the consumer is `DialogViewModel`, which holds the dialog stack in its state. `DialogComponent` reconciles its `SlotNavigation`/`StackNavigation` against that state.

## `DialogComponent` / `DialogContentComponent` shape

There are two components, both in `:shared/dialog/...`:

1. **`DialogComponent`** owns the **outer** slot (`SlotNavigation<DialogConfig>`). The slot holds at most one child: an instance of `DialogContentComponent`. Its `DialogViewModel` consumes `DialogProvider.dialog` and updates `DialogState.sessionConfig` / `innerConfigs`. A reconciliation loop activates/dismisses the slot to match `state.sessionConfig`.

2. **`DialogContentComponent`** owns the **inner** stack (`StackNavigation<DialogConfig>`). This stack is what powers multi-step bottom sheets — opening a second dialog from inside a first does **not** close the sheet; it pushes onto this inner stack.

Sketch (abridged):

```kotlin
internal class DialogComponent(
    componentContext: ComponentContext,
) : BaseComponent<DialogDirection>(componentContext) {

    override val viewModel = componentContext.retainedInstance {
        DialogViewModel(dialogProvider = getKoin().get())
    }

    private val dialog = SlotNavigation<DialogConfig>()

    internal val childSlot: Value<ChildSlot<DialogConfig, Child>> = childSlot(
        source = dialog,
        serializer = DialogConfig.serializer(),
        key = "DialogComponent",
        handleBackButton = true,
        childFactory = { config, ctx -> Child.Content(DialogContentComponent(config, ctx, back = viewModel::onDismiss)) },
    )

    init {
        lifecycle.doOnCreate {
            viewModel.state
                .map { ReconcileTarget(it.sessionConfig, it.innerConfigs) }
                .distinctUntilChanged()
                .onEach(::reconcile)
                .launchIn(reconcileScope)
        }
    }

    private fun reconcile(target: ReconcileTarget) { /* dialog.activate / dialog.dismiss + push/pop on the inner stack */ }
}

internal class DialogContentComponent(
    initial: DialogConfig,
    componentContext: ComponentContext,
    private val back: (pendingResult: (() -> Unit)?) -> Unit,
) : BaseComponent<DialogContentDirection>(componentContext) {

    internal val navigation = StackNavigation<DialogConfig>()

    internal val childStack: Value<ChildStack<DialogConfig, Child>> = childStack(
        source = navigation,
        serializer = DialogConfig.serializer(),
        initialStack = { listOf(initial) },
        key = "DialogContentComponent",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(router: DialogConfig, context: ComponentContext): Child = when (router) {
        is DialogConfig.AmountPicker -> /* AmountPickerComponent(..., onResult = { viewModel.onBack { router.onResult(it) } }) */
        is DialogConfig.Confirmation -> /* ConfirmationComponent(..., onResult = { viewModel.onBack { router.onResult() } }) */
        // ... one branch per DialogConfig subtype
    }
}
```

`DialogComponent` is created by `RootComponent` and rendered alongside `RootScreen` (`AppTheme { RootScreen(...); dialogComponent.Render() }`).

Pending callbacks (`router.onResult(value)`) are captured into a `pendingResult` lambda that `DialogContentViewModel.onBack(pendingResult)` later invokes through the outer `DialogViewModel` — this guarantees the sheet is dismissed **before** the callback runs, so any state updates the callback triggers can show their own dialog without colliding with the closing one.

## In-sheet navigation

A dialog flow may "open another dialog" without closing the sheet (e.g. "pick a tag → open its details"). This is implemented as a **Decompose `StackNavigation<DialogConfig>` inside `DialogContentComponent`**: the consumer ViewModel calls `dialogController.show(<NextConfig>)`, `DialogViewModel.innerConfigs` grows, and `DialogComponent.reconcile(...)` pushes onto `DialogContentComponent.navigation`.

Two consequences:

1. Each step is a fully independent dialog feature (its own `Component`/`ViewModel`/`Screen` package under `:ui-dialog-features:<x>`). The current sheet content swaps via the inner Decompose stack rather than via a Compose `AnimatedContent` over a `state.step` field.
2. Single-step dialogs do not need to model an internal "step" enum in their `State`. They just collect input and emit `onResult(...)`.

If a dialog truly is multi-mode (radio-button-like, no nested navigation), modelling the mode inside the dialog's own `State` as a `sealed interface` is fine — but that's separate from cross-dialog navigation.

## Returning a result

Two patterns; the picker uses both depending on context.

### Callback in `DialogConfig` (default)

```kotlin
dialogController.show(
    DialogConfig.AmountPicker(
        initial = 72f,
        onResult = { value -> update { it.copy(amount = AmountFormatState.of(value)) } }
    )
)
```

`AmountPickerViewModel.onApplyClick()` calls `onResult(value)` (the lambda from `DialogConfig`) and then `navigateTo(AmountPickerDirection.Close)`.

This is **the** default — local, explicit, scoped to one call site.

### `ResultManager` (when callback cannot be threaded)

Used when the caller and the responder are in **different lifecycle scopes** (e.g. a dialog opened another dialog, and the result needs to reach the original initiator). See `03-architecture-patterns/04-cross-component-results.md`.

## Dismissal behavior

`onDismiss` and `dismissBySwipe` are the two knobs every `DialogConfig` subtype can override (their base declarations as `open val` were quoted at the top of this file).

- `onDismiss` — invoked from `DialogViewModel.onRelease(config)` once the bottom sheet finishes its hide animation, regardless of *how* it closed (system back, swipe, or a confirmed action that triggered `viewModel.onBack(pendingResult = ...)`). Use it for analytics or cleanup that must run exactly once per dialog session. The successful-action callback (`onResult`, `onConfirm`, …) runs **before** `onDismiss` on the same release tick — so if the user picks a value, both fire in that order; if the user swipes away, only `onDismiss` fires.
- `dismissBySwipe` — set `false` for confirmation-style dialogs where dismissal must be explicit. It controls both the swipe-to-dismiss gesture and the back-press capture inside the sheet's `ModalBottomSheetProperties`.

## Rules summary

- **One `DialogConfig` per dialog flow.** Don't multiplex configs for similar flows.
- **All inputs serializable, all callbacks `@Transient`.**
- **`onResult` callbacks fire only on explicit success.** Dismiss vs result are separate.
- **Cross-dialog flows use `DialogContentComponent`'s inner Decompose `StackNavigation<DialogConfig>`.** Re-issuing `dialogController.show(<NextConfig>)` from within an active sheet pushes onto that inner stack — the sheet stays mounted and the next dialog feature's `Component`/`ViewModel`/`Screen` swaps in.
- **Single-dialog multi-mode** (radio-button-like swaps inside one feature, no nested navigation) lives in the dialog's own `State` as a `sealed interface` — that's an in-feature concern, not cross-dialog.
- **Dialogs do not invoke `:data-features:feature-api` for the data they collect.** They build a value and hand it back to the caller via `onResult`. The caller persists it through its Feature.
- **Exception: confirmation dialogs that need to invoke a side effect themselves** (e.g. delete confirmation that calls `feature.delete()` before dismissing) — these depend on `:data-features:feature-api`. Document the exception in the dialog's package-level comment.
