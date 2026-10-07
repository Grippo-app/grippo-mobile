# Promote — todo shape, acceptance anchors, defensive bullets

Self-contained reference for the promoted `todo/<stem>.md` shape (Step 5 promotion rules): the four
required sections, the `### Automated` / `### Manual` acceptance split, the per-bullet automation
anchor, the build gate, the conditional `## Design` / spec-gate bullets, and the five defensive
Out-of-scope bullets. The Figma census split (Step 5.5) and
the `## Preview states` + screenshot-gate bullets (Step 5.5a) live in
[`figma-split.md`](figma-split.md). The prep flow that calls into this is
[`prep-flow.md`](prep-flow.md).

`task-intake` reads the promoted file and enforces the same shape (see
[`blockers-and-dependencies.md`](blockers-and-dependencies.md) for the BLOCKED conditions on a malformed
`todo/` file).

---

## Standard todo shape

Mirror `orchestrator/tasks/README.md` "Todo shape":

```markdown
# TASK <N> — <title>

## Goal
One paragraph. What capability does the user gain when this lands?

## Inputs
- Data this depends on (existing `*Feature` interface, existing route, existing widget).
- Where the entry point lives (which screen launches it, which menu item, which deeplink).
- Any constraints not derivable from the skills' references (product-specific copy, design figure references, accessibility callouts).
- `Assumed — <fact>: <grounded default> (basis: <repo path / project convention / precedent>).` — one bullet per decidable gap prep closed itself instead of asking (Step 3 of [`prep-flow.md`](prep-flow.md)). Each is grounded and correctable via a Mode-A re-edit. Never record an assumption for an undecidable gap (product intent, irreversible migration/destructive/authorization surface, ambiguous owner, missing artifact) — those are asked, not assumed.

## Design
- Conditional — written only when `figmaEnabled: true` and the task creates/modifies UI nodes (`screen|dialog|component|overlay`): one `- <NodeName> [kind] — <figma node URL>` bullet per node, or the explicit escape `- <NodeName> — none (<why no mock>)`. Omit the whole section otherwise.
- The parser's `none` grammar is EXACT: `none`, or `none (<reason>)` with a non-empty reason containing no `)` and nothing after the closing paren (` — ` INSIDE the reason is fine; `none — text`, `none()`, `none (why) trailing` all fail closed as `DESIGN_VALUE_RESIDUE`). Bare `none` is parseable but not an audited opt-out; task-prep must ask for the reason. A pullable `<NodeName>` must stay inside `[A-Za-z0-9_]` (`RISKY_SCREEN_NAME` blocks at the cache gate otherwise), and no name may contain the bullet separator ` — ` (the parser splits on the FIRST one).
- Optional tighten-only gate bullet: the exact line `- gate: strict` forces strict pixel-verdict routing for this task even on an advisory-configured project. It is not a node bullet (no separator, no URL). Any OTHER `gate:` value is a malformed design (`DESIGN_VALUE_RESIDUE`, blocks at the cache gate) — the weakening direction deliberately has no grammar.

## Acceptance

### Automated
- Bulleted, automatically verifiable: by reading the diff, running a build task, a gate validator, or an automated test. Every bullet MUST carry at least one automation anchor (see promotion rules below).
- Automatable runtime behaviour ("tapping X opens Y", ordering, error paths) belongs HERE with a `test:` anchor — it is test-gated, certified by the deterministic executor's fresh receipts, and never hides in `### Manual` just because a diff cannot prove it.
- Includes the build gate: "`./gradlew :shared:assemble<IosFrameworkName>DebugXCFramework` and `./gradlew :androidApp:assembleDebug` both green."

### Manual
- Bulleted, requires genuinely human, hardware or external verification: subjective copy/design approval, canvas/glass pixel verdicts, physical push/camera/biometric flows without an automation harness, store/external-account operations, owner acceptance. Optional — omit the heading entirely when there is nothing in that class. A test PASS never auto-closes a manual bullet, and an automatable behaviour never moves here.

## Out of scope
- What the task does NOT cover. Cuts off scope creep. **Required** — write "nothing else" if the boundary is trivial, but the section must be present.

## Depends on (optional)
- TASK_<M>_<title> — must be in `orchestrator/tasks/done/` before this task runs.
```

---

## Promotion rules

- The four sections (`## Goal`, `## Inputs`, `## Acceptance`, `## Out of scope`) are **required**. A promotion that cannot fill any one of them is not actually ready — go back to Step 4 and ask. A section filled with a grounded, recorded `Assumed —` default (a *decidable* gap prep closed per Step 3 of [`prep-flow.md`](prep-flow.md)) counts as filled; a section that can only be filled by inventing *undecidable* product intent does not — ask.
- `## Acceptance` MUST contain a `### Automated` sub-heading with at least one bullet (the build gate is always there). `### Manual` is optional — write the heading and at least one bullet ONLY for genuinely human/hardware/external verification (subjective approval, physical-device-only flows, store/external accounts, owner acceptance). Runtime behaviour that a test can prove stays in `### Automated` with a `test:` anchor. Otherwise omit the `### Manual` heading entirely. Do NOT write `- none` and do NOT keep an empty `### Manual` heading.

### Automation anchors (the acceptance-anchor gate)

- **Every `### Automated` bullet must carry at least one automation anchor.** An anchor is one of:
  - a **file path** (`<path>`),
  - a **class/method/identifier** reference written `<Name>.<member>` or in backticks,
  - a **gradle task** (`./gradlew :...`),
  - a **test anchor** — `` `test:<kebab-id>` `` followed by ` — ` and the behaviour sentence.
- **Test anchors** (the test-gated class): lowercase kebab-case after `test:`
  (machine grammar: `test-policy.json` `anchorGrammar`); unique within the
  task; named after the BEHAVIOUR, never a class/file; written by task-prep and
  immutable afterwards (the planner maps them to concrete test cases but never
  renames one; the exact bullet bytes enter the evidence identity). One anchor
  may need several cases/lanes. Example:
  > `` - `test:save-note-failure-keeps-cache` — Failed save leaves the local cache unchanged and surfaces the mapped error. ``
  File/class/gradle anchors remain the structural/build classes — different
  first-class categories, never interchangeable aliases.
- The anchor lets `acceptance-tracer` map the bullet to a concrete artifact in the diff or to a certified test identity. If a bullet under `### Automated` lacks all four anchor forms, return:
  > `BLOCKED: acceptance bullet "<bullet, first 80 chars>" lacks an automation anchor (file/class/gradle task/test anchor). Give the behaviour a test: anchor, rewrite it with a structural anchor, or — only if it is genuinely human/hardware/external — move it to ### Manual.`
  Do NOT silently move it for the user — they make the call.

### Build gate

- The **build-gate acceptance line is required** and lives under `### Automated`. The iOS Gradle module is always `:shared:`; only the task-name suffix tracks `iosFrameworkName` from `orchestrator/project-config.md` (`:shared:assemble<IosFrameworkName>DebugXCFramework`); skip the iOS bullet when `iosEnabled: false`.

### Design bullets + spec-gate (figmaEnabled only)

- **Design bullets for UI tasks when `figmaEnabled: true`.** A UI task records one `## Design` bullet per UI node it creates or visually modifies (`screen|dialog|component|overlay`). Each node must carry either a Figma node URL or an explicit audited `none (<reason>)` supplied by the source/owner. A missing bullet, an empty value, or a reasonless no-mock claim goes back to Step 4; task-prep never fabricates `none (no mock exists)` from absence. An explicit `none` value is honoured by skipping that node in the census (Step 5.5) and the spec/screenshot gates, and the exemption is surfaced in the ship summary's `### Spec fidelity` block as a `SKIPPED — no mock` line (status-neutral — NOT a `### Caveats` bullet, so a no-mock node does not flip the outcome status). (The only hard stop in the design path is Step 5.5's cache pre-flight, and it fires only for nodes that DO carry a non-`none` URL.)
- **Generate the spec-gate acceptance bullet** — for every non-`none` `## Design` bullet, add one prep-generated line under `### Automated`, verbatim:
  > ``- Spec: <NodeName> matches its Figma node values (<url>) — `figma-spec-validator` green (Minors allowed).``
  This bullet is **spec-gated** (the planner and `acceptance-tracer` defer it to `figma-spec-validator`, like the build gate defers to `build-validator`). Skip when `figmaEnabled: false`.

### Out-of-scope defensive bullets

- The **five defensive Out-of-scope bullets below are required** in every promoted todo (unless the task explicitly authorises one of the gated surfaces). The first four are the anchor strings `scope-leak-validator` Step 3 literal-matches against; the TODO/FIXME marker bullet is owned by `anti-pattern-scanner` (to avoid duplicate routing). Write them verbatim:
  - `no new entries in gradle/libs.versions.toml`
  - `no changes to CLAUDE.md or orchestrator/**`
  - `no changes to build-logic/** convention plugins`
  - `no schema migration (Database.kt version, migrations/*) — separate task`
  - `no TODO/FIXME markers left in code the task claims as done`

  Drop a defensive bullet only when the task explicitly authorises that surface (e.g. a `room-migration-builder` task authored against `Database.kt` removes the schema-migration line).

### Number, dependencies, frozen-snapshot preservation

- **Preserve the task number** `<N>` from the backlog filename. Do not renumber.
- `## Depends on` is optional. If the backlog file (or user answers) already
  contains a `## Depends on` section, copy each `TASK_<M>_<title>` entry
  verbatim into the promoted todo. Add a new dependency only when the backlog
  or answers stated a real prerequisite. It may still be live during
  preparation; Run remains blocked until it is accepted in `done/` (see
  [`blockers-and-dependencies.md`](blockers-and-dependencies.md)).
- For server-created design-origin component tasks, preserve the `## Inputs` machine snapshot lines (`designComponentId:` / `figmaNodeId:` / `frozenStructuralHash:`) verbatim; the frozen binding evidence (`orchestrator/tasks/evidence/component-bindings/<sha32>.json`) is the task's contract — do not replace its frozen values with live design-inventory or cache reads.

---

## `task-intake` side — how the shape is enforced

A promoted `todo/` file is re-read by `task-intake` (see [`intake-classification.md`](intake-classification.md)),
which BLOCKs on:

- Any of the four required sections (`## Goal`, `## Inputs`, `## Acceptance`, `## Out of scope`) missing or empty — `## Out of scope` is required on the same footing as the others.
- `## Acceptance` without a non-empty `### Automated` subsection, with direct bullets outside the allowed subsections, or with an empty `### Manual` subsection.
- A `## Outcome` appendix present in `todo/` (re-opened from `done/` without stripping the trailer).

The `### Manual` bullets are recorded by `acceptance-tracer` as `manual` and feed the outcome appendix;
they are not gated automatically.
