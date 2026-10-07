# Compose-Specific Rules

Compose conventions on top of Kotlin style (enforced by review + the Compose
compiler's stability metrics).

## Stability (MUST)

### State classes
Every state class is `@Immutable` (fields use default ctor values so the VM builds initial state with `FooState()`):
- `@Immutable` tells Compose instances are deeply immutable — recomposition skips on identity.
- `@Stable` is weaker (equal-by-equals, not deeply immutable). Use when fields may change but `equals` reflects the change.
- **`var` fields require `@Stable`** (or forfeit skip optimization). Don't use `var` in state anyway.

### Collections
Use `ImmutableList<T>` / `ImmutableSet<T>` / `PersistentList<T>` in state. Kotlin's `List`/`Set`/`Map` are **not Compose-stable** (degrade skipping). Construct via `buildList { ... }.toImmutableList()`.

### Sealed types
`@Immutable` on the parent AND every subtype — inference doesn't auto-propagate.

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
- **`PascalCase`** function name; **`@Composable`** explicit.
- **`modifier: Modifier = Modifier`** is the **first** parameter (mirrors design-system `Button`/`Toolbar`/`BannerCard`/`BottomSheetToolbar`). The exception is `Toggle`, where `modifier` trails `onCheckedChange` — a known outlier, not a pattern to copy.
- **Required callbacks (`on<X>`)** come after the styling parameters, before any trailing defaults.
- **Defaults at the end**.
- **`public`** for design-system components, **`internal`** for feature screens/sub-screens.

### Entity composables — bind to the `*State` model (MUST)
A composable that renders a **domain entity** (row, card, header, section) takes that entity's `*State` UI model (declared in `:ui-core:state`, with its `stub*()` factory) as a **single** parameter — it does **not** re-expose the model's fields as a flat scalar list:
```kotlin
// ✅ one model in
@Composable internal fun NoteRow(modifier: Modifier = Modifier, state: NoteRowState, onClick: () -> Unit)
// ❌ flat-scalar explosion
@Composable internal fun NoteRow(modifier: Modifier = Modifier, id: String, title: String, summaryLabel: UiText, createdAtLabel: String, onClick: () -> Unit)
```
The model already exists and travels as one unit (mapper → state → preview via `stub*()` → list keys on `it.id`). Spreading forces every call site, preview, and `key` to re-assemble it; a new field becomes an N-call-site change instead of one model edit. Sub-states render through sub-composables the same way (`HeaderComposable(state = state.header)`).

**Exception — generic primitives.** Domain-agnostic widgets in `:design-system:components` / `:compose-libs:*` (`Button`, `Chip`, `Toolbar`, `BannerCard`) stay slot/enum/primitive-driven (sealed `*Style`/`*Content` + `@Composable` slots) so they're reusable across domains. A widget that takes **product types** binds to the `*State` model; a widget taking only Compose primitives stays primitive-driven. Form fields sit on the entity side via `*FormatState` (`InputEmail(value: EmailFormatState)`).

**Placement.** A feature's entity sub-composables live in the sub-screen's `components/` subfolder, one per file (`components/NoteRow.kt`); the seven MVI files stay at the package root. A widget reused across features graduates to `:design-system:components` / `:compose-libs:*`.

### Screen function signature (MUST)
Every `<Name>Screen.kt`: `@Composable internal fun <Name>Screen(component: <Name>Component, state: <Name>State, loaders: ImmutableSet<<Name>Loader>, contract: <Name>Contract)` when it renders a child stack/slot; otherwise omit `component` and keep `(state, loaders, contract)`. The non-component args stay in that order and keep those names.

### `@ReadOnlyComposable` (SHOULD)
For getters that only read CompositionLocals and never affect recomposition. `AppTokens` is a `@Stable object`; each token (`colors`/`typography`/`dp`/...) is a regular `public val` with `@Composable @ReadOnlyComposable get` (not an extension property) — skips recomposition-scope bookkeeping.

## Side effects

- **`LaunchedEffect`** — key on whatever should re-trigger it. `LaunchedEffect(Unit)` is **forbidden for navigation** (navigation goes via `Direction` + `eventListener`); it is **acceptable** only for one-time side effects depending on no input (e.g. collecting a `snapshotFlow` for the composable's lifetime). If it depends on any value — even an implicit one like the system locale — key on that value.
- **`DisposableEffect`** — for setup needing teardown (`onDispose { ... }`).
- **`produceState`** — for a value derived from a slow source.
- **`derivedStateOf`** — derived state depending on multiple inputs that should update only when the result changes (`val x by remember(a, b) { derivedStateOf { ... } }`).
- **Plain `remember(...)`** — for computed values that are pure functions of inputs (derived UI state), to avoid recomputing every recomposition.

## Modifiers

### Order matters (MUST)
Different orders produce different rendering — not a free choice. General order:
1. Size (`fillMax*`, `width`, `height`).
2. Outer padding / offset.
3. Background / border.
4. Shape (clip).
5. Interaction (clickable, focusable).
6. Inner padding.
7. Drawing modifiers (`drawBehind`, `graphicsLayer`).

### `Modifier.then(...)` (SHOULD)
For conditional modifiers: `.then(if (enabled) Modifier.clickable { ... } else Modifier)`. Don't build modifier lists via `mutableListOf`.

### Extension modifiers (SHOULD)
For repeated patterns, extract a `private fun Modifier.x(): Modifier`. Live in the same file (private) or `:design-system:components` if shared. Note: there is **no** general `AppTokens.dp.radius.*` slot (`AppDp.radius` is `private`) — read radii from the component-scoped group (`AppTokens.dp.bannerCard.radius`, `AppTokens.dp.wheelPicker.radius`, ...).

## Lists — always `key = { it.id }` (MUST)
```kotlin
LazyColumn(contentPadding = PaddingValues(horizontal = AppTokens.dp.screen.horizontalPadding)) {
    items(items = state.notes, key = { it.id }, contentType = { "NoteRow" }) { note ->
        NoteRow(state = note, modifier = Modifier.animateItem(), onClick = { contract.onNoteClick(note.id) })
    }
}
```
- **`key = { it.id }`** is mandatory (without it, animation breaks; identity lost).
- **`Modifier.animateItem()`** for reorder/insert/delete transitions.
- **`contentType = { "..." }`** for performance (Compose reuses composables of the same content type).

## Recomposition discipline (SHOULD)
Don't pass lambda parameters capturing changing state without `remember`. Use `key` parameters in `remember` to invalidate. Hoist state to the lowest common parent. Avoid re-creating `Modifier` chains inside loops without `remember`.

## Layout primitives (SHOULD)
`Column`/`Row`/`Box` (over `ConstraintLayout` unless its specific features are needed); `LazyColumn`/`LazyRow` for collections > 10 items; `Spacer(Modifier.height(...))` for vertical spacing between siblings (prefer over per-item `padding(top=, bottom=)`); `Arrangement.spacedBy(...)` for uniform spacing.

## Material3 — wrapped primitives (MUST)
Interactive/structural Material3 components (`Button`, `TextField`, `Card`, `Toolbar`, sheets, dialogs, chips, indicators) are wrapped in `:design-system:components`. **Outside** the design-system module, **do not import those wrapped primitives directly** — use the design-system wrapper (it applies `AppTokens` consistently):
```kotlin
// ❌ in a feature module
import androidx.compose.material3.Button
// ✅
import com.<org>.<product>.design.components.Button
Button(onClick = ..., content = ButtonContent.Text(text = "Click"))
```
**Exception — unwrapped Material3 primitives** imported directly in feature modules: `androidx.compose.material3.Text` (styles via `AppTokens.typography.*`, colors via `AppTokens.colors.*`), `androidx.compose.material3.Icon` (`tint` via `AppTokens.colors.*`), `rememberTooltipState` + tooltip primitives. Any other `material3.*` symbol → add a wrapper to `:design-system:components` first.

## Anti-patterns (MUST avoid)

- `LaunchedEffect(Unit) { navigate(...) }` — navigation goes via `Direction` + `eventListener`.
- `mutableStateOf(...)` inside a `Composable` as a "global" — local state for UI; logical state belongs in the ViewModel.
- Calling `getKoin().get()` in a Composable — forbidden. Get deps via Component → ViewModel.
- `@Composable val` properties without `@ReadOnlyComposable` when the value doesn't depend on observable state.
- `if (loaders.contains(MyLoader.Foo)) ... else ...` inline — wrap in `remember(loaders) { ... }`.
- Heavy work in a `Composable` without `LaunchedEffect`/`produceState`.
- Storing state in a `var` outside `remember`/`rememberSaveable`.
- `@OptIn` per function when the global `optIn(...)` already covers it.
- Material3 colors in a feature module — use `AppTokens.colors.*`.
- Mutable collections (`mutableListOf`) in state — use immutable.
- Hardcoded `Color(...)`, `12.dp`, `14.sp` — use `AppTokens.*`.
- Flat-scalar explosion of a domain entity in a composable signature — pass the entity's `*State` model. Primitives (`Button`, `Chip`) are exempt.
