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

Follow the [project coroutine rules](../validation-gates/references/forbidden-patterns.md#coroutines):
do not add manual `ensureActive()` checks or wrappers in production. Keep work in
the existing `safeLaunch`/Flow pipeline and preserve cancellation propagation.

Follow the [Kotlin block style](../../../CLAUDE.md#kotlin): keep `safeLaunch`,
`trackOperation`, business/control bodies and multi-operation blocks multiline.
Short single-expression `update` and simple Flow transformations stay compact;
expand long expressions. No named local functions inside `init`, constructors,
other functions or lambdas; declare methods/functions at class/file scope.
Callbacks and DSL lambdas remain allowed. `init` normally starts requests and
subscriptions directly through `safeLaunch`/Flow. A complete startup operation
reused after error-dialog dismissal may be a private class-level method called
from `init` and `onError`; do not duplicate its sequence or use a local function.
Do not add `initialize`/`loadInitialData` helpers solely to hide orchestration.

Follow the [shared error pipeline](references/error-pipeline.md): unwrap failures
with `getOrThrow()` inside `safeLaunch`. Do not hide, replace or present unhandled
failures with local catches, `runCatching` or result failure handlers, including
private helpers. Suspend helpers run in the caller's coroutine and propagate
failures without extra launches, recovery wrappers or stage flags. Pure `safeLaunch.mapError`
changes only dialog presentation; `onError` runs after dismissal. Results, recovery
and navigation stay business-owned, never in analytics or the mapper.
Keep both hooks consistent across block and Flow `safeLaunch`; pass callbacks
only where needed, following the shared error contract in the reference.

## Analytics impact

For behavior changes apply [feature-analytics](../feature-analytics/SKILL.md) and
update route/control/input/complete-operation coverage with actual omissions.
Visual-only edits need an impact assessment. Business owns requests/results/
validation/retry/navigation; analytics decorates complete operations.

UI main depends on analytics-api. Inject one stateless service as `analytics` in
the Component's normal retainedInstance VM construction. Feature analytics stays
in VM callbacks/operations; every independent screen MUST override screenPresentation.
BaseComponent calls internal VM.bindScreen during doOnCreate; foundation alone owns
lifecycle binding/ScreenAnalytics/Initial→Restore. Graph hook stays null: no view
or lifecycle service resolution. Components/routes/factories/Render need no analytics
wiring; VM knows no Lifecycle/ComponentContext or activationFlow visibility observer.
Normal retained VM ownership stays; no feature provider/snapshot/retention helper.

Direct typed trackEvent facts use existing callbacks after busy guards. UI bounded
status mapping stays in analytics/, business-model conversions in data-mappers.
Text status compares old/new existing validation, without a cache; picker/click/
selection facts remain distinct. No raw values or analytics-owned validation.
Reporting Exceptions cannot block business/lifecycle; original errors/cancellation
remain unchanged. Follow the linked canonical skill for schemas/DI/verification.

## Required inputs

- **Task file path** (`orchestrator/tasks/todo/TASK_*.md`).
- **Target module + names** — feature module, PascalCase screen/dialog name,
  serializable route payload type(s) for screens; initial payload, `title: UiText`
  and typed output callbacks for an in-memory dialog session.
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
   with errors in the shared pipeline — never `viewModelScope.launch`, `GlobalScope`
   or local catches),
   `<Name>Component` (`BaseComponent<Direction>`, `retainedInstance { getKoin().get() }`,
   `eventListener` maps every Direction subtype), `<Name>Screen`
   (`@Composable internal fun(state, loaders, contract)` in `BaseComposeScreen`,
   `AppTokens.*` only, `@AppPreview` per significant state with `Contract.Empty`).
   Screen-specific sub-composables go in a sibling `components/` folder, one per
   file, entity composables bound to their `*State` as one param.
4. **For a dialog**: no toolbar, `ScreenBackground.Color(...background.dialog)`;
   add a plain `DialogConfig.<Name>` subtype with the required `title: UiText`,
   live typed callbacks, and its typed `hasSameContentAs` branch (compare content,
   exclude callbacks). Add the `DialogContentComponent.createChild` branch and
   `Child` entry. The host's step-bound navigation delivers results through
   `finish { router.onResult(value) }` and Back through `finish(null)`.
   Ordinary screen ViewModels open it via `DialogController.open(...)`; nested
   features emit semantic directions and forward typed constructor callbacks so
   the host chooses the config/title and uses the originating `Session`.
5. **Wire screen navigation** — add the `@Serializable` Router subtype +
   `createChild` branch + inner `sealed class Child` entry (Decompose `ChildStack`).
   Dialogs use the existing session slot and internal step stack; follow
   [dialogs.md](references/dialogs.md). Cross-feature jumps go through
   `:ui-screen-features:screen-api` + constructor-threaded callbacks (the
   `RootDirection` → `RootContract` → `RootViewModel` → `RootComponent.eventListener`
   chain), never direct feature-to-feature imports. Deeplinks add the `Deeplink`
   enum entry + `RootViewModel.parseDeeplink` branch.
6. **Honor process-death** — all screen `*Router`/payloads `@Serializable`, no
   lambdas in routes, state rebuilt in `init {}` via Flow re-subscription,
   range/filter state lives in the route, `key = "..."` on `childStack`.
   Dialog configs/callbacks remain in memory; the session slot and step stack use
   `serializer = null` and are not restored. Read
   [process-death.md](references/process-death.md).
7. **For `:ui-core:state` work** — add `*State`/`*RowState`/`*EnumState`/
   `*FormatState` + `stub*()`; for a new error type, edit the
   `AppError` → `AppErrorState` → `ErrorProviderImpl` when-branch triad atomically.
8. **Integrate affected analytics** — apply the linked analytics skill before
   completing behavior changes. Every independent screen MUST declare its VM
   screenPresentation; containers keep default null. Run the canonical
   [screen coverage gate](../feature-analytics/SKILL.md#mandatory-screen-coverage-gate)
   across all screens of a new feature or all affected screens before completion.
   Foundation binds the lifecycle. Decorate complete
   business blocks in the existing error pipeline and observe typed callbacks.
   Keep the coverage matrix synchronized; do not scatter event construction
   through Screens, Components, ViewModels and use cases.
9. **Verify within the user's scope** — `./gradlew :androidApp:assembleDebug` (and the iOS XCFramework
   assemble when `iosEnabled: true`). Apply the analytics skill's affected tests,
   DI/leaf-lifecycle checks and platform validation when analytics changes.
   Honor explicit no-build/test restrictions and record unrun checks.
   Build failures in the permitted scope are yours to fix.

## Screen results and dialog-to-screen changes

For any screen that returns data, read [references/results.md](references/results.md)
before changing routes, Components or observers. The receiver defines its key in
its companion object and passes `resultKey` to the producer; the producer uses
`sendResult → back`. No Back in receiver observers, result relays or hidden
selection/navigation flags.

When converting a dialog to a screen, follow the existing screen feature-root,
subgraph, seven-file MVI and visibility patterns. Route through `screen-api`,
update all callers/dependencies/config entries, use screen toolbar/background/
spacing/insets from the design system, and preserve all existing modes and states.
Remove obsolete dialog names, resources and integration points within the migration
scope. Compare with actual neighboring features; do not invent a new construction
and present it as an existing project convention.

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

The UI mechanics are **self-contained** under `references/`. Feature behavior
also requires the canonical [feature-analytics skill](../feature-analytics/SKILL.md).
Start with `references/index.md` (the routing table); it maps each task-kind to
the reference file(s) to read.

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
- `architecture-validator` — base-class usage, no `viewModelScope`/`GlobalScope` or local catches; review the shared error pipeline and cancellation.
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
