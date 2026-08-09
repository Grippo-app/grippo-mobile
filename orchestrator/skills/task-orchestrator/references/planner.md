# implementation-planner — file-level contract

Self-contained `implementation-planner` procedure. `task-intake`
picks **which** builders run; `context-finder` returns **what** already exists;
the planner decides **exactly** what each builder creates and what it is called.
Without this contract the builders drift on names and the chain breaks at the
validators. Read-only — produces a Markdown contract, never product code.

The frozen output shape (11 required headings, in order) is pinned in
[`orchestrator/contracts/planner-output.md`](../../../contracts/planner-output.md);
match it exactly.

## Authoritative reading (verify each exists; else `BLOCKED: required reading missing`)

1. `orchestrator/project-config.md` — `apiClassName`, `iosFrameworkName`,
   `featuresWithRootComponentSuffix`, package root, supported locales.
2. The cookbook recipe for the builder's file set — the owning skill's `references/` (`ui-feature/references/add-screen.md`, `data-layer/references/cookbook-data-feature.md`, `mappers/references/cookbook-add-mapper.md`, `design-system/references/cookbook-resource.md`; look up via `requirements-lookup.md`) — the recipes for each builder's standard file set. Read only
   the recipes for builders the plan listed.
3. Module placement rules — the owning skill's `references/module-structure.md` (`ui-feature/`, `data-layer/`, `platform-build-toolkit/`).
4. the design-system skill, references/naming.md + references/packages.md — naming + package-root rules.

## Inputs the orchestrator passes (3 required + 2 optional)

1. The task file content (verbatim — Goal / Inputs / Acceptance / Out of scope).
2. The task-intake plan — builder list + order + gating validators.
3. The context-finder excerpts — live paths, line numbers, signatures.
4. *(Optional)* inputs-resolver signature-drift bullets (MEDIUM, non-blocking;
   `none` when PASS) — surface present bullets as **Open assumptions**.
5. *(Optional)* the component census report
   (`.cache/figma/reports/census-<stem>.json`) — passed iff Step 1b ran.

If any of the **three required** inputs is missing: `BLOCKED:
implementation-planner missing input — <which>…`. Do not improvise.

## Step 1 — Derive canonical names

From `## Acceptance` + `## Goal`, derive every public symbol. Naming is fixed by
the design-system skill, references/naming.md: pick the **stem** from the task; derive the rest
mechanically. A screen `ProfileNoteArchive` always has the seven MVI files
(`…Screen.kt`, `…ViewModel.kt`, `…Contract.kt`, `…State.kt`, `…Direction.kt`,
`…Loader.kt`, `…Component.kt`) plus its route declaration. Emit a name table per
builder (`### Names (canonical) — owned by <builder>`).

Per-builder name-table rules (mirror the cookbook recipe):

- **data-feature-builder** — interface name, method signatures (param names +
  types from the task), impl class, Koin module. If a UseCase is required (per
  the builder's "UseCase decision"), add the UseCase class + entry-point
  signature; list `<Verb><Noun>UseCase.kt` under Files-to-create and
  `FeatureApiModule.kt` under Files-to-modify (add `single { <Verb><Noun>UseCase(…) }`).
- **endpoint-builder** — the new method name on `<apiClassName>` + the DTO names.
- **ui-core-state-builder** — UI-mirror names from the entity stem `<Name>`:
  `<Name>RowState`, `<Name>EnumState`, `<Name>FormatState`, preview seed
  `stub<Name>()`. On a new error case, also the error-pipeline triple
  `AppError.<X>` / `AppErrorState.<X>` / the `ErrorProviderImpl` `when`-branch.
- **app-shell-builder** — no new public Kotlin symbols; enumerate the concrete
  manifest/intent-filter/permission edits under Files-to-modify.
- **External deeplink** — the triad: `Deeplink.<Entry>` enum member (stable
  `key: String` via `Deeplink.fromKey`) + the `RootViewModel.parseDeeplink`
  `when`-branch → `RootDirection`. Owned by `cross-feature-nav-builder`
  (shell-side intent-filter by `app-shell-builder`).
- **compose-lib-builder** — the `:compose-libs:<name>` module + the public
  Composable signature (`public fun <Name>(modifier: Modifier = Modifier, …)`).
- **DataStore preference** — the preference KEY + its accessor (read `Flow` +
  suspend write). No Room migration involved.

## Step 2 — Enumerate files

Per builder list **Files to create** (full path, class/object, owning builder)
and **Files to modify** (full path, one-line change, owning builder). Use the
**live** paths from context-finder excerpts; do not invent paths — an
unresolved modified-file path goes under **Open assumptions**, not a guess.
Resolve `<package-root>` from project-config inline (no placeholder in output).

**Sub-composable placement.** Non-`*Screen` UI (headers, cards, rows, banners) is
created under `<sub-screen-package>/components/`, one cohesive composable per
file. Enumerate such a file in Files-to-create **only** when the task names it
concretely (an acceptance bullet, or a Figma census component the screen must
render); otherwise leave it to the builder's discretion. Path:
`.../<subscreen>/components/<Name>.kt`, never flat beside the seven MVI files.

## Step 3 — Public signatures

For every **new** public symbol, write the verbatim signature (builders consume
it as a hard contract — they do not invent arg names/types). Only specify
signatures for **new** public API; do not echo signatures for code the builders
only modify.

For endpoint/DTO/mapper work with a contract snapshot present
(resolve it with `cd orchestrator/api-contract && npm run --silent
contract:paths`), the per-builder contract MUST cite the exact resolved slice —
`<areasDir>/<area>.json`, the `schemaRef`, the field list with nullability — so
builders and `acceptance-tracer` share one source. Existence-check the
dynamically-named slice first; if missing: `BLOCKED: resolved contract area
slice <areasDir>/<area>.json missing though the endpoint is in the inventory…`.

For screen/dialog work with a census report present, component-mapping references
may name **only `MAPPED` fqNames**. A `MISSING`/`INCOMPLETE`/`AMBIGUOUS`
component at planning time is a census regression: `BLOCKED: census regression —
<component> is <status>…`. The census is mandatory for a figma screen task
(non-`none` `## Design` + `figmaEnabled`); if the orchestrator omitted it,
`BLOCKED: planner missing census report for a figma screen task…` — do NOT
silently skip the MAPPED-only check.

## Step 4 — Acceptance mapping

For each bullet under `## Acceptance` → `### Automated`, name the satisfying files
**and the owning builder**. This is the table `acceptance-tracer` reads —
unmatched bullets route directly to the `Owner builder` column (no 2-hop lookup).

Bullets under `### Manual` are NOT routed; list them in a separate `### Manual`
sub-table for the appendix. If the task has no manual bullets, omit the `###
Manual` sub-heading entirely (the absent heading is the no-op signal —
`acceptance-tracer` keys off `### Automated`).

Classify each `### Automated` bullet:

| Class | Owner builder column |
|---|---|
| **structural** (names a concrete artifact: route/class/file/method/DTO) | the builder from the plan that owns those files (must match Step 5) |
| **resource-gated** (a string/drawable in every locale) | `resource-builder` |
| **build-gated** (the build covers it) | `— (build-gated)` |
| **spec-gated** (the task-prep `- Spec: …` bullet; `figma-spec-validator`) | `— (spec-gated)` |
| **screenshot-gated** (the task-prep `- Screenshot: …` bullet; `figma-screenshot-validator`) | `— (screenshot-gated)` |
| **test-gated** (a `` `test:<kebab-id>` `` anchor bullet — automatable runtime behavior) | the builder that owns the subject behavior (must match Step 5) |

Testable runtime behaviour lives in `### Automated` as a **test-gated** bullet
with its `test:` anchor (task-prep writes the anchor; you never invent or
rename one). A runtime-behaviour bullet under `### Automated` WITHOUT a
`test:` anchor means the task-prep promotion drifted:
`BLOCKED: bullet "<first 80 chars>" reads as runtime behaviour but carries no
test anchor…`. Do NOT silently re-classify, and never move an automatable
behaviour to `### Manual` just because a diff cannot prove it.

**Consistency rule:** every builder in an `Owner builder` column for a structural
bullet MUST also appear in Step 5's `## Builder contracts`.

## Step 4b — Test contract

One canonical `## Test contract` block (frozen sub-heading order: `### Behavior
changes`, `### Tests to create or modify`, `### Regression suites`, `### Platform
lanes`, `### Test dependencies`, `### Test applicability`). For every
`test:<id>` anchor state: the behavior contract in one sentence; owner builder;
owner module; test layer; proposed test file; proposed test case identity;
happy/negative/boundary cases; required platform lane(s); direct suite;
affected consumer suites; needed test capability; the machine policy's minimum
evidence for that change kind; a one-line rationale. Suites, lanes, capabilities and reason
vocabulary come from the machine policy (`orchestrator/tasks/test-policy.json`,
carried by version/hash) — you may propose MORE than the policy minimum, never
less: the deterministic resolver widens any narrower proposal and records it in
`selectionReasons`. `### Test applicability` is either `- executable` or the
single typed `- test-not-applicable: <allowlisted value>` (only when the whole
task is genuinely non-executable; the resolver rejects a N/A that coexists with
behaviors).

## Step 5 — Builder contracts

End with a per-builder section (`#### <builder>`), each builder reads its own as
its scope: Files-to-create count, Files-to-modify list, "Must use these symbol
names exactly", "Public signatures fixed by the contract", and "Scope: ONLY the
files listed above". When two builders share a symbol, name the **producer** in
the contract and list it under the producer's "must use these names exactly"; the
consumer reads the same name.

## Step 6 — Open assumptions

End with the assumptions you couldn't resolve from the inputs alone (the
orchestrator surfaces these in the final summary). If none: `- none`.

## Step 6.5 — Behavioral edge-cases (reviewer-check items)

The acceptance mapping proves artifacts *exist*; it does not prove they *behave*
at the edges — and no validator does (the reviewer is the pipeline's only logic
gate). Infer the edge-cases the change must satisfy and list them as concrete
per-task check items the reviewer verifies. **Internal** — lives in the plan,
consumed by the reviewer; NOT acceptance, never a task-file bullet, never in `##
Acceptance mapping`, carries no file/class/gradle anchor.

Derive from `## Goal` + `## Acceptance` + the cookbook recipes + context-finder
excerpts. For each new/modified behavior, ask which generic category has a
*concrete* instance in THIS task; write only the instances that genuinely apply,
each tied to a named symbol/path and tagged with its category (never name a
product concept): **empty**, **null**, **boundary**, **error**, **ordering**,
**concurrency**, **wrong-condition**.

**Flag invented rules (spec-gap).** Whenever an edge-case exists because the task
did NOT state the governing rule (sort key/direction/tie-break, boundary
inclusivity, empty/error/loading behaviour, quantity, rounding/timezone,
duplicate handling) and you picked a default to keep the plan buildable, ALSO
record that choice under `## Open assumptions` tagged `spec-gap`, naming the
default (e.g. `- spec-gap: assumed newest-first sort; the task did not state an
order`). The edge-case bullet drives the reviewer's verification; the `spec-gap`
line records that the *rule itself* was your choice.

**Degrade-to-today.** No inferable edge-case (pure-rename, resource-only,
logic-free scaffold) → write the single line `- none`. Do not pad: better three
precise items than seven vague ones.

## Output format

A single Markdown block in this exact order (the frozen
`planner-output` shape):

```markdown
# Implementation plan — TASK_<N>_<title>
## Summary
## Names (canonical)
## Files to create
## Files to modify
## Public signatures
## Acceptance mapping
### Automated
### Manual          (optional — omit when no manual bullets)
## Test contract
### Behavior changes
### Tests to create or modify
### Regression suites
### Platform lanes
### Test dependencies
### Test applicability
## Behavioral edge-cases (reviewer-check items)
## Builder contracts
## Open assumptions
```

Return only this block. The orchestrator passes it verbatim to each builder and
to `acceptance-tracer`.

## What you MUST NOT do

- Do not write Kotlin/Swift/any product code. Builders write code; you write the
  contract.
- Do not invent file paths; resolve `<package-root>`/`<apiClassName>`/
  `<iosFrameworkName>` from `project-config.md` (no unresolved placeholders in
  a real plan).
- Do not re-classify the task — `task-intake` decided which builders run. If you
  disagree: `BLOCKED: implementation-planner disagrees with task-intake plan —
  <reason>`.
- Do not re-classify a `### Manual` bullet into `### Automated` (or vice-versa) to
  tidy the table — BLOCK instead.
- Do not include `Out of scope` bullets in the acceptance mapping
  (`scope-leak-validator`'s job).
- Do not promote a `## Behavioral edge-cases` item into `## Acceptance mapping` or
  suggest it as a task-file bullet — it has no anchor for `acceptance-tracer`.
- Do not extend the contract beyond what the task + cookbook recipes prescribe.
