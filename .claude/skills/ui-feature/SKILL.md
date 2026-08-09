---
name: ui-feature
description: Build the product UI layer of the KMP app — add a screen, sub-screen, tab, bottom-sheet dialog, picker, or modal; wire Decompose navigation (ChildStack / SlotNavigation, cross-feature jumps, deeplinks); author the seven-file MVI contract (State / Direction / Loader / Contract / ViewModel / Component / Screen); add `:ui-core:state` UI models, `*FormatState`, `stub*()` previews, and the error-pipeline triad; handle process-death restoration. Triggers on "MVI", "Compose screen", "Decompose component", "BaseViewModel", "ViewModel", "UI state", "navigation", "dialog", "feature module".
---

Owns the UI-feature work: `screen-builder`, `dialog-builder`,
`feature-module-scaffold-builder`, `ui-core-state-builder`, and
`cross-feature-nav-builder`. This is the operational
entrypoint; long normative rules live in self-contained `references/` (routed below).

## When to use

- Add a new sub-screen inside an existing `:ui-screen-features:<feature>` module.
- Scaffold a brand-new feature module (root component + first screen).
- Add a bottom-sheet dialog / picker / modal under `:ui-dialog-features:<name>`.
- Add the `:ui-core:state` UI models (`*State` / `*RowState` / `*EnumState` /
  `*FormatState` + `stub*()`) a screen or mapper binds to, or the error triad.
- Wire a cross-feature navigation jump or a deeplink between features.

Not this skill: domain models / `*Feature` interfaces (data-layer), `toState()`
mappers (mappers), reusable widgets in `:design-system:components`
(design-system), Koin module wiring (di-modules).

## Required inputs

- **Task file path** (`orchestrator/tasks/todo/TASK_*.md`).
- **Target module + names** — feature module, PascalCase screen/dialog name,
  route payload type(s), output type (dialogs).
- **The `*Feature` / `*UseCase`** the ViewModel reads from — MUST already exist in
  `:data-features:feature-api`.
- **The `*State` model + `stub*()`** the Screen previews — MUST already exist in
  `:ui-core:state` (or run the `:ui-core:state` workflow first).
- **Implementation plan** (if present): its per-builder section is authoritative
  for file list + names. Plan wins on names/paths; this skill wins on methodology.

## Workflow

1. **Read the routing table** below for the task kind, then read those files.
   Verify each exists (`[ -f <path> ]`); if any is missing, emit
   `BLOCKED: required reading missing — <list>` and stop.
2. **Scaffold first if the feature module doesn't exist** — create the module,
   `settings.gradle.kts` include, `build.gradle.kts`, the `public` feature-root
   component (bare-name, or `<Feature>Root*` only when a sub-screen reuses the
   feature name), and the `RootRouter` entry. Then the screen work chains on.
3. **For a screen/dialog: write the seven MVI files** (same package, siblings) —
   `<Name>State` (`@Immutable`, defaults-in-ctor, no `companion Empty` on State),
   `<Name>Direction` (`sealed interface : BaseDirection`, nav intents only),
   `<Name>Loader` (`@Immutable sealed interface : BaseLoader`, empty ok),
   `<Name>Contract` (`@Immutable interface` + `companion object Empty`),
   `<Name>ViewModel` (`BaseViewModel<State,Direction,Loader>`, implements Contract;
   `safeLaunch` / `Flow.safeLaunch` / `withLoader` / `update {}` / `navigateTo`
   only — never `viewModelScope.launch`, `GlobalScope`, raw `try/catch`),
   `<Name>Component` (`BaseComponent<Direction>`, `retainedInstance { getKoin().get() }`,
   `eventListener` maps every Direction subtype), `<Name>Screen`
   (`@Composable internal fun(state, loaders, contract)` in `BaseComposeScreen`,
   `AppTokens.*` only, `@AppPreview` per significant state with `Contract.Empty`).
   Screen-specific sub-composables go in a sibling `components/` folder, one per
   file, entity composables bound to their `*State` as one param.
4. **For a dialog**: no toolbar, `ScreenBackground.Color(...background.dialog)`;
   add the `@Serializable DialogConfig.<Name>` subtype (callbacks `@Transient`)
   and the `DialogContentComponent.createChild` branch; callable via
   `DialogController.show(...)`.
5. **Wire navigation** — add the `@Serializable` Router subtype + `createChild`
   branch + inner `sealed class Child` entry (Decompose `ChildStack` for screens,
   `SlotNavigation` for dialogs). Cross-feature jumps go through
   `:ui-screen-features:screen-api` + constructor-threaded callbacks (the
   `RootDirection` → `RootContract` → `RootViewModel` → `RootComponent.eventListener`
   chain), never direct feature-to-feature imports. Deeplinks add the `Deeplink`
   enum entry + `RootViewModel.parseDeeplink` branch.
6. **Honor process-death** — all `*Router`/payloads/`DialogConfig` `@Serializable`,
   no lambdas in routes, state rebuilt in `init {}` via Flow re-subscription,
   range/filter state lives in the route, `key = "..."` on `childStack`.
7. **For `:ui-core:state` work** — add `*State`/`*RowState`/`*EnumState`/
   `*FormatState` + `stub*()`; for a new error type, edit the
   `AppError` → `AppErrorState` → `ErrorProviderImpl` when-branch triad atomically.
8. **Verify** — `./gradlew :androidApp:assembleDebug` (and the iOS XCFramework
   assemble when `iosEnabled: true`). Build failures are yours to fix.

## Stop and ask

- Required reading file missing → `BLOCKED: required reading missing — <list>`.
- A referenced `<X>Feature` / `<X>UseCase` doesn't exist in `:data-features:feature-api`
  → stop; needs data-layer first.
- A referenced `<X>State` / `stub<X>()` missing in `:ui-core:state`
  → `BLOCKED: <X>State / stub<X>() missing in :ui-core:state — needs the :ui-core:state workflow first`.
- Figma-enabled screen with a `## Design` bullet but a census component in any
  unresolved status (`MISSING`/`INCOMPLETE`/`AMBIGUOUS`/`UNSUPPORTED`/`RETIRED`/
  `SOURCE_STALE`) → do NOT write that screen; report `MISSING_COMPONENT` /
  `AMBIGUOUS_COMPONENT` (orchestrator HALTs — the component must be built or its
  mapping/inventory state resolved first).
- Figma-enabled screen with a `## Design` bullet → **build to the pulled design, not
  your own interpretation.** Read the pulled `orchestrator/.cache/figma/screens/<stem>/<Screen>.spec.json`
  (element inventory, layout, paddings/gaps, radii, fills/tokens, text styles) and the
  oracle `<Screen>.png` (appearance + which content/sections exist), and build to match;
  seed the `@AppPreview` stub + each ScreenshotTest state from the design's actual oracle
  content, per state — never invented data (`references/add-screen.md` "Build to the
  pulled design"; gate spec §2 content-parity / §2.1 multi-state). An invented layout or
  a stub whose content diverges from the oracle is the root cause of a failed screenshot
  comparison — a BLOCKER you fix by matching the design.
- Ambiguous UI shape (mirror class, dialog payload) the task doesn't specify —
  inventing a shape is out of scope; ask the orchestrator.

## References to read

This skill is **self-contained** — it carries its own rules under `references/`
and reads no external rule docs at runtime. Start with `references/index.md`
(the routing table); it maps each task-kind to the reference file(s) to read.

| task-kind | read |
|---|---|
| Add a sub-screen | `references/add-screen.md` → `references/mvi-contract.md`, `references/base-classes.md`, `references/compose-rules.md` |
| Author the seven-file MVI contract | `references/mvi-contract.md` → `references/base-classes.md`, `references/state.md` |
| Add a dialog | `references/dialogs.md` → `references/mvi-contract.md`, `references/base-classes.md` |
| Scaffold a feature module | `references/module-structure.md` → `references/navigation.md`, `references/mvi-contract.md` |
| Cross-feature nav / deeplink | `references/navigation.md` (§ Cross-feature navigation, § Deeplinks) → `references/module-structure.md` |
| Process-death / restoration | `references/process-death.md` |
| `:ui-core:state` models / error triad | `references/state.md` → `references/module-structure.md` (§ ui-core), `references/error-pipeline.md` |
| Compose / stability / previews | `references/compose-rules.md` → `references/base-classes.md` (§ BaseComposeScreen) |
| Base classes / results | `references/base-classes.md`, `references/results.md` |
| Cross-component results | `references/results.md` |
| Widgets shared across features | `references/module-structure.md` (§ `:compose-libs:*` — graduate to design-system instead) |

Always-on cross-cuts (MVI, dialogs, error pipeline, Decompose nav,
process-death) are summarized at the foot of `references/mvi-contract.md`. The
forbidden-patterns surface is owned by the validation-gates skill; each reference
file restates the patterns relevant to its topic under its own "Anti-patterns".

## Validators / gates

- `mvi-contract-validator` — seven files present, correct shapes/suffixes.
- `compose-stability-validator` — `@Immutable`/`@Stable`, immutable collections, no `var` in state.
- `architecture-validator` — base-class usage, no `viewModelScope`/`GlobalScope`/raw `try/catch`.
- `naming-convention-validator` — State/Direction/Loader/Contract/Component/Screen naming.
- `scope-leak-validator` — writes only files assigned in the plan section.
- `acceptance-tracer` — every acceptance bullet traces to a touched file.
- `build-validator` — `:androidApp:assembleDebug` (+ iOS XCFramework) green.
- Figma-enabled: `figma-spec-validator` + the Roborazzi screenshot-fidelity gate.

## Output contract

Return a builder report conforming to `orchestrator/contracts/builder-report.md`
(`agent`, `status` ∈ `done|blocked|failed|skipped`, `files_touched`,
`produced_signatures`, `blockers`, `assumptions`, `scope_deviations`, `handoff`).
When an implementation plan drives the task, its shape is
`orchestrator/contracts/planner-output.md` — the per-builder contract section
is the authoritative file list and naming table.
