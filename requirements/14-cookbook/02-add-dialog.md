# Add a Dialog (Bottom Sheet)

How to add a new dialog feature — e.g. `:ui-dialog-features:tag-picker`.

## Steps

### 1. Add the module to `settings.gradle.kts`

```kotlin
include(":ui-dialog-features:tag-picker")
```

### 2. Create `:ui-dialog-features:tag-picker/build.gradle.kts`

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.tag.picker" }

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

`src/commonMain/kotlin/com/<org>/<product>/tag/picker/TagPickerState.kt`:

```kotlin
@Immutable
public data class TagPickerState(
    val current: String = "",
)
```

State takes default constructor values; do not declare a `companion object Empty` on State (Empty belongs on `Contract`).

`TagPickerDirection.kt`:

```kotlin
public sealed interface TagPickerDirection : BaseDirection {
    public data class BackWithResult(val value: String) : TagPickerDirection
    public data object Back : TagPickerDirection
}
```

`TagPickerLoader.kt`:

```kotlin
@Immutable
internal sealed interface TagPickerLoader : BaseLoader
```

`TagPickerContract.kt`:

```kotlin
@Immutable
internal interface TagPickerContract {
    fun onValueChange(value: String)
    fun onApplyClick()
    fun onCloseClick()

    @Immutable
    companion object Empty : TagPickerContract {
        override fun onValueChange(value: String) = Unit
        override fun onApplyClick() = Unit
        override fun onCloseClick() = Unit
    }
}
```

`TagPickerViewModel.kt`:

```kotlin
internal class TagPickerViewModel(
    initial: String,
) : BaseViewModel<TagPickerState, TagPickerDirection, TagPickerLoader>(
    TagPickerState(current = initial),
), TagPickerContract {

    override fun onValueChange(value: String) {
        update { it.copy(current = value) }
    }

    override fun onApplyClick() {
        navigateTo(TagPickerDirection.BackWithResult(state.value.current))
    }

    override fun onCloseClick() {
        navigateTo(TagPickerDirection.Back)
    }
}
```

`TagPickerComponent.kt`:

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

    init {
        backHandler.register(backCallback)
    }

    override suspend fun eventListener(direction: TagPickerDirection) {
        when (direction) {
            is TagPickerDirection.BackWithResult -> onResult.invoke(direction.value)
            TagPickerDirection.Back -> back.invoke()
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        TagPickerScreen(state.value, loaders.value, viewModel)
    }
}
```

The Component is `public class` (dialog Components are entry points the shared `DialogContentComponent` builds — they cross the module boundary). It declares a single `back: () -> Unit` and an `onResult: (T) -> Unit`; `Direction` carries the result via `BackWithResult(value)`, while plain `Back` covers user-initiated dismissals (close button, back gesture).

`TagPickerScreen.kt`:

```kotlin
@Composable
internal fun TagPickerScreen(
    state: TagPickerState,
    loaders: ImmutableSet<TagPickerLoader>,
    contract: TagPickerContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog)) {
    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    Text(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        text = AppTokens.strings.res(Res.string.tag_picker_title),
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
private fun TagPickerScreenPreview() {
    PreviewContainer {
        TagPickerScreen(
            state = TagPickerState(current = "tag-id-1"),
            loaders = persistentSetOf(),
            contract = TagPickerContract.Empty,
        )
    }
}
```

Module namespace follows the existing dialog convention: `com.<org>.<product>.ui.dialog.features.tag.picker` (reference repo prepends `ui.dialog.features.` to every dialog module).

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
    public data class TagPicker(
        val initial: String,
        @Transient val onResult: (String) -> Unit = { },
    ) : DialogConfig(
        onDismiss = null,
        dismissBySwipe = true,
    ) {
        override val key: String get() = buildKey("TagPicker", initial)
    }
}
```

`onDismiss` and `dismissBySwipe` are `open` so subtypes can override them at construction (e.g. `dismissBySwipe = false` for blocking sheets that must complete a flow before dismissal). `buildKey` is `protected` — call it from subtype `get()` only.

### 5. Wire it in `DialogContentComponent.createChild` (in `:shared`)

`DialogContentComponent` does not expose a `dismiss()` method. To close the sheet from a child, call `viewModel.onBack(pendingResult)` — pass a non-null lambda to fire the host callback after dismissal, or `null` for a silent dismissal.

```kotlin
private fun createChild(router: DialogConfig, context: ComponentContext): Child = when (router) {
    // ... other branches
    is DialogConfig.TagPicker -> Child.TagPicker(
        TagPickerComponent(
            componentContext = context,
            initial = router.initial,
            onResult = { value -> viewModel.onBack { router.onResult.invoke(value) } },
            back = { viewModel.onBack(null) },
        )
    )
}

internal sealed class Child(open val component: BaseComponent<*>) {
    // ...
    data class TagPicker(override val component: TagPickerComponent) : Child(component)
}
```

### 6. Add `:ui-dialog-features:tag-picker` to `:shared/build.gradle.kts`

```kotlin
sourceSets.commonMain.dependencies {
    // ...
    implementation(projects.uiDialogFeatures.tagPicker)
}
```

### 7. Use the dialog from any ViewModel

```kotlin
internal class NoteEditorViewModel(
    private val dialogController: DialogController,
) : BaseViewModel<...>(...), NoteEditorContract {

    override fun onPickTagClick() {
        dialogController.show(
            DialogConfig.TagPicker(
                initial = state.value.selectedTagId,
                onResult = { newTagId ->
                    update { it.copy(selectedTagId = newTagId) }
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
- **`buildKey` collisions.** Two configs with the same key are treated as the same dialog. Include enough discriminator data (e.g. `buildKey("TagPicker", initial, screenId)`).
- **Forgetting to wire in `DialogContentComponent.createChild`.** Showing the config crashes at runtime.
- **Wrapping in `Column { ... }` or using `Toolbar` / `BottomSheetToolbar`.** No existing dialog ships a toolbar — the reference convention is `BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.dialog))` with a `Spacer(AppTokens.dp.dialog.top)`, a centered title `Text`, the body, then `Spacer(AppTokens.dp.dialog.bottom)` + `Spacer(Modifier.navigationBarsPadding())`. Horizontal padding is `AppTokens.dp.dialog.horizontalPadding`, not `contentPadding.content`.
- **Inventing a `DialogController.dismiss()` call from inside the picker.** `DialogController` only exposes `show(config)`. Closing the sheet is the host's job — emit `Direction.BackWithResult(value)` (or plain `Direction.Back`); the wiring in `DialogContentComponent.createChild` translates that into `viewModel.onBack { router.onResult.invoke(value) }`, which dismisses the sheet and then fires the host callback.
