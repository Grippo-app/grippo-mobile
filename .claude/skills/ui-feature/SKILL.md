---
name: ui-feature
description: Implement and review Grippo screens, dialogs, UI state and Decompose navigation using the seven-file MVI pattern and canonical analytics/preservation gates.
---

Build Grippo UI features using the current source and the topic references below.

Designing, redesigning or visually reviewing product UI MUST apply
[brand-design](../brand-design/SKILL.md) for intent,
[semantic recognition](../brand-design/references/design-language.md#semantic-recognition),
composition and brand acceptance,
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

Kotlin block style, the ban on named local functions and when an operation
becomes a shared class-level method are owned by
[code style](../validation-gates/references/code-style.md#kotlin-blocks);
apply them as written there. Constructor order, `init`, handlers, `update` and
named arguments are owned by
[ViewModel layout](../validation-gates/references/code-style.md#viewmodel-layout);
apply it as written there.

Drafts, restoration flags and data-backed form synchronization are owned by
[state-preservation](../state-preservation/SKILL.md#data-backed-form-synchronization);
the single error path is owned by the
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

1. Match the task to the [direct reference routing](#references-to-read) below
   and read the owners for the topics being changed. For a screen or dialog,
   apply the seven-file MVI contract. If a referenced file is missing, report it
   in the reply and continue with the actual source.
2. Implement within the authorized module/navigation scope, using the owning
   recipe and actual neighboring files. New module wiring follows the linked
   platform checklist; existing modules need only their affected integrations.
3. Apply the [state-preservation gate](../state-preservation/SKILL.md#mandatory-preservation-gate)
   and [analytics screen coverage gate](../feature-analytics/SKILL.md#mandatory-screen-coverage-gate)
   to behavior changes; visual-only changes use their canonical impact assessments.
4. Review the actual source against this skill's completion gate, then run the
   checks the [verification policy](../../../CLAUDE.md#verification-policy)
   requires for the changed files. Apply analytics' scoped checks when analytics
   changes. Manual runtime checks follow
   [runtime-verification](../runtime-verification/SKILL.md). Fix build failures
   caused by the task's changes. Report unrelated or pre-existing failures and
   every unrun check; do not expand scope or change unrelated infrastructure to
   make a build pass.

## Screen results and dialog-to-screen changes

For a screen that returns data, read [screen results](references/results.md)
before changing routes, Components or observers. For a dialog-to-screen migration,
follow [the conversion recipe](references/add-screen.md#convert-a-dialog-to-a-screen).

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

This is the canonical routing table. Read the linked owners for the changed
topics directly; a recipe does not replace its topic's contract. Shared cross-cut
prohibitions follow
[forbidden patterns](../validation-gates/references/forbidden-patterns.md).

| Task or topic | Read |
|---|---|
| New UI, visual redesign or brand/composition review | [brand-design](../brand-design/SKILL.md), alongside the applicable mechanics below |
| Add a sub-screen | [add-screen recipe](references/add-screen.md), [MVI contract](references/mvi-contract.md), [foundation usage](references/base-classes.md), [feature Compose](references/compose-rules.md) |
| Author a screen/dialog's seven MVI files or change ViewModel data flow | [MVI contract](references/mvi-contract.md), [base classes](references/base-classes.md), [State](references/state.md), [data boundary](references/results.md#data-boundary-for-result-producers-and-receivers) |
| Add a dialog / picker / modal | [dialog contract and recipe](references/dialogs.md), [MVI contract](references/mvi-contract.md), [base classes](references/base-classes.md) |
| Scaffold a feature module | [module structure](references/module-structure.md), [navigation](references/navigation.md), [MVI contract](references/mvi-contract.md), [module wiring checklist](../platform-build-toolkit/references/new-module.md) |
| Convert a dialog to a screen | [conversion recipe](references/add-screen.md#convert-a-dialog-to-a-screen), [MVI contract](references/mvi-contract.md), [screen results](references/results.md) |
| Decompose ChildStack, routers, animations, Back or deeplinks | [navigation](references/navigation.md) |
| Cross-feature jump or embedded foreign flow | [cross-feature navigation](references/navigation.md#cross-feature-navigation), [module boundaries](references/module-structure.md#anti-pattern-feature-to-feature-cross-import-must) |
| Cross-component results, receiver keys or result-bus lifecycle | [screen results](references/results.md) |
| Process death, route/shell restoration or Component draft wiring | [restoration](references/process-death.md), [state-preservation](../state-preservation/SKILL.md) |
| safeLaunch, recovery, Retry/Close, error dialogs or a new error type | [error pipeline](references/error-pipeline.md), including the [error triad](references/error-pipeline.md#the-error-triad-ui-corestate-work) |
| Data-side operation outcomes, cancellation or cache reconciliation | [operation outcomes](../data-layer/references/operation-outcomes.md) |
| Kotlin style, naming, packages or ViewModel layout | [code style](../validation-gates/references/code-style.md) |
| Reusable UI models, UiText, FormatState, State conventions or stubs | [State](references/state.md), [UI-core ownership](references/module-structure.md); new errors also read the [error triad](references/error-pipeline.md#the-error-triad-ui-corestate-work) |
| Shared Compose structure, stability, effects, lazy identity or modifiers | [compose-components](../compose-components/SKILL.md) |
| Feature Screen composition or keyboard integration | [feature Compose](references/compose-rules.md), [BaseComposeScreen](references/base-classes.md#basecomposescreen--screenbackground) |
| Foundation APIs, marker models, OperationManager or platform helpers | [base classes](references/base-classes.md) |
| Analytics impact, typed VM events/operations or screen coverage | [feature-analytics](../feature-analytics/SKILL.md), [VM integration](../feature-analytics/references/viewmodel.md) |
| Widgets shared across features | [shared widget ownership](references/module-structure.md#when-to-extract-a-widget-into-compose-libs) |

## Completion gate

Review the actual source/diff for the seven MVI files, correct visibility,
State/update ownership, immutable collections, neutral UI, typed navigation/results,
shared error handling and cancellation. Trace each acceptance criterion to its
implementation. Apply the mandatory analytics and preservation gates before completion.
Inventory every changed ViewModel member and State constructor field against
[minimal runtime state](references/state.md#minimal-runtime-state); reject shadow
stores and fields whose value is already determined by the retained inputs.
Review UI-model display accessors and their consumers against
[state-owned presentation mappings](references/state.md#state-owned-presentation-mappings).
Fix findings and review the resulting diff again. Run the checks the
[verification policy](../../../CLAUDE.md#verification-policy) names; report
evidence and unverified runtime scenarios.
