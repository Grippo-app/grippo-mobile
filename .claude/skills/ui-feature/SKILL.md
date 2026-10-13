---
name: ui-feature
description: Implement and review Grippo screens, dialogs, UI state and Decompose navigation using the seven-file MVI pattern and canonical analytics/preservation gates.
---

Build Grippo UI features using the current source and the topic references below.

## When to use

- Add a new sub-screen inside an existing `:ui-screen-features:<feature>` module.
- Scaffold a feature module: expose one screen Component directly for a single page;
  use a container root and child stack for multiple screens.
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

Keep feature data in State/update. Private runtime flags, previous-source caches
and shadow snapshots for initialization/restoration/edit synchronization are
forbidden; do not relocate them into State, wrappers or mutable closures.
Fresh forms populate once and restored drafts keep their values; apply the
[canonical draft policy](../state-preservation/SKILL.md#data-backed-form-synchronization).

Follow the [shared error pipeline](references/error-pipeline.md): unwrap failures
with `getOrThrow()` inside `safeLaunch`. Do not hide, replace or present unhandled
failures with local catches, `runCatching` or result failure handlers, including
private helpers. Suspend helpers run in the caller's coroutine and propagate
failures without extra launches, recovery wrappers or stage flags. Pure `safeLaunch.mapError`
changes only dialog presentation; `onError` runs after dismissal. Results, recovery
and navigation stay business-owned, never in analytics or the mapper.
Keep both hooks consistent across block and Flow `safeLaunch`; pass callbacks
only where needed, following the shared error contract in the reference.

## State preservation

For new and changed features apply
[state-preservation](../state-preservation/SKILL.md). Its mandatory gate inventories
all new screens or affected state owners, preserves nonsecret editable drafts,
rebuilds data-backed state and verifies actual archive restoration. Empty container
state needs no supplier. The implementation pattern stays in
[process-death.md](references/process-death.md).

## Analytics impact

For behavior changes apply [feature-analytics](../feature-analytics/SKILL.md) and
update route/control/input/complete-operation coverage with actual omissions.
Visual-only edits need an impact assessment. Business owns requests/results/
validation/retry/navigation; analytics decorates complete operations.

The canonical analytics skill owns API shape, typed fact mapping, error isolation,
screen classification and foundation lifecycle binding. Components resolve the
stateless service only during retained VM construction; screen hooks and coverage
are mandatory, containers keep null. Do not duplicate those specifications here.

## Required inputs

- **Authorized task scope** and acceptance criteria from the user.
- **Target module + names** — feature module, PascalCase screen/dialog name,
  serializable route payload type(s) for screens; initial payload, `title: UiText`
  and typed output callbacks for an in-memory dialog session.
- **The `*Feature` / `*UseCase`** the ViewModel reads from — inspect the existing contract in
  `:data-features:feature-api`; implement a missing contract through data-layer when
  that work is within the authorized task, otherwise resolve the scope first.
- **Reusable UI models + `stub*()`** the Screen previews — reuse existing models in
  `:ui-core:state` or create the required model/stub through the workflow below. The owning screen's MVI State is
  created alongside its other six files, not in `:ui-core:state`.
- **Implementation plan** (if present): its agreed file list and names constrain the change. Plan wins on names/paths; this skill wins on methodology.

## Workflow

1. **Read the routing table** below for the task kind, then read those files.
   Verify each exists (`[ -f <path> ]`); if any is missing, emit
   `BLOCKED: required reading missing — <list>` and stop.
2. **Within authorized module/navigation scope, scaffold if the feature module doesn't exist** — create the module,
   `settings.gradle.kts` include, `build.gradle.kts`, the `public` feature-root
   entry Component and the necessary RootRouter entry. For one screen the public
   Component owns its seven MVI files and screenPresentation directly; for multiple
   screens use a container root and child stack, following current naming.
3. **For a screen/dialog: write the seven MVI files** (same package, siblings) —
   `<Name>State` (`@Immutable`, defaults-in-ctor, no `companion Empty` on State),
   `<Name>Direction` (`sealed interface : BaseDirection`, nav intents only),
   `<Name>Loader` (`@Immutable sealed interface : BaseLoader`, empty ok),
   `<Name>Contract` (`@Immutable interface` + `companion object Empty`),
   `<Name>ViewModel` (`BaseViewModel<State,Direction,Loader>`, implements Contract;
   `safeLaunch` / `Flow.safeLaunch` / `withLoader` / `update {}` / `navigateTo`
   with errors in the shared pipeline — never `viewModelScope.launch`, `GlobalScope`
   or local catches),
   `<Name>Component` (`BaseComponent<Direction>`, `retainedInstance { <Name>ViewModel(...) }`
   with dependencies resolved inside that factory,
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
6. **Apply the state-preservation gate** — all screen `*Router`/payloads `@Serializable`, no
   lambdas in routes, data-backed state rebuilt through existing subscriptions,
   route inputs seed ranges/filters; local StateKeeper drafts preserve the latest
   selection. Use `key = "..."` on `childStack`.
   Nonsecret editable drafts use a local Component StateKeeper supplier and
   serializable initialState; rebuild catalogs from Flows without erasing choices.
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
   assemble when the iOS platform check is authorized and available). Apply the analytics skill's scoped checks,
   DI/leaf-lifecycle checks and platform validation when analytics changes.
   Honor explicit platform/build restrictions and record unrun checks.
   Build failures in the permitted scope are yours to fix.

## Screen results and dialog-to-screen changes

For any screen that returns data, read [references/results.md](references/results.md)
before changing routes, Components or observers. The receiver defines its key in
its companion object and passes `resultKey` to the producer; the producer uses
`sendResult → back`. No Back in receiver observers, result relays or hidden
selection/navigation flags.

When converting a dialog to a screen, follow the seven-file MVI and visibility
patterns. Expose a single screen directly; use a container/subgraph only for an
actual multi-screen flow. Route through `screen-api`,
update all callers/dependencies/config entries, use screen toolbar/background/
spacing/insets from the design system, and preserve all existing modes and states.
Remove obsolete dialog names, resources and integration points within the migration
scope. Compare with actual neighboring features; do not invent a new construction
and present it as an existing project convention.

## Stop and ask

- Required reading file missing → `BLOCKED: required reading missing — <list>`.
- A referenced `<X>Feature` / `<X>UseCase` doesn't exist in `:data-features:feature-api`
  → apply data-layer first when authorized; ask only for unresolved contract or scope decisions.
- A referenced `<X>State` / `stub<X>()` missing in `:ui-core:state`
  → create the required model/stub within authorized UI-state scope before the screen.
- An unresolved product shape, navigation outcome or supplied design ambiguity:
  ask the user a simple quiz with a recommended choice. Inspect the actual Figma
  node or supplied artifact when design fidelity is required; do not invent its states.
- Module/root/public-route changes outside existing authorization require a scope
  decision. Necessary work already authorized by the user needs no repeated approval.

## References to read

The UI mechanics are **self-contained** under `references/`. Feature behavior
also requires the canonical [feature-analytics skill](../feature-analytics/SKILL.md).
Start with `references/index.md` (the routing table); it maps each task-kind to
the reference file(s) to read.

Use the canonical [reference routing table](references/index.md#task-kind--topic--read); do not maintain a second table here.

Always-on cross-cuts (MVI, dialogs, error pipeline, Decompose nav,
process-death) are summarized at the foot of `references/mvi-contract.md`. The
forbidden-patterns surface is owned by the validation-gates skill; each reference
file restates the patterns relevant to its topic under its own "Anti-patterns".

## Completion gate

Review the actual source/diff for the seven MVI files, correct visibility,
State/update ownership, immutable collections, neutral UI, typed navigation/results,
shared error handling and cancellation. Trace each acceptance criterion to its
implementation. Apply the mandatory analytics and preservation gates before completion.
Fix findings and review the resulting diff again. Run permitted static/manual checks
and builds within the user's scope; report evidence and unverified runtime scenarios.
MUST follow CLAUDE.md Verification policy: automated tests/infrastructure require
an explicit user request; verification-only production hooks remain forbidden.
