# Add a Dialog (Bottom Sheet)

How to add a new dialog feature — e.g. `:ui-dialog-features:rating-picker`.

## Steps

### 1. Add the module to `settings.gradle.kts`

```kotlin
include(":ui-dialog-features:rating-picker")
```

### 2. Create `:ui-dialog-features:rating-picker/build.gradle.kts`

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.rating.picker" }

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

### 3. Create the seven MVI files

`src/commonMain/kotlin/com/<org>/<product>/rating/picker/RatingPickerState.kt`:

```kotlin
@Immutable
public data class RatingPickerState(
    val current: Int = 3,
)
```

State takes default constructor values; do not declare a `companion object Empty` on State (Empty belongs on `Contract`).

`RatingPickerDirection.kt`:

```kotlin
public sealed interface RatingPickerDirection : BaseDirection {
    public data class BackWithResult(val value: Int) : RatingPickerDirection
    public data object Back : RatingPickerDirection
}
```

`RatingPickerLoader.kt`:

```kotlin
@Immutable
internal sealed interface RatingPickerLoader : BaseLoader
```

`RatingPickerContract.kt`:

```kotlin
@Immutable
internal interface RatingPickerContract {
    fun onValueChange(value: Int)
    fun onApplyClick()
    fun onCloseClick()

    @Immutable
    companion object Empty : RatingPickerContract {
        override fun onValueChange(value: Int) = Unit
        override fun onApplyClick() = Unit
        override fun onCloseClick() = Unit
    }
}
```

`RatingPickerViewModel.kt`:

```kotlin
internal class RatingPickerViewModel(
    initial: Int,
) : BaseViewModel<RatingPickerState, RatingPickerDirection, RatingPickerLoader>(
    RatingPickerState(current = initial),
), RatingPickerContract {

    override fun onValueChange(value: Int) {
        update { it.copy(current = value) }
    }

    override fun onApplyClick() {
        navigateTo(RatingPickerDirection.BackWithResult(state.value.current))
    }

    override fun onCloseClick() {
        navigateTo(RatingPickerDirection.Back)
    }
}
```

`RatingPickerComponent.kt`:

```kotlin
public class RatingPickerComponent(
    componentContext: ComponentContext,
    private val initial: Int,
    private val onResult: (Int) -> Unit,
    private val back: () -> Unit,
) : BaseComponent<RatingPickerDirection>(componentContext) {

    override val viewModel: RatingPickerViewModel = componentContext.retainedInstance {
        RatingPickerViewModel(initial = initial)
    }

    private val backCallback = BackCallback(onBack = viewModel::onCloseClick)

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: RatingPickerDirection) {
        when (direction) {
            is RatingPickerDirection.BackWithResult -> onResult.invoke(direction.value)
            RatingPickerDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        RatingPickerScreen(state.value, loaders.value, viewModel)
    }
}
```

The Component is `public class` (dialog Components are entry points the shared `DialogContentComponent` builds — they cross the module boundary). It declares a single `back: () -> Unit` and an `onResult: (T) -> Unit`; `Direction` carries the result via `BackWithResult(value)`, while plain `Back` covers user-initiated dismissals (close button, back gesture).

`RatingPickerScreen.kt`:

```kotlin
@Composable
internal fun RatingPickerScreen(
    state: RatingPickerState,
    loaders: ImmutableSet<RatingPickerLoader>,
    contract: RatingPickerContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {
    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    Text(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        text = AppTokens.strings.res(Res.string.rating_picker_title),
        style = AppTokens.typography.h2(),
        color = AppTokens.colors.text.primary,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    // Pick a real input control from :design-system:components or :compose-libs.
    // The reference repo uses custom *WheelPicker widgets, not Material3 Slider.

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    Button(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        content = ButtonContent.Text(text = AppTokens.strings.res(Res.string.apply)),
        style = ButtonStyle.Primary,
        onClick = contract::onApplyClick,
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

    Spacer(modifier = Modifier.navigationBarsPadding())
}

@AppPreview
@Composable
private fun RatingPickerScreenPreview() {
    PreviewContainer {
        RatingPickerScreen(
            state = RatingPickerState(current = 4),
            loaders = persistentSetOf(),
            contract = RatingPickerContract.Empty,
        )
    }
}
```

Module namespace follows the existing dialog convention: `com.<org>.<product>.ui.dialog.features.rating.picker` (reference repo prepends `ui.dialog.features.` to every dialog module).

### 4. Add the `DialogConfig` subtype in `:ui-dialog-features:dialog-api`

In `DialogConfig.kt`:

```kotlin
@Serializable
public sealed class DialogConfig(
    @Transient public open val onDismiss: (() -> Unit)? = null,
    public open val dismissBySwipe: Boolean = true,
) {
    public abstract val key: String

    protected fun buildKey(vararg parts: Any?): String = /* … */

    // ... other subtypes

    @Serializable
    public data class RatingPicker(
        val initial: Int,
        @Transient val onResult: (Int) -> Unit = { },
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true,
    ) {
        override val key: String get() = buildKey("RatingPicker", initial)
    }
}
```

`onDismiss` and `dismissBySwipe` are `open` so subtypes can override them at construction (e.g. `dismissBySwipe = false` for blocking sheets like `StartTraining`). `buildKey` is `protected` — call it from subtype `get()` only.

### 5. Wire it in `DialogContentComponent.createChild` (in `:shared`)

`DialogContentComponent` does not expose a `dismiss()` method. To close the sheet from a child, call `viewModel.onBack(pendingResult)` — pass a non-null lambda to fire the host callback after dismissal, or `null` for a silent dismissal.

```kotlin
private fun createChild(router: DialogConfig, context: ComponentContext): Child = when (router) {
    // ... other branches
    is DialogConfig.RatingPicker -> Child.RatingPicker(
        RatingPickerComponent(
            componentContext = context,
            initial = router.initial,
            onResult = { value -> viewModel.onBack { router.onResult.invoke(value) } },
            back = { viewModel.onBack(null) },
        )
    )
}

internal sealed class Child(open val component: BaseComponent<*>) {
    // ...
    data class RatingPicker(override val component: RatingPickerComponent) : Child(component)
}
```

### 6. Add `:ui-dialog-features:rating-picker` to `:shared/build.gradle.kts`

```kotlin
sourceSets.commonMain.dependencies {
    // ...
    implementation(projects.uiDialogFeatures.ratingPicker)
}
```

### 7. Use the dialog from any ViewModel

```kotlin
internal class FeedbackViewModel(
    private val dialogController: DialogController,
) : BaseViewModel<...>(...), FeedbackContract {

    override fun onChangeRatingClick() {
        dialogController.show(
            DialogConfig.RatingPicker(
                initial = state.value.rating,
                onResult = { newRating ->
                    update { it.copy(rating = newRating) }
                }
            )
        )
    }
}
```

## Verify

```bash
./gradlew :shared:assembleSharedDebugXCFramework
./gradlew :androidApp:assembleDebug
```

Run, trigger the dialog, confirm result callback fires.

## What you did NOT have to do

- **No new Koin module.** Dialog features don't usually declare DI.
- **No `:shared/Koin.kt` edit.** Same reason.
- **No `RootRouter` change.** Dialogs live in `SlotNavigation<DialogConfig>`, parallel to the screen stack.

## Common mistakes

- **`onResult` not marked `@Transient`.** Lambdas can't serialize; build fails or process death drops the callback.
- **`initial` not declared as a regular field.** Inputs are serialized so the picker resumes with the same starting value.
- **`buildKey` collisions.** Two configs with the same key are treated as the same dialog. Include enough discriminator data (e.g. `buildKey("RatingPicker", initial, screenId)`).
- **Forgetting to wire in `DialogContentComponent.createChild`.** Showing the config crashes at runtime.
- **Wrapping in `Column { ... }` or using `Toolbar` / `BottomSheetToolbar`.** No existing dialog ships a toolbar — the reference convention is `BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog))` with a `Spacer(AppTokens.dp.dialog.top)`, a centered title `Text`, the body, then `Spacer(AppTokens.dp.dialog.bottom)` + `Spacer(Modifier.navigationBarsPadding())`. Horizontal padding is `AppTokens.dp.dialog.horizontalPadding`, not `contentPadding.content`.
- **Inventing a `DialogController.dismiss()` call from inside the picker.** `DialogController` only exposes `show(config)`. Closing the sheet is the host's job — emit `Direction.BackWithResult(value)` (or plain `Direction.Back`); the wiring in `DialogContentComponent.createChild` translates that into `viewModel.onBack { router.onResult.invoke(value) }`, which dismisses the sheet and then fires the host callback.
