# Intake — classification, prerequisites, builder order, validators

Self-contained reference for mapping a `todo/` task (or a backlog item under Step-1 classification)
to the change kinds it spans, the builders those kinds need, the order the builders must run in, and
the validator gate that closes the task. Builder ordering
is the frozen contract `orchestrator/contracts/builder-order.md` (cited inline); a wrong order
lands generated code in the wrong module or stalls a builder.

This is the canonical change-kind taxonomy. `task-prep` Step 1 reuses it verbatim — it is NOT
re-derived there.

---

## Required reading (verify each exists with `[ -f <path> ]` before relying on content)

1. `orchestrator/skills/_index/install-manifest.json` — the installed-skill (builder) roster.
2. the implementation skills (ui-feature/data-layer/design-system/mappers) recipe references — to recognize what each recipe covers.
3. `orchestrator/tasks/README.md` — the task-file shape.

If any are missing, stop and report `BLOCKED: required reading missing — <list>`. Do not proceed on
assumed content.

---

## 1. Classify the change

Map the task to one or more change kinds. A single task often spans several.

| Kind | Trigger phrases | Builder |
|---|---|---|
| Initial data-services scaffold | "Bootstrap data layer", "create initial `<Product>Api`", "scaffold backend + database modules", "set up `:data-services:backend` / `:data-services:database`" — and `<apiClassName>` does NOT yet exist | `data-service-scaffold-builder` |
| Brand-new top-level feature | "Add `:ui-screen-features:<X>`", "create new feature module `<X>`", "scaffold a feature for X", "new top-level feature `<X>`" — and the module `:ui-screen-features:<X>` does NOT yet exist | `feature-module-scaffold-builder` (then `screen-builder` for the first sub-screen) |
| New sub-screen inside an existing feature | "Add a screen", "Add a tab", "Add `<Feature><Sub>`", "Sub-screen of `<Feature>`" | `screen-builder` |
| New dialog (bottom sheet) | "Add a picker", "Add a bottom sheet", "Add a modal", "Add a `DialogConfig.<X>`", "Return a value from a popup" | `dialog-builder` |
| New domain capability + data feature | "Add `<X>Feature`", "Add notifications", "New domain entity that needs API + DB", "Repository pattern for X", "persist a user preference / setting", "remember the last-selected X", "DataStore-backed flag" | `data-feature-builder` (+ usually `endpoint-builder`, `room-migration-builder`, `mapper-builder`). When the Goal/Acceptance prose describes an operation that **combines two or more distinct domain capabilities** ("Login + profile bootstrap", "auth + token swap", "compose X and Y"), append `[cross-feature → UseCase likely]` to the `data-feature-builder` step in `## Builder sequence` — `implementation-planner` confirms the final decision against the actual Feature interfaces found by `context-finder`. A **persisted-preference** trigger routes to `data-feature-builder` over a DataStore key/accessor and does NOT trigger `room-migration-builder` / migration authorization — a DataStore preference is not a Room schema change. |
| New mapper file | "Add a mapper for X", "Translate DTO → Entity", "Domain → State" | `mapper-builder` (per direction) |
| New API endpoint + DTO | "Add endpoint `/x`", "Call `POST /y`", "DTO for Z response" | `endpoint-builder` |
| Schema change | "Add column to entity", "New table", "Rename column", "Bump database version" | `room-migration-builder` (REQUIRES explicit migration authorization) |
| New string / drawable / icon / font | "Add copy", "Add an illustration", "Add icon `<X>`", "New font weight" | `resource-builder` |
| Wire navigation between features | "From `<Feature A>` open `<Feature B>`", "Tap `<element>` → open `<Subscreen>`", "Cross-feature jump", "open `<Subscreen>` from a deeplink", "handle the `<scheme>://<host>` URI", "external link / push opens screen X" | `cross-feature-nav-builder` |
| Design-system widget rewrite or refinement | "figma", "design system", "component" — when the task names a `:design-system:components` widget plus a Figma component (a design node / frozen binding spec), OR asks to extend `AppColor` / `AppDp` / `AppTypography` slots inside an existing top-level group. The trigger word "component" refers exclusively to a `:design-system:components` Kotlin widget (Button, Input, Card, …); tasks about Decompose `Component` types (`<Feature>Component`, `<Subscreen>Component`) route to `screen-builder` / `dialog-builder` / `feature-module-scaffold-builder` instead. | `design-system-component-builder` |
| Toolkit utility — new or extended | "Add/extend `:toolkit:<x>`", "platform helper", "utility for dates / clipboard / connectivity / permissions / logging", "cross-cutting helper" — product-agnostic, not domain logic, not a data service, not UI | `toolkit-builder` (mode `extend` when `:toolkit:<x>` exists, `new` otherwise) |
| UI-core state / error mirror | "Add a `*State` / `*RowState` / `*EnumState` UI model to `:ui-core:state`", "expose `<Entity>` to the UI layer as state", "add a `stub<X>()` preview seed", "new `AppError.<X>` + its `AppErrorState` mirror + `ErrorProviderImpl` branch", "wire a new error case into the error pipeline" | `ui-core-state-builder` |
| App-shell platform wiring | "Add a permission to the manifest", "register an intent filter / `<scheme>://` URI in `:androidApp`", "wire a platform service in `:iosApp`", "edit `Info.plist` / `AndroidManifest.xml`", "add a launcher/intent entry to the shell" — shell config only, no business logic | `app-shell-builder` |
| Reusable product-agnostic Compose widget | "Add a `:compose-libs:<name>` widget", "reusable Compose component with no design tokens", "product-agnostic UI library piece (no AppTokens, no data layer)" — distinct from a `:design-system:components` widget, which is token-coupled and routes to `design-system-component-builder` | `compose-lib-builder` |

If the task matches no row, that's a strong signal it is under-specified — ask questions (see
[`prep-flow.md`](prep-flow.md)) rather than guess.

---

## 2. Resolve preconditions

For each builder picked, check whether the prerequisites exist. A missing prerequisite for one
builder might be supplied by another builder earlier in the chain — record the ordering.

- `data-service-scaffold-builder` requires only project-config. It refuses to run if `<apiClassName>` already exists (and refuses partial scaffolds — escalate).
- `feature-module-scaffold-builder` requires only the feature name. It refuses to run if `:ui-screen-features:<name>` already exists.
- `screen-builder` requires the target feature module + the `<X>Feature` interface it consumes. If the feature module does not yet exist, prepend `feature-module-scaffold-builder` to the chain — never expect `screen-builder` to create the module itself.
- `dialog-builder` requires no upstream dependencies (dialogs are leaves).
- `data-feature-builder` requires nothing upstream but signals downstream builders.
- `mapper-builder` requires the source and target types to already exist. For the `:data-mappers:domain-to-state` direction specifically, the target `*State` UI model must exist — if it does not, prepend `ui-core-state-builder` to the chain (it supplies the `*State` mirror) before the mapper runs.
- `endpoint-builder` requires `<apiClassName>` to exist in `:data-services:backend` (the class the new method is added to) **and** a contract source — in order of preference: the endpoint present in the generation-aware snapshot resolved by `npm run --silent contract:paths` in `orchestrator/api-contract/`, a backend contract URL the task cites, or explicit acknowledgement of contract status. None of the three → escalate per "Backend contract unverified" in [`blockers-and-dependencies.md`](blockers-and-dependencies.md). If `<apiClassName>` does not yet exist, prepend `data-service-scaffold-builder` to the chain.
- `room-migration-builder` requires `Database` to exist in `:data-services:database` and explicit user authorization in the task text. If `Database` does not yet exist, prepend `data-service-scaffold-builder` to the chain.
- `resource-builder` requires English source + every locale's translation (or explicit "translate later" acknowledgement).
- `cross-feature-nav-builder` requires the destination screen/route to already exist (or to be planned in the same task via `screen-builder`).
- `toolkit-builder` requires nothing upstream. In `new` mode it refuses if `:toolkit:<name>` already exists or an existing toolkit module already covers the capability (extend instead); a new third-party library requires explicit authorization in the task text.
- `ui-core-state-builder` requires only project-config. It SUPPLIES the `*State` mapper/screen targets the rest of the chain consumes — it does not depend on a mapper or screen existing first; downstream `mapper-builder` (for `:data-mappers:domain-to-state`) and `screen-builder` consume the `*State` it adds.
- `app-shell-builder` requires the `:androidApp`/`:iosApp` shells to already be bootstrapped. It refuses to move business logic or Koin modules into the shells — shell config only (permissions, intent filters, platform-service wiring); logic/Koin-module relocations escalate.
- `compose-lib-builder` requires only the `:compose-libs:<name>` target (creates it when absent). It refuses token-coupled or product-type widgets — anything that needs `AppTokens` / design-system tokens routes to `design-system-component-builder` instead.

---

## 3. Determine builder order

Order matters because later builders consume earlier ones' outputs. Canonical order, low layer →
high layer (frozen by `orchestrator/contracts/builder-order.md` §"Canonical order"):

```
1. toolkit-builder / compose-lib-builder (FIRST — bottom of the dependency graph)
2. data-service-scaffold-builder     (when <apiClassName> / Database is absent)
3. data-feature-builder              (creates the <X>Feature interface)
4. ui-core-state-builder             (adds the :ui-core:state *State mirrors + error triad the mapper/screen target)
5. mapper-builder × N                (one per direction needed, after source/target types exist)
6. feature-module-scaffold-builder   (creates an empty :ui-screen-features:<X> module)
7. screen-builder / dialog-builder   (after scaffold/state/mapper prerequisites)
8. cross-feature-nav-builder         (wires entry point on a different feature; registers any deeplink key)
9. resource-builder                  (any new strings/illustrations the screen needs)
10. app-shell-builder                (LAST — consumes :shared + the Deeplink key, so after cross-feature-nav-builder)
```

The orchestrator may interleave (mappers can be added in parallel with the screen if their inputs
exist). Mark builders that can run in parallel — they are independent.

The frozen contract states the dependency-graph order canonically as: `toolkit-builder` /
`compose-lib-builder` FIRST → `data-service-scaffold-builder` (when `<apiClassName>` / `Database`
absent) → `data-feature-builder` → `ui-core-state-builder` → `mapper-builder` →
`feature-module-scaffold-builder` → `screen-builder` / `dialog-builder` → `cross-feature-nav-builder`
→ `resource-builder` → `app-shell-builder` LAST.

### Hard-ordering rules

- **`feature-module-scaffold-builder` MUST run before `screen-builder`** when both apply. `screen-builder` refuses to create a brand-new feature module; without the scaffold first, it stalls. If the task introduces a brand-new `:ui-screen-features:<X>` AND a first sub-screen inside it, the plan ALWAYS contains the pair in that order.
- **`ui-core-state-builder` MUST run before `mapper-builder`** (for `:data-mappers:domain-to-state`) and before `screen-builder` when both apply — those builders consume the `*State` targets it supplies and stall if the `*State` does not pre-exist (same shape as the `feature-module-scaffold → screen` rule). If the task adds a new UI `*State` AND a domain→state mapper or a screen that renders it, the plan ALWAYS lists `ui-core-state-builder` ahead of them.
- **Toolkit first**: when `toolkit-builder` applies alongside other builders, it runs FIRST — toolkit sits at the bottom of the dependency graph, so its API must exist before data/UI builders consume it. `compose-lib-builder` runs in the same low-layer slot as toolkit (before `screen-builder`) — a `:compose-libs:<name>` widget must exist before a screen composes it.
- **App-shell last**: when `app-shell-builder` applies alongside other builders, it runs LAST — it consumes `:shared` and, for an external deeplink, the `Deeplink.<Entry>` key that `cross-feature-nav-builder` registers, so it follows the nav builder in the chain.

### Prerequisite-prepend rules (frozen)

- `screen-builder` needs the feature module + `<X>Feature` → prepend `feature-module-scaffold-builder` if missing.
- `mapper-builder` (`domain-to-state`) needs the `*State` → prepend `ui-core-state-builder` if missing.
- `endpoint-builder` / `room-migration-builder` need `<apiClassName>` / `Database` → prepend `data-service-scaffold-builder` if missing.
- `room-migration-builder` additionally requires explicit user authorization in the task text (see [`blockers-and-dependencies.md`](blockers-and-dependencies.md)).

Component tasks produced by the design-system-first split (`TASK_<M>_component_<widget>`, with an
`## Origin` lineage bullet) classify as the existing **design-system widget** kind above — no special
handling; their parent screen task is gated by the standard dependency check until they ship (see
[`figma-split.md`](figma-split.md)).

---

## 4. Pick the validators

Every task runs every applicable validator.

| Validator | When it applies |
|---|---|
| `architecture-validator` | Always (cheap; scoped to changed modules). |
| `mvi-contract-validator` | Any task touching `:ui-screen-features:*` or `:ui-dialog-features:*`. |
| `anti-pattern-scanner` | Always (broad gate). |
| `naming-convention-validator` | Always. |
| `di-validator` | Any task adding `@Single`/`@Factory`/`@Module` or touching `:shared/Koin.kt`. |
| `compose-stability-validator` | Any task touching `*State.kt` or `*Screen.kt`. |
| `data-layer-validator` | Any task touching `:data-services:**`, `:data-features:**`, or `:data-mappers:**`. |
| `build-validator` | Always (the final gate). |
| `scope-leak-validator` | Always — enforces the `Inputs`/`Acceptance`/`Out of scope` boundary on every task. |
| `acceptance-tracer` | Always — traces each `## Acceptance` → `### Automated` bullet to the diff (or, for a `test:` anchor bullet, to a certified test identity) and classifies it as `verified` / `partial` / `missing` / `conflicting` / `build-gated` / `resource-gated` / `spec-gated` / `screenshot-gated` / `test-gated`. A `test-gated` bullet turns `verified` only through a fresh PASS receipt in the sealed certification summary. Bullets under `### Manual` are recorded as `manual` and feed the outcome appendix; a test PASS never closes a manual bullet. Consumes the implementation plan produced earlier in the run loop when available. |
| `figma-component-coverage` | **Only when `figmaEnabled: true`** AND the task touches `:design-system:components` or the component mapping registry. Self-skips otherwise. |
| `figma-drift` | **Only when `figmaEnabled: true`** AND the task touches design-system components or tokens. Suggestion-only; self-skips otherwise. |
| `figma-spec-validator` | **Only when `figmaEnabled: true`** AND the task carries at least one non-`none` `## Design` bullet (screens cache pulled via the executable `figma:screens:<stem>` action — orchestrator Step 1b gates it). Compares the screen code's declared values against the cached value spec; Blocker/Major route, Minor is advisory. Self-skips otherwise. |
| `figma-screenshot-validator` | **Mandatory when `figmaEnabled: true`** AND the task has at least one non-`none` `## Design` bullet — Step 4.6b, post-assemble (no separate enable flag). Compares rendered screenshots against the pulled Figma oracle(s) per theme. The oracle + Roborazzi capture are REQUIRED inputs: a declared screen whose oracle/capture is missing is a BLOCKER, never a silent skip. Self-skips only for a non-UI task. |
| `backend-contract-drift` | **Only when `contract:paths` reports a valid snapshot and `backendContractEnabled` is not `false`** AND the task touches `:data-services:backend` DTOs / `<Product>Api` or `:data-mappers:*`. Suggestion-only; self-skips otherwise. |

In practice almost every non-trivial task runs every validator. Only `resource-builder`-only tasks
(strings/drawables) may skip `di-validator` and `data-layer-validator`. The four `figma-*` validators
run only on `figmaEnabled` projects (see the implement-figma skill; `figma-spec-validator`
additionally needs the task's `## Design` bullets, and `figma-screenshot-validator` is mandatory for
any non-`none` `## Design` bullet — the pulled oracle + Roborazzi capture are REQUIRED inputs
(missing ⇒ BLOCKER, never a skip), not run-conditions; it runs post-assemble at Step
4.6b). `backend-contract-drift` runs only when a
contract snapshot exists and `backendContractEnabled` is not `false` (see
`../../backend-contract-client/references/drift.md`).

The orchestrator also runs `inputs-resolver` as a **pre-flight** between intake and the rest of the
pipeline — it verifies every artifact named in `## Inputs` actually exists on disk. It is not in the
parallel validator group above; it runs earlier, in the task-orchestrator run-loop (Step 1a), **in parallel
with `context-finder`** (the two are independent — neither consumes the other's output). The
orchestrator surfaces its verdict as a BLOCKED escalation if any HIGH-severity claim is unresolvable.
You do not list it; the orchestrator invokes it unconditionally.

---

## Output contract (intake execution plan)

`task-intake` returns a single Markdown block; the orchestrator reads it and drives execution:

```markdown
# Execution plan — TASK_<N>_<title>

## Classification
- Kinds: <list>
- Total builders: <N>
- Total validators: <N>

## Builder routing (order)
1. `<builder>` — <one-line goal> (depends on: <prior builder or "none">)
2. `<builder>` — …
3. …

Parallel groups (optional):
- {`<builder>`, `<builder>`} — can run together because their outputs don't depend on each other.

## Validator set
Every task runs these unless noted:
- `architecture-validator`
- `mvi-contract-validator` (skip: <reason if applicable>)
- `anti-pattern-scanner`
- `naming-convention-validator`
- `di-validator`
- `compose-stability-validator`
- `data-layer-validator`
- `build-validator`
- `scope-leak-validator`
- `acceptance-tracer`
- `figma-component-coverage` (only if `figmaEnabled: true` AND design-system/mapping-registry touched)
- `figma-drift` (only if `figmaEnabled: true` AND design-system/tokens touched)
- `figma-spec-validator` (only if `figmaEnabled: true` AND the task has non-`none` `## Design` bullets)
- `figma-screenshot-validator` (mandatory when `figmaEnabled: true` AND the task has non-`none` `## Design` bullets — oracle+capture are required inputs, missing ⇒ BLOCKER; Step 4.6b, post-assemble)
- `backend-contract-drift` (only if a contract snapshot exists AND `backendContractEnabled` is not `false` AND backend DTOs / `<Product>Api` / mappers touched)

## Gate sequence
- Step 4 validators → Step 4.5 assemble → Step 4.6 verify (`auto|true|false`) → Step 4.6b screenshot (when `figmaEnabled` + non-`none` `## Design`) → Step 5 review → Step 5.5 security.

## Planner reference
- Derived from task-intake classification + builder-order + validation-run; blockers/escalations: <list, or "none">.
```

Return only this block.

## What intake MUST NOT do

- Do not write any code.
- Do not invoke any builder or validator — that's the orchestrator's job.
- Do not invent acceptance criteria the task didn't state.
- Do not classify a task as a refactor if it isn't — the project explicitly opts out of refactors-by-default.
- Do not auto-approve a Room migration. Migration authorization MUST come from the task text or be raised as a blocker.
