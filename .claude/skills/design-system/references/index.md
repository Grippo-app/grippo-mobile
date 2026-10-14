# design-system — references index

Read only the reference for the affected topic. Exact source APIs and signatures
remain in the linked producers; references own project invariants, not copied
implementation catalogs.

## Routing table

| When the task involves… | Read |
|---|---|
| `AppTokens` facade, the six accessors, when to add a token/category, VM-side `StringProvider` access, CompositionLocal definitions | `tokens.md` |
| `AppColor` shape, nested groups, color slots, dark/light implementations and slot ownership | `color.md` |
| `AppDp` shape, dimension/spacing/radius slots, private scales and adaptation ownership | `dp.md` |
| `AppTypography` text styles, `@Composable` accessors, font family, adding a style token | `typography.md` |
| `AppString` / `AppDrawable` / `AppIcon` accessors, string/drawable/font conventions | `accessors.md` |
| `AppTheme` composable, `ProvideResources`, composition locals, actual theme and locale behavior | `theme.md` |
| `@AppPreview`, `PreviewContainer`, stub data, preview rules | `previews.md` |
| Card hierarchy, semantic color, adaptive layouts, whole-screen visual review and capture cleanup | `visual-consistency.md` |
| Shared widgets (Button, Input, Chip, Toolbar, Card, IllustratedEmptyState, BannerCard, LineIndicator, Toggle, selectable cards), rules for all components, component-vs-`compose-libs` decision | `components.md` |
| Compose Multiplatform resources, `composeResources/` layout, `androidResources.enable`, `strings.xml`/`plurals.xml`/drawables/fonts | `resources.md` |
| `:design-system:*` module layout (`components`, `core`, `preview`, `resources:provider`, `resources:provider-impl`), build files, rules summary | `design-system-modules.md` |
| `AsyncImage` token-themed usage (image loader) | `image-loader.md` |
| Add a string / plural / drawable / icon / font (step-by-step recipe + verify) | `cookbook-resource.md` |
| UI/resource Kotlin rules and links to owning policies | `kotlin-style.md` |
| Project naming and file placement | `naming.md` |
| Existing packages and dotted-directory preservation | `packages.md` |

## Cross-skill references (not owned here)

These rules are owned by sibling skills; the design-system skill defers to them:

| Topic | Source |
|---|---|
| Requested product illustration creation, refinement or style review; canonical visual reference and asset acceptance | [illustrations](../../illustrations/SKILL.md) |
| Compose function-signature rule, modifier-first, entity composables, Toggle outlier | `../../ui-feature/references/compose-rules.md` |
| Forbidden patterns (no literal colors/dp/TextStyle, no `MaterialTheme.*`, no `stringResource(R.…)`, no `ic_*` raster) | `../../validation-gates/references/forbidden-patterns.md` |
| Stop-and-ask items (new token category, slot rename, widget removal, new font, theme wiring) | [scope questions](../../validation-gates/references/when-to-stop-and-ask.md) and [authorized scope](../../../../CLAUDE.md#scope-discipline) |

## Completion

Apply the semantic completion gate in [SKILL.md](../SKILL.md).
