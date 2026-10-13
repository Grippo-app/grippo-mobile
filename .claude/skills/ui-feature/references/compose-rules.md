# Compose-Specific Rules

> Note/Tag/AmountPicker/NoteDetail examples are educational, nonexistent APIs. They are not the current route, dependency or source catalog; inspect real callers and signatures before adapting them.

Compose conventions on top of Kotlin style (enforced by review + the Compose
compiler's stability metrics).

## Stability (MUST)

### State classes
Immutable State is annotated `@Immutable`. Constructor defaults represent meaningful
initial values, not fabricated required data; follow [State initialization](state.md#state-conventions):
- `@Immutable` promises deeply immutable observable data; all State fields remain `val`.
- Use `@Stable` only when equality, observable changes and property types satisfy its contract.
- An annotation does not make mutable data safe. Do not introduce `var` in State.

### Collections
Use `ImmutableList<T>` / `ImmutableSet<T>` / `PersistentList<T>` in state. Kotlin's `List`/`Set`/`Map` are **not Compose-stable** (degrade skipping). Construct via `buildList { ... }.toImmutableList()`.

### Sealed types
Use `@Immutable` on a sealed parent/subtype only when its complete contract is deeply immutable. Callback/composable-slot widget types use `@Stable` when its stability contract holds; MUST NOT mark them immutable merely because they are sealed. Immutable business State remains pure data with immutable fields.

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

### Secret input keyboard

Reuse InputPassword with `KeyboardType.Password`,
`KeyboardCapitalization.None` and `autoCorrectEnabled = false`, including while
the value is visible. Preserve its existing focus, visibility and IME callbacks
and default focus-clearing behavior; keyboard actions do not implicitly submit.
Keep callbacks neutral and do not add a separate input framework.

## Side effects

MUST route navigation through Direction/eventListener. LaunchedEffect is permitted
for actual presentation/system side effects; its keys must include the changing
inputs that determine that effect. Business state stays in the ViewModel.
Presentation/system registrations needing teardown MUST use DisposableEffect
with onDispose cleanup and keys matching the registration's changing inputs.

## Modifiers

MUST use existing component-scoped AppTokens dimensions/colors/typography. There
is no public general AppTokens.dp.radius group. Verify actual token accessors rather
than inventing a path. Modifier ordering must match the supplied design and widget
behavior; no generic order substitutes for rendered review.

## Lists — stable identity (MUST)
```kotlin
LazyColumn(contentPadding = PaddingValues(horizontal = AppTokens.dp.screen.horizontalPadding)) {
    items(items = state.notes, key = { it.id }, contentType = { "NoteRow" }) { note ->
        NoteRow(state = note, modifier = Modifier.animateItem(), onClick = { contract.onNoteClick(note.id) })
    }
}
```
- MUST use a stable unique item identity as the key; `.id` is appropriate only when that model actually owns a stable unique ID. Do not invent an ID or use mutable position as identity.
- Use `Modifier.animateItem()` when reorder/insert/delete transitions are part of the required presentation, not automatically on every list.
- **`contentType = { "..." }`** for performance (Compose reuses composables of the same content type).

## Recomposition discipline (SHOULD)

Use remember with relevant keys for expensive pure computations or identity that
must remain stable. Simple loader membership and Boolean rendering conditions may
remain inline; do not add mechanical caches around every expression.

## Material3 — actual product component boundaries (MUST)

Feature UI MUST reuse existing design-system components for their product roles,
including the existing Button, inputs, Toolbar and selection components. Inspect
actual signatures rather than assume generic Card/TextField wrappers exist.
When a demanded product component is missing, add it in the owning design-system
category only for that real need; do not generate wrappers for every imported symbol.

Text and Icon primitives may be used with explicit AppTokens styles/colors/tint;
existing tooltip primitives keep their actual role. Shared sheet hosts and
product-independent compose-libs retain their source-established boundaries.
Neutral callbacks MUST preserve the existing widget's focus/IME/selection behavior
and import no analytics types. Do not widen a module exception into a parallel
feature design system or bypass an existing appropriate product component.

## Anti-patterns (MUST avoid)

- `LaunchedEffect(Unit) { navigate(...) }` — navigation goes via `Direction` + `eventListener`.
- `mutableStateOf(...)` inside a `Composable` as a "global" — local state for UI; logical state belongs in the ViewModel.
- Calling `getKoin().get()` in a Composable — forbidden. Get deps via Component → ViewModel.
- `@Composable val` properties without `@ReadOnlyComposable` when the value doesn't depend on observable state.
- Recomputing nontrivial derived button state without relevant remember keys; a simple loader-membership read needs no separate cache.
- Heavy IO/CPU in rendering or on Main: keep it in the existing business/data owner with the appropriate dispatcher. LaunchedEffect/produceState alone do not select a worker dispatcher or make blocking work safe.
- Storing state in a `var` outside `remember`/`rememberSaveable`.
- `@OptIn` per function when the global `optIn(...)` already covers it.
- Material3 colors in a feature module — use `AppTokens.colors.*`.
- Mutable collections (`mutableListOf`) in state — use immutable.
- Product colors/dimensions/typography literals in consumers — use `AppTokens.*`. Neutral technical constants such as `0.dp` and `Color.Transparent`, and explicit representative preview fixtures, are permitted; they must not become alternative product styling.
- Flat-scalar explosion of a domain entity in a composable signature — pass the entity's `*State` model. Primitives (`Button`, `Chip`) are exempt.
