# ui-feature references — routing table

Reference pack for UI mechanics. Behavior changes also read the canonical
[feature-analytics skill](../../feature-analytics/SKILL.md); its rules are maintained
there rather than duplicated in each recipe.

## task-kind / topic → read

| task-kind / topic | read |
|---|---|
| New UI, visual redesign or brand/composition review | [brand-design](../../brand-design/SKILL.md), alongside the applicable UI mechanics below |
| Add a sub-screen (recipe) | `add-screen.md` → `mvi-contract.md`, `base-classes.md`, `compose-rules.md` |
| Author the seven-file MVI contract | `mvi-contract.md` → `base-classes.md`, `state.md` |
| Add a dialog / picker / modal (recipe + nav) | `dialogs.md` → `mvi-contract.md`, `base-classes.md` |
| Scaffold a feature module | `module-structure.md` → `navigation.md`, `mvi-contract.md` |
| Wire Decompose navigation (ChildStack, routers, animations, back, deeplinks) | `navigation.md` |
| Cross-feature navigation jump (recipe) | `navigation.md` (§ Cross-feature navigation) → `module-structure.md` (§ feature-to-feature anti-pattern) |
| Cross-component results (`ResultManager`, receiver-owned keys, SharedFlow semantics) | `results.md` |
| Process-death / restoration (7 hard rules) | `process-death.md` |
| Error pipeline (`safeLaunch` attempts, `resolve`, Retry/Close, ErrorProvider, dialog mapping, error triad) | `error-pipeline.md` |
| Data-side operation outcomes, cancellation, cache reconciliation | [operation-outcomes](../../data-layer/references/operation-outcomes.md) (data-layer owns it) |
| Kotlin style, naming, packages, ViewModel layout | [code-style](../../validation-gates/references/code-style.md) (validation-gates owns it) |
| `:ui-core:state` models / `UiText` / `*FormatState` / state conventions / `stub*` | `state.md` → `module-structure.md` (§ ui-core), `error-pipeline.md` (§ error triad) |
| Shared Compose structure, stability, effects, lazy identity and modifiers | [compose-components](../../compose-components/SKILL.md) |
| Feature Screen/MVI composition and keyboard integration | `compose-rules.md` → `base-classes.md` (§ BaseComposeScreen) |
| Base classes (`BaseViewModel`/`BaseComponent`/`BaseComposeScreen`/models/`OperationManager`/platform helpers) | `base-classes.md` |
| ViewModel data flow (observe vs get, `safeLaunch`) | `mvi-contract.md` (§ ViewModel rules) → `results.md` (§ Data boundary for result producers and receivers) |
| Analytics impact, typed VM events/operations, mandatory independent-screen hook and coverage | [feature-analytics](../../feature-analytics/SKILL.md) → [ViewModel integration](../../feature-analytics/references/viewmodel.md) |
| Widgets shared across features | `module-structure.md` (§ `:compose-libs:*`) — choose design-system for product coupling, compose-libs for product-independent reuse |

## reference files

| file | covers |
|---|---|
| `mvi-contract.md` | the seven files (State/Direction/Loader/Contract/ViewModel/Component/Screen), flow of data, what-goes-where, architecture cross-cuts |
| `navigation.md` | Decompose 3 layers, `*Router` sealed classes, `ChildStack`/`createChild`/`Child`, Direction→nav, **cross-feature nav recipe**, animations, deeplinks, back, anti-patterns |
| `dialogs.md` | `SlotNavigation`/`DialogConfig`/`DialogController`/`DialogProvider`, `DialogComponent`/`DialogContentComponent`, in-sheet nav, returning a result, dismissal, **add-dialog recipe** |
| `results.md` | receiver-owned companion keys, serialized resultKey, sendResult → back, SharedFlow semantics/lifecycle, Action protocols, verification, ViewModel data flow |
| `error-pipeline.md` | single error path, `ErrorProvider`/`ErrorProviderImpl` mapping, `BaseViewModel.resolve` and Retry/Close, authoring rules, the `AppError`→`AppErrorState`→`ErrorProviderImpl` triad |
| `process-death.md` | StateKeeper mechanism, 7 hard rules, `retainedInstance`, iOS specifics, anti-patterns |
| `base-classes.md` | `BaseViewModel`/`BaseComponent`/`BaseComposeScreen` API + rules, marker interfaces, `OperationManager`, platform helpers, current source ownership |
| `state.md` | `UiText`, `*FormatState`, state conventions, sealed-interface states, no flat scalar piles, `stub*` |
| `compose-rules.md` | feature Screen/MVI composition, placement, navigation effects, secret keyboards and product primitives |
| `module-structure.md` | `:ui-screen-features:*`/`:ui-dialog-features:*` shape + file layout, `:ui-core:*` modules, app shells, feature-to-feature anti-pattern, `:compose-libs:*` |
| `add-screen.md` | step-by-step recipe to add a sub-screen |

## always-on cross-cuts

Every task-kind: the architecture cross-cuts (MVI, dialogs, error pipeline,
Decompose nav, process-death) are summarized at the foot of `mvi-contract.md`.
Behavior changes also assess/update affected analytics coverage; visual-only edits
assess impact. Screen/component examples show MVI mechanics, not complete telemetry.
The forbidden-patterns surface (the validation-gates skill,
references/forbidden-patterns.md) is owned by that skill, not this one — each reference file restates
the patterns relevant to its topic under its own "Anti-patterns" heading.
