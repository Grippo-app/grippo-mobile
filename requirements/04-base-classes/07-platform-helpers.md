# Platform Helpers

`:ui-core:foundation/platform` houses the `expect/actual` helpers needed across screens. They are small, focused, and aggressively used.

## `collectAsStateMultiplatform`

```kotlin
// commonMain
@Composable
public expect fun <T> StateFlow<T>.collectAsStateMultiplatform(
    context: CoroutineContext = EmptyCoroutineContext,
): State<T>

// androidMain
@Composable
public actual fun <T> StateFlow<T>.collectAsStateMultiplatform(
    context: CoroutineContext,
): State<T> = collectAsStateWithLifecycle(context = context)

// iosMain
@Composable
public actual fun <T> StateFlow<T>.collectAsStateMultiplatform(
    context: CoroutineContext,
): State<T> = collectAsState(context)
```

### Why

- On **Android**, `collectAsStateWithLifecycle` from `androidx.lifecycle.compose` integrates with the Activity/Fragment lifecycle: emissions are paused while the host is below STARTED. This avoids wasted recompositions when the user navigates away briefly.
- On **iOS**, no equivalent exists; the standard `collectAsState` is used.
- Using `collectAsStateMultiplatform` everywhere keeps the same call site working on both targets.

### Use

```kotlin
@Composable
override fun Render() {
    val state = viewModel.state.collectAsStateMultiplatform()
    val loaders = viewModel.loaders.collectAsStateMultiplatform()
    FooScreen(state.value, loaders.value, viewModel)
}
```

Never use plain `collectAsState` for `viewModel.state` / `viewModel.loaders`. Use `collectAsStateMultiplatform` consistently.

## `platformAnimation` and `platformStackAnimator`

```kotlin
// commonMain
public expect fun <C : Any, T : Any> platformAnimation(): StackAnimation<C, T>
public expect fun platformStackAnimator(): StackAnimator

// androidMain
public actual fun <C : Any, T : Any> platformAnimation(): StackAnimation<C, T> =
    stackAnimation(animator = platformStackAnimator())

public actual fun platformStackAnimator(): StackAnimator = (fade() + slide())

// iosMain
public actual fun <C : Any, T : Any> platformAnimation(): StackAnimation<C, T> =
    stackAnimation(animator = platformStackAnimator())

public actual fun platformStackAnimator(): StackAnimator = iosLikeSlide()

private fun iosLikeSlide(animationSpec: FiniteAnimationSpec<Float> = tween()): StackAnimator =
    stackAnimator(animationSpec = animationSpec) { factor, direction ->
        Modifier
            .then(if (direction.isFront) Modifier else Modifier.fade(factor + 1F))
            .offsetXFactor(factor = if (direction.isFront) factor else factor * 0.5F)
    }

private fun Modifier.fade(factor: Float) =
    drawWithContent {
        drawContent()
        drawRect(color = Color(red = 0F, green = 0F, blue = 0F, alpha = (1F - factor) / 4F))
    }

private fun Modifier.offsetXFactor(factor: Float): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(x = (placeable.width.toFloat() * factor).toInt(), y = 0)
        }
    }
```

### Why

- On **Android**, a fade-plus-slide combination feels natural for forward/back navigation. Decompose's `fade() + slide()` builds a horizontal slide with cross-fade.
- On **iOS**, navigation should feel iOS-native. `iosLikeSlide` mimics the iOS slide-in-from-right + parallax: the outgoing screen moves out at half speed (`factor * 0.5F`) and dims slightly (`fade(factor + 1F)`).
- Using `platformStackAnimator` everywhere keeps the call site the same and produces platform-appropriate animations.

### Use in `*RootScreen`

```kotlin
@Composable
internal fun ProfileRootScreen(stack: Value<ChildStack<ProfileRouter, Child>>, ...) {
    ChildStack(
        stack = stack,
        animation = stackAnimation(animator = platformStackAnimator())
    ) { child ->
        child.instance.component.Render()
    }
}
```

For per-child custom animations (e.g. fade for the Auth screen, slide for everything else):

```kotlin
ChildStack(
    stack = stack,
    animation = stackAnimation { child, _, _, _ -> child.instance.animator() }
) { ... }

private fun Child.animator(): StackAnimator = when (this) {
    is Child.Auth -> fade()
    is Child.Home -> fade()
    else -> platformStackAnimator()
}
```

## Why `expect/actual` over interface+impl

For these helpers, `expect/actual` is **better** than the interface-with-impls pattern because:

1. No runtime DI — calls resolve at compile time.
2. No interface allocation overhead in Composables.
3. The platform branching is visible in the file structure (`PlatformAnimation.android.kt`, `PlatformAnimation.ios.kt`).
4. Compose handles `@Composable expect` properly.

The toolkit modules use a mix of `expect/actual` (for simple helpers like `NativeContext`, `AppTheme.current`, `AppLocale.current`) and interfaces (for `Connectivity`, `NotificationManager`, where the impl is large enough to warrant a separate class).

## Adding a new platform helper

If you need a new `expect/actual` Composable helper:

1. Declare it in `commonMain`:
   ```kotlin
   @Composable
   public expect fun rememberSomething(): Something
   ```
2. Implement in `androidMain`:
   ```kotlin
   @Composable
   public actual fun rememberSomething(): Something = ...
   ```
3. Implement in `iosMain`:
   ```kotlin
   @Composable
   public actual fun rememberSomething(): Something = ...
   ```
4. Live in a single-purpose file (one helper per file).
5. **Avoid** `expect/actual` if both implementations would be identical — just put the implementation in `commonMain`.

## Why these live in `:ui-core:foundation` (not in `:toolkit:*`)

`:toolkit:*` is the bottom of the dependency graph; it doesn't depend on `:ui-core:foundation`. Helpers in `:toolkit:*` are non-Composable / pre-UI. These helpers are Composable and tied to Decompose `ChildStack` — they belong with the UI infrastructure.

`AppTheme.current` / `AppLocale.current` live in `:toolkit:theme` / `:toolkit:localization` because they're consumed by `BackendClient` (for `Accept-Language`) and `RootScreen` (for theme); the toolkit module places them at the lowest layer that needs them.
