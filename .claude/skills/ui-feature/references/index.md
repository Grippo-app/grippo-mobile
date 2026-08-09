# ui-feature references — routing table

Self-contained reference pack. A builder reading the file(s) below for its
task-kind gets the full normative rules — no external rule docs are read at
runtime.

## task-kind / topic → read

| task-kind / topic | read |
|---|---|
| Add a sub-screen (recipe) | `add-screen.md` → `mvi-contract.md`, `base-classes.md`, `compose-rules.md` |
| Author the seven-file MVI contract | `mvi-contract.md` → `base-classes.md`, `state.md` |
| Add a dialog / picker / modal (recipe + nav) | `dialogs.md` → `mvi-contract.md`, `base-classes.md` |
| Scaffold a feature module | `module-structure.md` → `navigation.md`, `mvi-contract.md` |
| Wire Decompose navigation (ChildStack, routers, animations, back, deeplinks) | `navigation.md` |
| Cross-feature navigation jump (recipe) | `navigation.md` (§ Cross-feature navigation) → `module-structure.md` (§ feature-to-feature anti-pattern) |
| Cross-component results (`ResultManager`, keys, channel semantics) | `results.md` |
| Process-death / restoration (7 hard rules) | `process-death.md` |
| Error pipeline (sendError, ErrorProvider, dialog mapping, error triad) | `error-pipeline.md` |
| `:ui-core:state` models / `UiText` / `*FormatState` / state conventions / `stub*` | `state.md` → `module-structure.md` (§ ui-core), `error-pipeline.md` (§ error triad) |
| Compose / stability / previews / lists / modifiers | `compose-rules.md` → `base-classes.md` (§ BaseComposeScreen) |
| Base classes (`BaseViewModel`/`BaseComponent`/`BaseComposeScreen`/models/`OperationManager`/platform helpers) | `base-classes.md` |
| ViewModel data flow (observe vs get, `safeLaunch`) | `results.md` (§ ViewModel data flow) |
| Widgets shared across features | `module-structure.md` (§ `:compose-libs:*`) — graduate to design-system instead |

## reference files

| file | covers |
|---|---|
| `mvi-contract.md` | the seven files (State/Direction/Loader/Contract/ViewModel/Component/Screen), flow of data, what-goes-where, architecture cross-cuts |
| `navigation.md` | Decompose 3 layers, `*Router` sealed classes, `ChildStack`/`createChild`/`Child`, Direction→nav, **cross-feature nav recipe**, animations, deeplinks, back, anti-patterns |
| `dialogs.md` | `SlotNavigation`/`DialogConfig`/`DialogController`/`DialogProvider`, `DialogComponent`/`DialogContentComponent`, in-sheet nav, returning a result, dismissal, **add-dialog recipe** |
| `results.md` | `ResultManager`/`ResultEmitter` API + drop-in source, channel semantics, Action types, subscribing/producing, multi-caller keying, ViewModel data flow |
| `error-pipeline.md` | single error path, `ErrorProvider`/`ErrorProviderImpl` mapping, `sendError`, authoring rules, the `AppError`→`AppErrorState`→`ErrorProviderImpl` triad |
| `process-death.md` | StateKeeper mechanism, 7 hard rules, `retainedInstance`, iOS specifics, anti-patterns |
| `base-classes.md` | `BaseViewModel`/`BaseComponent`/`BaseComposeScreen` API + rules, marker interfaces, `OperationManager`, platform helpers, **drop-in sources** |
| `state.md` | `UiText`, `*FormatState`, state conventions, sealed-interface states, no flat scalar piles, `stub*` |
| `compose-rules.md` | stability, function/screen signatures, entity composables, side effects, modifiers, lists, Material3 wrappers, anti-patterns |
| `module-structure.md` | `:ui-screen-features:*`/`:ui-dialog-features:*` shape + file layout, `:ui-core:*` modules, app shells, feature-to-feature anti-pattern, `:compose-libs:*` |
| `add-screen.md` | step-by-step recipe to add a sub-screen |

## always-on cross-cuts

Every task-kind: the architecture cross-cuts (MVI, dialogs, error pipeline,
Decompose nav, process-death) are summarized at the foot of `mvi-contract.md`.
The forbidden-patterns surface (the validation-gates skill,
references/forbidden-patterns.md) is owned by that skill, not this one — each reference file restates
the patterns relevant to its topic under its own "Anti-patterns" heading.
