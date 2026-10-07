# design-system — references index

Self-contained reference pack for the `design-system` skill. These files carry the skill's
**own** normative rules — a builder reads the matching reference below.

## Routing table

| When the task involves… | Read |
|---|---|
| `AppTokens` facade, the six accessors, when to add a token/category, VM-side `StringProvider` equivalent, CompositionLocal definitions | `tokens.md` |
| `AppColor` shape, nested groups, color slots, dark/light impls, two-levels-deep guidance | `color.md` |
| `AppDp` shape, dimension/spacing/radius slots, base scales, no-theme-switch rationale | `dp.md` |
| `AppTypography` text styles, `@Composable` accessors, font family, adding a style token | `typography.md` |
| `AppString` / `AppDrawable` / `AppIcon` accessors, string/drawable/font conventions | `accessors.md` |
| `AppTheme` composable, `ProvideResources`, composition locals, theme + locale switching | `theme.md` |
| `@AppPreview`, `PreviewContainer`, stub data, preview rules | `previews.md` |
| Shared widgets (Button, Input, Chip, Toolbar, Card, EmptyState, BannerCard, LineIndicator, Toggle, selectable cards), rules for all components, component-vs-`compose-libs` decision | `components.md` |
| Compose Multiplatform resources, `composeResources/` layout, `androidResources.enable`, `strings.xml`/`plurals.xml`/drawables/fonts | `resources.md` |
| `:design-system:*` module layout (`components`, `core`, `preview`, `resources:provider`, `resources:provider-impl`), build files, rules summary | `design-system-modules.md` |
| `AsyncImage` token-themed usage (image loader) | `image-loader.md` |
| Add a string / plural / drawable / icon / font (step-by-step recipe + verify) | `cookbook-resource.md` |
| Kotlin style, visibility, `explicitApi()`, collections, flows | `kotlin-style.md` |
| Naming tables (classes, files, functions, parameters, sealed types, stub*) | `naming.md` |
| Package scheme, directory=package, area sub-packages | `packages.md` |

## Cross-skill references (not owned here)

These rules are owned by sibling skills; the design-system skill defers to them:

| Topic | Source |
|---|---|
| Compose function-signature rule, modifier-first, entity composables, Toggle outlier | `../../ui-feature/references/compose-rules.md` |
| Forbidden patterns (no literal colors/dp/TextStyle, no `MaterialTheme.*`, no `stringResource(R.…)`, no `ic_*` raster) | `../../validation-gates/references/forbidden-patterns.md` |
| Stop-and-ask items (new token category, slot rename, widget removal, new font, theme wiring) | `../../validation-gates/references/when-to-stop-and-ask.md` § Design system |

## Owned agents (frozen contracts)

| Agent | Kind | Contract |
|---|---|---|
| `design-system-component-builder` | builder | `orchestrator/contracts/agents/design-system-component-builder.md` |
| `resource-builder` | builder | `orchestrator/contracts/agents/resource-builder.md` |

## Validators

`compose-stability-validator`, `naming-convention-validator`, `anti-pattern-scanner`,
`architecture-validator`, `build-validator` — always. Figma gates are conditional:
`figma-spec-validator` / `figma-screenshot-validator` require at least one
non-`none` `## Design` bullet; `figma-component-coverage` applies to
design-system component/mapping-registry work; `figma-drift` applies only when
a published component-drift comparison is in scope (design token AND component
comparison are the server-run local comparators on the site's Design → Tokens /
Design → Components tabs, not validators).
