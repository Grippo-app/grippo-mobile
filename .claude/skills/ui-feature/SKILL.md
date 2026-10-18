---
name: ui-feature
description: Implement and review Grippo screens, dialogs, UI state and Decompose navigation using the seven-file MVI pattern and canonical analytics/preservation gates.
---

Build Grippo UI features using the current source and the topic references below.

Designing, redesigning or visually reviewing product UI MUST apply
[brand-design](../brand-design/SKILL.md) for intent, composition and brand acceptance,
with design-system for implementation. This skill retains feature/state/navigation
ownership. Compose source implementation/review applies
[compose-components](../compose-components/SKILL.md). Changes without visual impact
do not trigger the brand workflow.

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
avoid mechanical cancellation checks and new recovery wrappers. Keep work in the
existing `safeLaunch`/Flow pipeline and preserve cancellation propagation; explicit
checks require a real cancellation gap under that canonical rule.

Kotlin block style, the ban on named local functions and the startup-operation
pattern (one private class-level method called from `init` and `onError`) are
owned by [code style](../validation-gates/references/code-style.md#kotlin-blocks);
apply them as written there. Constructor order, `init`, handlers, `update` and
named arguments are owned by
[ViewModel layout](../validation-gates/references/code-style.md#viewmodel-layout);
apply it as written there.

Drafts, restoration flags and data-backed form synchronization are owned by
[state-preservation](../state-preservation/SKILL.md#data-backed-form-synchronization);
the single error path (`getOrThrow()` inside `safeLaunch`, no local catches, pure
`mapError`, `onError` after dismissal) is owned by the
[error pipeline](references/error-pipeline.md). Apply both as written there.

## State preservation

Every behavior change applies [state-preservation](../state-preservation/SKILL.md)
and its mandatory preservation gate; the Component-side draft pattern is in
[process-death.md](references/process-death.md#editable-form-drafts).

## Analytics impact

Every behavior change applies [feature-analytics](../feature-analytics/SKILL.md),
including its screen coverage gate; a visual-only change needs the one-sentence
impact assessment defined in [CLAUDE.md](../../../CLAUDE.md#definitions).

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
- **Accepted decisions from task-prep** constrain names and paths.

## Workflow

1. **Read the routed references** — look up the task kind in the
   [routing table](references/index.md#task-kind--topic--read) and read those
   files. If a referenced file is missing, report it in the reply and continue
   with the source.
2. **Within authorized module/navigation scope, scaffold if the feature module doesn't exist** — create the module,
   `settings.gradle.kts` include, `build.gradle.kts`, the `public` feature-root
   entry Component and the necessary RootRouter entry. For one screen the public
   Component owns its seven MVI files and screenPresentation directly; for multiple
   screens use a container root and child stack, following current naming.
3. **For a screen/dialog: write the seven MVI files** (same package, siblings):
   - `<Name>State` — `@Immutable`; meaningful constructor defaults or explicit
     required initial data; no `companion Empty` on State.
   - `<Name>Direction` — `sealed interface : BaseDirection`; nav intents only.
   - `<Name>Loader` — `@Immutable sealed interface : BaseLoader`; empty is ok.
   - `<Name>Contract` — `@Immutable interface` + `companion object Empty`.
   - `<Name>ViewModel` — `BaseViewModel<State,Direction,Loader>`, implements
     Contract; `safeLaunch` / `Flow.safeLaunch` / `withLoader` / `update {}` /
     `navigateTo` with errors in the shared pipeline — never
     `viewModelScope.launch`, `GlobalScope` or local catches.
   - `<Name>Component` — `BaseComponent<Direction>`;
     `retainedInstance { <Name>ViewModel(...) }` with dependencies resolved
     inside that factory; `eventListener` maps every Direction subtype.
   - `<Name>Screen` — `@Composable internal fun(state, loaders, contract)` in
     `BaseComposeScreen`, `AppTokens.*` only; new or visually changed UI gets an
     `@AppPreview` per significant state ([definitions](../../../CLAUDE.md#definitions))
     with `Contract.Empty`.

   Nonvisual changes assess visual impact and reuse existing previews without
   unrelated preview expansion. Shared model stubs live in ui-core:state; private
   screen-State fixture builders may live beside the feature preview.
   Choose extraction under [Compose structure](../compose-components/references/structure-and-runtime.md#read-from-top-to-bottom)
   and place extracted feature composables under [feature file layout](references/module-structure.md#file-layout-for-a-feature).
   Entity composables bind their `*State` as one parameter.
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
9. **Verify** — run the build checks the
   [verification policy](../../../CLAUDE.md#verification-policy) requires for the
   changed files. Apply the analytics skill's scoped checks when analytics changes.
   Manual runtime checks follow [runtime-verification](../runtime-verification/SKILL.md).
   Fix build failures caused by the task's changes. Report unrelated or pre-existing
   failures and every unrun check; do not expand scope or change unrelated
   infrastructure to make a build pass.

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

Feature mechanics are routed under `references/`. Shared Compose implementation
belongs to [compose-components](../compose-components/SKILL.md). Feature behavior
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
Inventory every changed ViewModel member and State constructor field against
[minimal runtime state](references/state.md#minimal-runtime-state); reject shadow
stores and fields whose value is already determined by the retained inputs.
Fix findings and review the resulting diff again. Run the checks the
[verification policy](../../../CLAUDE.md#verification-policy) names; report
evidence and unverified runtime scenarios.
