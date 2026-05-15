# Compose-Specific Rules

Compose has its own conventions on top of Kotlin style. These rules are enforced by code review (and partially by the Compose compiler's stability metrics).

## Stability

### State classes

Every state class is `@Immutable`:

```kotlin
@Immutable
internal data class ProfileBodyState(
    val user: UserState? = null,
    val weight: WeightFormatState = WeightFormatState.Empty(),
    val height: HeightFormatState = HeightFormatState.Empty(),
    val history: ImmutableList<WeightHistoryState> = persistentListOf(),
)
```

(Fields use default constructor values so the ViewModel can build the initial state with `ProfileBodyState()` — see `09-conventions/02-naming.md` § "State defaults". `User` / `WeightPoint` are domain types; the UI state holds their `*State` counterparts from `:ui-core:state`.)

- `@Immutable` tells Compose: instances are deeply immutable. Recomposition skips on identity.
- `@Stable` is weaker — equal-by-equals but not deeply immutable. Use when fields may change but `equals` reflects the change.
- **`var` fields require `@Stable`** (or you forfeit skip optimization). Don't use `var` in state classes anyway.

### Collections

```kotlin
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

val items: ImmutableList<Foo> = persistentListOf(a, b, c)
```

- **Use `ImmutableList<T>` / `ImmutableSet<T>` / `PersistentList<T>`** in state.
- **`List<T>` / `Set<T>` / `Map<T>`** (Kotlin's defaults) are **not Compose-stable** — they degrade skipping.
- **`buildList { ... }`** then `.toImmutableList()` is the construction pattern.

### Sealed types

```kotlin
@Immutable
sealed interface MyState {
    @Immutable data object Empty : MyState
    @Immutable data class Loaded(val items: ImmutableList<Foo>) : MyState
}
```

`@Immutable` on the parent AND every subtype. Compose's stability inference doesn't auto-propagate.

## Composables

### Function signature

```kotlin
@Composable
public fun Button(
    modifier: Modifier = Modifier,
    content: ButtonContent,
    style: ButtonStyle = ButtonStyle.Primary,
    state: ButtonState = ButtonState.Enabled,
    size: ButtonSize = ButtonSize.Medium,
    onClick: () -> Unit,
    textStyle: TextStyle = AppTokens.typography.b14Bold(),
)
```

Conventions:

- **`PascalCase`** function name.
- **`modifier: Modifier = Modifier`** is the **first** parameter (mirrors the design-system `Button`, `Toolbar`, `BannerCard`, `BottomSheetToolbar`, ...). The exception is `Toggle`, where `modifier` trails `onCheckedChange` — a known outlier, not a pattern to copy.
- **Required callbacks (`on<X>`)** come after the styling parameters and before any trailing defaults.
- **Defaults at the end** of the parameter list (after the required callback).
- **`@Composable`** annotation explicit.
- **`public`** for design-system components, **`internal`** for feature screens/sub-screens.

### Screen function signature

Every `<Name>Screen.kt`:

```kotlin
@Composable
internal fun <Name>Screen(
    state: <Name>State,
    loaders: ImmutableSet<<Name>Loader>,
    contract: <Name>Contract,
)
```

Three arguments, same order, same names. Optional fourth: `component: <Name>Component` when the screen renders a child stack/slot.

### `@ReadOnlyComposable`

For getters that only read CompositionLocals and never affect recomposition:

```kotlin
@Stable
public object AppTokens {
    public val colors: AppColor
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
    // ... typography, dp, icons, strings, drawables (same shape)
}
```

`AppTokens` is a `@Stable` `object`; each token (`colors`, `typography`, `dp`, ...) is a regular `public val` member with `@Composable @ReadOnlyComposable` get — **not** an extension property. `@ReadOnlyComposable` lets Compose inline the call — no recomposition tracking.

## Side effects

### `LaunchedEffect`

```kotlin
LaunchedEffect(systemLocaleTag) {
    DateFormatting.install(systemLocaleTag)
}
```

- **Key the effect** on whatever should re-trigger it (e.g. `systemLocaleTag`, `state.range`).
- **`LaunchedEffect(Unit)` is forbidden for navigation.** Navigation goes via `Direction` + `eventListener`.
- **`LaunchedEffect(Unit)` is acceptable** only for one-time side effects that genuinely depend on no input (e.g. collecting a `snapshotFlow` for the lifetime of the composable). If the effect depends on any value — even an implicit one like the system locale — key on that value. The snippet above is the actual `RootComponent.Render` call site.

### `DisposableEffect`

For setup that needs teardown:

```kotlin
DisposableEffect(state.isMonitoring) {
    val callback = ConnectivityCallback { /* ... */ }
    nativeConnectivity.register(callback)
    onDispose { nativeConnectivity.unregister(callback) }
}
```

### `produceState`

For a value derived from a slow source:

```kotlin
val rendered by produceState(initialValue = "", state.weight) {
    value = computeRenderedWeight(state.weight)
}
```

### `derivedStateOf`

For derived state that depends on multiple inputs but should update only when the result changes:

```kotlin
val isSubmitButtonEnabled by remember(state.email, state.password) {
    derivedStateOf {
        state.email is EmailFormatState.Valid &&
            state.password is PasswordFormatState.Valid &&
            FooLoader.Submitting !in loaders
    }
}
```

### Plain `remember`

For computed values that are pure functions of inputs:

```kotlin
val buttonState = remember(loaders, state.weight) {
    when {
        FooLoader.SavingWeight in loaders -> ButtonState.Loading
        state.weight is WeightFormatState.Valid -> ButtonState.Enabled
        else -> ButtonState.Disabled
    }
}
```

Use `remember(...)` for **derived UI state** to avoid recomputing on every recomposition.

## Modifiers

### Order matters

```kotlin
Modifier
    .fillMaxWidth()                // size
    .padding(16.dp)                // outer padding (inside parent)
    .background(...)               // background
    .clip(RoundedCornerShape(8.dp))// shape
    .clickable { ... }              // interaction
    .padding(8.dp)                  // inner padding (inside background)
```

**General order:**

1. Size (`fillMax*`, `width`, `height`).
2. Outer padding / offset.
3. Background / border.
4. Shape (clip).
5. Interaction (clickable, focusable).
6. Inner padding.
7. Drawing modifiers (`drawBehind`, `graphicsLayer`).

Different orders produce different rendering — not a free choice.

### `Modifier.then(...)`

```kotlin
Modifier
    .padding(8.dp)
    .then(if (enabled) Modifier.clickable { ... } else Modifier)
```

Use for conditional modifiers. Don't build modifier lists via `mutableListOf`.

### Extension modifiers

```kotlin
private fun Modifier.bannerCardSurface(elevation: Dp): Modifier =
    this
        .shadow(elevation = elevation, shape = RoundedCornerShape(AppTokens.dp.bannerCard.radius))
        .background(AppTokens.colors.background.card)
```

For frequently-repeated patterns, extract a `Modifier` extension. Live in the same file (private) or in `:design-system:components` if shared.

Note: there is no general-purpose `AppTokens.dp.radius.*` slot — the internal `AppDp.radius` scale is `private`. Read radii from the component-scoped group that matches the surface you're styling (`AppTokens.dp.bannerCard.radius`, `AppTokens.dp.wheelPicker.radius`, `AppTokens.dp.tooltip.radius`, ...).

## Lists

### Always `key = { it.id }`

```kotlin
LazyColumn(
    contentPadding = PaddingValues(horizontal = AppTokens.dp.screen.horizontalPadding),
) {
    items(
        items = state.trainings,
        key = { it.id },
        contentType = { "TrainingRow" },
    ) { training ->
        TrainingRow(
            state = training,
            modifier = Modifier.animateItem(),
            onClick = { contract.onTrainingClick(training.id) },
        )
    }
}
```

- **`key = { it.id }`** is mandatory. Without it, animation breaks; identity is lost across recompositions.
- **`Modifier.animateItem()`** for transitions when items reorder/insert/delete.
- **`contentType = { "..." }`** for performance — Compose reuses composables of the same content type.

## Recomposition discipline

- **Don't pass lambda parameters** that capture changing state without `remember`. Wrap in `remember { ... }` if necessary.
- **Use `key` parameters in `remember`** to invalidate the cache when inputs change.
- **Hoist state to the lowest common parent.** Don't lift more than necessary.
- **Avoid re-creating `Modifier` chains** inside loops without `remember`.

## Layout primitives

- **`Column` / `Row` / `Box`** — basic layouts. Use over `ConstraintLayout` unless ConstraintLayout's specific features are needed.
- **`LazyColumn` / `LazyRow`** — for collections > 10 items.
- **`Spacer(Modifier.height(...))`** for vertical spacing between siblings. Prefer this over `Modifier.padding(top = ..., bottom = ...)` on individual items.
- **`Arrangement.spacedBy(...)`** for uniform spacing in a Column/Row.

## Material3

Interactive and structural Material3 components (`Button`, `TextField`, `Card`, `Toolbar`, sheets, dialogs, chips, indicators, ...) are wrapped in `:design-system:components`. **Outside** the design system module, **do not import those wrapped primitives directly**:

```kotlin
// ❌ in a feature module
import androidx.compose.material3.Button
Button(onClick = ...) { Text("Click") }

// ✅ use the design-system wrapper
import com.<org>.<product>.design.components.Button
Button(onClick = ..., text = "Click")
```

The wrappers apply `AppTokens` consistently.

**Exception — unwrapped Material3 primitives.** A small set of layout-level primitives are not wrapped and are imported directly in feature modules:

- `androidx.compose.material3.Text` — pass styles via `AppTokens.typography.*` and colors via `AppTokens.colors.*`.
- `androidx.compose.material3.Icon` — pass `tint` via `AppTokens.colors.*`.
- `androidx.compose.material3.rememberTooltipState` and tooltip primitives.

If you find yourself reaching for any other `androidx.compose.material3.*` symbol in a feature module, add a wrapper to `:design-system:components` first.

## Anti-patterns

- **`LaunchedEffect(Unit) { navigate(...) }`** — navigation goes via `Direction` + `eventListener`.
- **`mutableStateOf(...)` inside a `Composable` as a "global"** — local state for UI; logical state belongs in ViewModel.
- **Calling `getKoin().get()` in a Composable.** Forbidden. Get deps via Component → ViewModel.
- **`@Composable val` properties without `@ReadOnlyComposable`** when the value doesn't depend on observable state.
- **`if (loaders.contains(MyLoader.Foo)) ... else ...`** inline. Wrap in `remember(loaders) { ... }`.
- **Heavy work in a `Composable`** without `LaunchedEffect`/`produceState`. Recompositions are frequent; expensive computations stall the UI.
- **Storing state in a `var` outside `remember`/`rememberSaveable`** — survives one recomposition, then resets.
- **`@OptIn` per function** when the global `optIn(...)` already covers it (see convention plugin's opt-ins).
- **Material3 colors in a feature module.** Use `AppTokens.colors.*`.
- **Mutable collections (`mutableListOf`) in state.** Use immutable.
- **Hardcoded `Color(...)`, `12.dp`, `14.sp`.** Use `AppTokens.*`.
