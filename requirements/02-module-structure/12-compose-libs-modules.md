# `:compose-libs:*` Modules

`:compose-libs:*` houses **reusable Compose widgets that don't belong in the design system**. The distinction:

- **`:design-system:components`** — atomic primitives (`Button`, `Toolbar`, `Input*`) styled by `AppTokens`. The product's visual identity.
- **`:compose-libs:*`** — opinionated, self-contained widgets (`chart`, `konfetti`, `wheel-picker`, `segment-control`) with their own internal state, animations, and gesture handling. Mini-libraries.

If you can imagine open-sourcing the widget, it belongs in `:compose-libs:*`. If it's product-specific and uses your color/typography tokens, it belongs in `:design-system:components`.

## Reference modules

| Module | Purpose |
|---|---|
| `:compose-libs:chart` | Generic charting (line, bar) with axis, gridlines, animation |
| `:compose-libs:konfetti` | Confetti animation for celebrations |
| `:compose-libs:wheel-picker` | iOS-style scrolling number/option wheel |
| `:compose-libs:segment-control` | Segmented control (iOS-style toggle) |

Add or remove modules per product.

## Rules

1. **No design-system imports.** A `:compose-libs:*` module receives style customization through Composable parameters (`Color`, `TextStyle`, `Dp`), not by reading `AppTokens`. This keeps the widget reusable across products.
2. **No data-layer access.** Widgets receive their input data as Composable parameters (`List<ChartPoint>`, `ImmutableList<WheelOption>`), never from a feature or repository.
3. **Stateless or self-contained state.** A widget owns its animation state and gesture state via `remember`/`rememberSaveable`. It does not depend on a ViewModel.
4. **Public API.** All types and Composables are `public`. These modules are designed to be consumed by every UI feature module.
5. **`@Stable` / `@Immutable` everywhere.** Widget input types must be marked for Compose stability inference.

## Build (typical)

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.compose.libs.<name>" }

    sourceSets.commonMain.dependencies {
        implementation(compose.foundation)
        implementation(compose.material3)        // only if the widget uses Material3 primitives
        implementation(libs.immutable.collections)
    }
}
```

No Koin. No `:toolkit:*`. No `:design-system:*`.

## When to extract a widget into `:compose-libs:*`

Extract when:

- The widget is used by **two or more** feature modules and is **not** a design-system primitive.
- The widget has significant internal state, animation, or gesture handling (≥ 100 LoC of logic).
- The widget is product-agnostic — replacing colors and typography would make it usable in another product without code changes.

Don't extract:

- One-off styled wrappers around Material3 primitives — those go in `:design-system:components`.
- Widgets tightly coupled to product domain (e.g. a `<Entity>HistoryChart` that knows about product-specific units and domain types) — those go in `:design-system:components` because they need `AppTokens` and product types.

## How widgets are styled by callers

A caller passes design tokens explicitly:

```kotlin
WheelPicker(
    options = state.options,
    selectedIndex = state.selectedIndex,
    onSelectedChange = contract::onWheelSelectedChange,
    textStyle = AppTokens.typography.h4(),
    color = AppTokens.colors.text.primary,
    rowHeight = AppTokens.dp.size.medium,
)
```

The widget's own implementation never references `AppTokens` — it takes everything as parameters.

## When a `:compose-libs:*` widget needs to depend on a design-system token

It doesn't. If you find yourself wanting to import `AppTokens` into a `:compose-libs:*` module, you've discovered the widget is **not** product-agnostic. Either:

1. Convert it into a `:design-system:components` widget (move the module).
2. Add a parameter to the widget's API for the value (`color: Color`, `style: TextStyle`).

Choose (2) unless the widget already has many product-specific assumptions baked in.
