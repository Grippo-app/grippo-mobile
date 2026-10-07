---
name: design-system
description: Build or extend the design system — tokens (AppColor / AppDp / AppTypography / AppIcon / AppString / AppDrawable via the AppTokens facade), theme (AppTheme + composition locals), shared components/widgets (Button, Input, Chip, Toolbar, Card, …), previews (@AppPreview / PreviewContainer), and resources (strings per locale, drawables, ImageVector icons, fonts). Use when a task names a :design-system:components widget, a token slot, the theme, a preview, or a resource (string / drawable / icon / font). Triggers — "design system", "tokens", "AppColor", "AppDp", "AppTypography", "AppTokens", "component", "widget", "theme", "AppTheme", "preview", "resource", "string", "drawable", "icon", "font", "typeface".
---

# Design system

Operational entrypoint for design-system work. The normative rules live in the
references under `## References to read`; this file routes you to the right one and
holds the invariants you must not violate — this is the *map*, not new rules.

## When to use

- A widget in `:design-system:components` is added/rewritten (Button, Input, Chip, Toolbar, Card, EmptyState, BannerCard, LineIndicator, Toggle, selectable cards, …).
- A token slot is added inside an existing top-level group (`AppColor.<widget>.*`, `AppDp.<widget>.*`, `AppTypography` accessor).
- A resource is added: string (every locale), drawable (`.webp` raster), icon (`ImageVector` code), font (`.ttf` + weight registration).
- A `@AppPreview` / `PreviewContainer` is authored alongside a screen or widget.

Not this skill: new Decompose `Component` types (→ ui-feature), new modules (→ platform-build-toolkit), DI wiring (→ di-modules), MVI/screen logic (→ ui-feature).

## Required inputs

- **Task file** — `orchestrator/tasks/todo/TASK_<N>_<title>.md`.
- **Resource kind** (resource work) — one of `string | plural | drawable | icon | font`, exact `snake_case` keys / `PascalCase` icon names, English text + every-locale translations, explicit positional placeholders (`%1$s`, `%2$d`).
- **Widget name** + **frozen component spec** (component work) — when `figmaEnabled: true`, for a design-origin component task the frozen binding evidence (`orchestrator/tasks/evidence/component-bindings/<sha32>.json`, referenced by the task's `## Inputs` machine snapshot: `designComponentId:` / `figmaNodeId:` / `frozenStructuralHash:`); the variant/property/slot axes to encode (read from the frozen spec — sparse declared variant tuples, never invented); the explicit `AppColor`/`AppDp`/`AppTypography` slot list; the single caller to migrate.
- Project config fields (`supportedLocales`, `productPackage`, `iosEnabled`, `typefaceFactory`) from `orchestrator/project-config.md`.

If a required input is missing, emit `BLOCKED: missing required input — {missing_input}` — design-system rewrites have no safe defaults.

## Workflow

- **AppTokens is the only access path.** Every visual value in a component reads `AppTokens.colors|dp|typography|icons|strings|drawables`. Raw `Color(0xFF…)` / `12.dp` / inline `TextStyle(...)` literals are allowed **only** inside concrete `AppColor` implementations and `AppDp` data objects — never in components or features.
- **Token slots:** add inside an existing widget group; every new `AppColor` slot gets a concrete value in **every** concrete `AppColors` impl on disk (dark/light specs). No new top-level `AppTokens.<category>` (stop-and-ask).
- **Dark/light specs.** Token producers ship literals for each theme impl; one impl (`DarkColor`) may be the only one wired today — extend whatever impls exist.
- **Component mapping.** Builders never write the Component Mapping Registry (`orchestrator/figma/component-mappings.json`) — every mutation is CAS-guarded. For a design-origin component task, finalization's `components` phase publishes the binding-authorized mapping after a staged validation compare; a generic task never auto-maps (it only passes the touch-scoped extraction regression gate). Reuse candidates come from the census's `codeCandidates` and are registered through the site's Mapping Review, not by the builder. If a consulted mapping projection changes mid-task, the final gate driver re-runs `component-census.mjs <stem>` under the pinned run id (run-loop Step 3 "Mapping-registry ordering").
- **One-caller cap.** A component rewrite migrates at most ONE canonical call site in `:ui-screen-features:*` per task; extra callers split into follow-up tasks. Hard cut — no `@Deprecated` aliases.
- **No inline `ImageVector.Builder` placeholders.** Icons are real path data (transcribed library / designer SVG / Material fallback), never empty `ImageVector.Builder {}` stubs. figma-export JSON is a name/size manifest, not path source.
- **No PNG for vector candidates.** Monochrome UI icons are `ImageVector` code on `AppIcon`; `drawable/` is reserved for raster `.webp` product imagery. No `ic_*` / `img_*` / `bg_*` prefixes.
- **Resources per locale.** Every string/plural key exists in every `supportedLocales` file with matching positional placeholders. Font weights ship the `.ttf` AND register in `<typeface>()`.
- **Previews.** Every component has ≥1 `@AppPreview` covering the default state, inside `PreviewContainer`, using explicit stub inputs, `private`. Screen/dialog previews use their own `Contract.Empty`; design-system components do not have MVI contracts.
- **Screenshot capture (figmaEnabled component with a `## Design` bullet).** The `@AppPreview` serves the IDE only — it is NOT the gate's capture. A component built from a Figma node MUST also get a Roborazzi capture `@Test`, exactly like a screen (`../ui-feature/references/add-screen.md`), just wrapped in `PreviewContainerComponent` (sized to the bullet's `frameSizeDp`) instead of `PreviewContainerScreenshot`: one `@Test` per `## Design` bullet/state, `captureRoboImage(".../<Component>Screenshot[.dark].png")`, seeded from the oracle's ACTUAL content, in the component module's `androidHostTest` under `id("screenshot.test.convention")`. Canonical code + theme rule live in `../validation-gates/references/screenshot-fidelity-gate.md` §2/§2.1/§2.2 — the single source of truth; do not fork it here or in `references/previews.md`. **A missing capture is a `MISSING_CAPTURE` BLOCKER, not a reason to reclassify:** stripping the `## Design` bullet to ship a node-backed component uncompared is an erosion the `ship-done`/`verify-done` UI-by-evidence backstop blocks (a cited node URL or a `designComponentId:` machine snapshot ⇒ a pullable bullet is mandatory, and a newly created widget file demands one structurally). Author the capture — never de-classify to skip it.

## Stop and ask

- New top-level token category (`AppTokens.motion`, `AppTokens.elevation`, …).
- Rename of a token slot consumed by multiple call sites.
- Removal of a widget still consumed elsewhere.
- New font / typeface (route under an explicit font-addition task).
- Edits to `AppTheme.kt`, composition-local wiring, `AppTokens.kt` aggregator, or any `:design-system:core/build.gradle.kts`.
- More than one caller to migrate, or `@Deprecated` aliases for a hard-cut rename.

(Authoritative list: `../validation-gates/references/when-to-stop-and-ask.md` § Design system.)

## References to read

Routing — read the reference that matches the task before writing code. The
references are self-contained — the skill carries its own normative rules and reads no
external rule docs at runtime; full routing table: `references/index.md`.

- Routing index — `references/index.md`
- Tokens facade — `references/tokens.md`
- Color — `references/color.md` · Dp — `references/dp.md` · Typography — `references/typography.md`
- Strings/drawables/icons — `references/accessors.md` · Theme — `references/theme.md`
- Preview — `references/previews.md` · Shared components — `references/components.md` · Resources — `references/resources.md`
- Module layout — `references/design-system-modules.md`
- Conventions — `references/kotlin-style.md`, `references/naming.md`, `references/packages.md`
- Cookbook — `references/cookbook-resource.md` · Image loader — `references/image-loader.md`

Cross-skill (not owned by this skill): Compose-rules
`../ui-feature/references/compose-rules.md`; Anti-patterns
`../validation-gates/references/forbidden-patterns.md`, `../validation-gates/references/when-to-stop-and-ask.md`.

## Validators / gates

- `compose-stability-validator` — `@Stable`/`@Immutable` annotations; types carrying lambdas/`@Composable` slots must be `@Stable`, never `@Immutable`.
- `naming-convention-validator` — widget names are nouns; resource keys `snake_case`; icons `PascalCase`.
- `anti-pattern-scanner` — no `Color(0xFF…)`/`12.dp`/inline `TextStyle`/`MaterialTheme.*` in components; no `stringResource(R.…)`; no `ic_*` raster prefixes.
- `architecture-validator` — token reads stay in components, literals stay in impls; module boundaries.
- `build-validator` — `:design-system:resources:provider:assemble` + `:androidApp:assembleDebug` (+ XCFramework when `iosEnabled: true`) green; exhaustive `when` chains, zero deprecation warnings on changed files.
- Figma gates are conditional, not blanket: `figma-spec-validator` and
  `figma-screenshot-validator` run only for tasks with at least one non-`none`
  `## Design` bullet; `figma-component-coverage` runs for design-system
  component/mapping-registry work; `figma-drift` runs only when a published
  component-drift comparison is part of the task and is suggestion-only
  (design token AND component comparison are not validators: they are the
  server-run local comparators surfaced on the site's Design → Tokens /
  Design → Components tabs).

## Output contract

Builders return `orchestrator/contracts/builder-report.md`. When an
implementation plan is in play, it follows `orchestrator/contracts/planner-output.md`
(per-builder file list + naming table wins on target conflicts; this skill's
methodology wins on pattern conflicts). Frozen per-agent surfaces:
`orchestrator/contracts/agents/design-system-component-builder.md`,
`orchestrator/contracts/agents/resource-builder.md`.
