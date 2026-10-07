# Blockers, dependencies & escalations

Self-contained reference for every BLOCKED / ESCALATE condition across the prep + intake mechanics:
the dependency execution gate (`## Depends on` must be in `done/` before Run), the cross-column collision gate, the
migration-authorization / backend-contract / refactor escalations, the convergence-stall ESCALATE, and
the BLOCKED-recovery loop. Classification lives in
[`intake-classification.md`](intake-classification.md); the prep flow in [`prep-flow.md`](prep-flow.md);
the promote shape in [`acceptance-anchors.md`](acceptance-anchors.md); the Figma cache pre-flight BLOCK
in [`figma-split.md`](figma-split.md).

---

## Dependency gate (`task-prep` warning → `task-intake` Run blocker)

Extract `## Depends on` if present (optional section). During preparation, preserve
each dependency verbatim and inspect where it currently lives. A dependency may
still be in backlog, pending, or todo: that is a warning, not a reason to stop
classification or promotion. The promoted task remains non-runnable until every
dependency is accepted in `orchestrator/tasks/done/`:

```bash
[ -f "orchestrator/tasks/done/<TASK_N_title>.md" ]
```

`task-intake`/canonical Run admission returns:
**`BLOCKED: depends on incomplete tasks: <comma-separated list>`** when execution
is attempted too early. Preparation itself continues and publishes the todo shape.

Two further cheap `[ -f ]` checks in the same loop:

- **In-flight dependency** — a dependency in `todo/`, `pending/`, or `backlog/`
  is valid during preparation. Preserve it and let Run admission wait. A
  done-copy plus a live copy is still an inconsistent board and remains an
  integrity blocker.
- **Self-dependency** — if a `## Depends on` entry equals THIS task's own stem, BLOCK: **`BLOCKED: task depends on itself (<stem>) — remove the self-dependency from ## Depends on.`** A task can never gate itself; this would otherwise dead-lock on the `done/` check (it is in `todo/`, never `done/`).

**Test-foundation prerequisite child.** When the Step-4.7 doctor returns
`ABSENT_CAN_INSTALL` (see `prep-flow.md`), the bootstrap coordinator yields
exactly one globally deduplicated child task (Source
`follow-up/test-foundation-prerequisite`, Ref = this parent). The parent's
final todo proposal carries that child stem in `## Depends on` BEFORE its own
promotion; Run admission stays fail-closed until the child is accepted in
`done/`, after which the parent simply becomes admissible and starts a fresh
Step 0 on current bytes — there is no `todo → prep/backlog` back-transition
and no hidden re-prep. A feature task that somehow reaches Step 0 with an
absent foundation gets `BLOCKED: foundation-prerequisite-missing` — Step 0
never edits the todo, never creates the child and never bypasses task-prep.

On the `task-prep` side, promote with the unresolved dependency intact. Do not
remove a real dependency merely to make the task runnable. Dependency cycles
remain a canonical integrity blocker.

Component tasks produced by the design-system-first split (`TASK_<M>_component_<widget>`) classify as
the design-system widget kind; their parent screen task is gated by this standard dependency check until
they ship (see [`figma-split.md`](figma-split.md)).

---

## Task-file state BLOCKs (`task-intake`, on `todo/` files)

- **Not in `todo/`** — if the file is not in `todo/` but exists in `backlog/` or `pending/`, return: **`BLOCKED: task is not in todo/ yet — run task-prep first to promote it.`**
- **Outcome appendix in `todo/`** — this is invalid lifecycle state, not permission to edit history. Return: **`BLOCKED: outcome appendix present in todo/ — preserve the file and reconcile the canonical reopen/finalization transaction; never strip Outcome by hand.`** If the task is still in `done/`, use `transition-task-state.mjs reopen`; if it is already malformed in `todo/`, surface the exact integrity finding for explicit repair.
- **Missing required section** — if any of `## Goal`, `## Inputs`, `## Acceptance`, `## Out of scope` is missing or empty, return: **"BLOCKED: task file incomplete — missing required section(s): X, Y. Cannot plan execution."** `## Out of scope` is required on the same footing as the others.
- **Acceptance shape** — `## Acceptance` MUST contain a non-empty `### Automated` subsection and MAY contain a non-empty `### Manual` subsection. Direct bullets under `## Acceptance`, an empty subsection, or any other shape are invalid: return `BLOCKED: task acceptance must use ### Automated and optional ### Manual subsections — run task-prep to publish the canonical shape.`

---

## Cross-column collision gate (`task-prep` — canonical precondition, Step 5)

A stem must exist in exactly ONE logical column at any time. Before composing a
todo proposal, run the canonical validator with the expected source state and
fresh INDEX; before publication, `transition-task-state.mjs promote` repeats
that check under the exact source-revision fence:

- **Stem also in `todo/`** — **`BLOCKED: canonical task-state collision for <stem> includes todo/<stem>.md. No mutation was made. Inspect the validator's exact paths and reconcile which artifact is authoritative before retrying; do not delete either copy from task-prep.`**
- **Stem also in `done/`** — **`BLOCKED: canonical task-state collision for <stem> includes done/<stem>.md. No mutation was made. If the shipped task is authoritative use the explicit reopen operation; otherwise reconcile the exact validator findings before retrying.`**

See [`prep-flow.md`](prep-flow.md) Step 5 for the validator, revision fence,
transaction helper, and rollback contract.

---

## Input/format BLOCKs (`task-prep`)

- **Backlog missing the `# TASK <N> — <title>` first line** — **`BLOCKED: backlog file missing required first line '# TASK <N> — <title>'. Repair the heading so N exactly matches the existing filename stem, or drop and recreate the idea through “+ New backlog item”; then re-run task-prep.`** Never calculate or allocate a task number inside task-prep.
- **Outcome appendix in backlog/pending** — **`BLOCKED: outcome appendix present in <backlog/|pending/> — file was carried back from done/ without an authorized lifecycle rewrite. Reconcile it through the canonical in-column edit/recreate flow, then re-run task-prep; do not strip the trailer by hand.`**
- **Mode B inline answer write mismatch** — after persisting inline answers and re-reading, if `round` or the `### Answer` count don't match what was sent: **`BLOCKED: answer sidecar write failed — re-submit`**.
- **Promote transaction failed** — if `transition-task-state.mjs promote` does not return a matching v1 todo receipt: **`BLOCKED: promote transaction failed — source state preserved`** plus the bounded finding codes. The helper rolls back its candidate and restores the exact backlog/pending source before returning failure.
- **Acceptance bullet lacks an automation anchor** — **`BLOCKED: acceptance bullet "<bullet, first 80 chars>" lacks an automation anchor (file/class/gradle task). Move it to ### Manual, or rewrite it with an explicit anchor.`** Do NOT silently move it — the user decides.

---

## Figma cache pre-flight BLOCK (`task-prep` Step 5.5)

When `check-screen-cache.mjs ... --gate` exits non-zero (a non-`none` `## Design` bullet has no cached
screen): release the lock and return
**`BLOCKED: screen design cache missing for <screens>. Press "Pull Figma screens" on the task card (figma:screens:<stem> session), or run the figma:screens pull prompt manually (screensPrompt in orchestrator/site/scripts/figma-actions.js — the screen-cache authoring contract), then re-run task-prep on the same stem.`**
This is the only hard stop in the design path. See [`figma-split.md`](figma-split.md).

---

## Escalation points (`task-intake` Step 6)

Mark any of these in the execution plan:

- **Migration required**: the user MUST authorize Room migrations explicitly. If the task didn't, end the plan with: "BLOCKED: needs migration authorization. Choose: (a) edit the TASK file to add an explicit `Authorise Room migration: yes` line and re-run, (b) reply with that authorisation in chat (orchestrator re-invokes me with the addendum), or (c) drop the schema-changing part of the task and re-run task-prep without it."
- **Backend contract unverified**: if `endpoint-builder` needs to run but the task doesn't link to a Swagger doc, first run `cd orchestrator/api-contract && npm run --silent contract:paths` and consult its resolved `inventory` path (see the backend-contract-client skill, references/endpoint-inventory.md) before blocking:
  - **Endpoint in the inventory** → derive `<areasDir>/<area>.json` from the resolver output and existence-check that exact file. If present, cite its full resolved project-relative path plus `schemaRef` as the contract in the plan and do NOT block. If the inventory lists the endpoint but the resolved **area slice is missing**, treat it as an invalid/partial snapshot — fall through to the next branch.
  - **Absent (or the area slice for a listed endpoint is missing) and `backendContractEnabled: true`** → "BLOCKED: endpoint not in the contract snapshot (or its area slice is missing) — run Backend Test + Refresh (or typed `contract:probe` then `contract:refresh-*`), or fix the task."
  - **Absent (or no snapshot) and gate `auto`** → "BLOCKED: needs a configured Backend environment. Choose: (a) configure the source in Backend and run Test + Refresh, (b) use typed `contract:probe` then the matching `contract:refresh-*`, or (c) defer the endpoint change until the backend contract is stable."
  - **Gate `false`** → skip the snapshot consultation and mark the same BLOCKED message with options (a)–(c) only — the contract tooling is ignored entirely.
- **Cross-cutting refactor**: if the task hints at renaming an existing class / module / DTO, mark "ESCALATE: rename impacts existing consumers — needs human review per `../../validation-gates/references/when-to-stop-and-ask.md`. Choose: (a) keep the rename in this task and accept that the orchestrator will route consumer fan-out as part of the same run (may be slow), (b) split the rename into a separate dedicated task and re-run task-prep on the smaller scope, or (c) cancel the rename and find a backward-compatible alternative."
- **New feature module**: if the task implies creating a brand-new `:ui-screen-features:<x>`, prepend `feature-module-scaffold-builder` to the plan and proceed — do NOT escalate (the scaffold is the routine entry point for fresh features). Only escalate if the task wants more than one sub-screen at once AND the multi-screen shape is unclear: "ESCALATE: feature implies multi-screen hosting — confirm whether to host first sub-screen as the feature root or introduce internal StackNavigation. Choose: (a) host the first sub-screen as the feature root (simpler, no StackNavigation), (b) introduce internal StackNavigation in the new feature module from the start, or (c) split the multi-screen ask into one task per sub-screen and re-run task-prep on each."

---

## Convergence-stall ESCALATE (`task-prep` Step 6.4)

Two triggers, evaluated in order:

- **Stuck convergence** (hard stop): `T_now >= T_prev` AND last round was also stuck (`T_prev >= T_prev_prev`, both fields present). One stuck round alone never escalates; only two-in-a-row does. Return (use the `last 3:` triple when `T_prev_prev` is reliably present; when `prevGapCount` is absent/blank from a hand-edited sidecar, drop the stale middle value and use the `last 2:` form with the caveat):
  > `ESCALATE: backlog item resists clarification — gap count not shrinking across rounds (last 3: <T_prev_prev>, <T_prev>, <T_now>). The task may be structurally too big, contradict a project constraint, or chase a moving target. Choose: (a) split into smaller tasks, (b) rewrite the backlog with more upfront detail, (c) drop the task.`
  >
  > When `T_prev_prev` is not reliably available, print `(last 2: <T_prev>, <T_now>)` and append `(prior-round history unavailable — sidecar hand-edited)` instead of a 3-tuple with a blank middle.
  >
  > When the user takes option (a), record lineage on each child: add an optional `## Origin` section with one bullet `- split from <parent-stem>` (see `orchestrator/tasks/README.md` task-file shape). `regen-index.py` parses it into the INDEX `splitFrom` field, and the site shows the "↰ from TASK N" badge + a Lineage section linking parent↔children. (The site's "+ New backlog item" form writes this automatically when a parent is picked in its "Split from" field.)
- **Soft warning at round 5+** (informational, not blocking): if `round >= 5` and `T_now > 0`, prepend a one-line note to the output:
  > `Note: round 5+ — typical tasks promote within 2-3 rounds. Consider whether this task is structurally too big to clarify in a single backlog.`
  Still iterate; do not auto-escalate just from round count. The hard stop is convergence (the 6.3 ESCALATE row), not iteration count.

### ESCALATE output blocks

```markdown
# task-prep — ESCALATE

**Task:** TASK_<N>_<title>
**Reason:** <one line — refactor opt-out, multi-feature scope, dependency cycle, etc.>

**Suggested action:**
- <bullet>
- <bullet>
```

Convergence-stall variant (Step 6.4 trigger):

```markdown
# task-prep — ESCALATE

**Task:** TASK_<N>_<title>
**Reason:** convergence stall — gap count not shrinking across rounds (last 3: <T_prev_prev>, <T_prev>, <T_now>).

**Suggested action:**
- Split the task into smaller scoped tasks (one per decision area).
- Rewrite the backlog with more upfront detail so fewer clarifications are needed.
- Drop the task.
```

When `T_prev_prev` is not reliably available (absent/blank `prevGapCount` from a hand-edited sidecar),
replace the `**Reason:**` line's `(last 3: …)` with `(last 2: <T_prev>, <T_now>) (prior-round history
unavailable — sidecar hand-edited)` rather than emitting a 3-tuple with a blank middle.

---

## Recovery from BLOCKED (`task-intake`)

When `task-intake` returns a `BLOCKED:` plan, the orchestrator surfaces it to the user verbatim. The
user has three recovery paths — document this in the BLOCKED message itself:

- **Authorize one exact in-column edit.** Give the missing values to the parent session; it must publish the amended todo through `transition-task-state.mjs edit` with the current `sourceRevision`, then re-run intake only after the helper returns a matching v1 receipt. Never edit `orchestrator/tasks/todo/` directly.
- **Reply in chat with the missing answers.** The orchestrator may first re-invoke `task-intake` with those values as non-durable context, but any accepted amendment must still be published through the same exact `transition-task-state.mjs edit` flow before the task can continue. Chat context alone never changes canonical task state.
- **Choose an explicit lifecycle-safe reshape.** If the existing todo remains structurally valid, apply the user's exact amendment through the authorized `transition-task-state.mjs edit` flow and re-run intake. If it needs discovery again, create and prep a new backlog task, then explicitly drop the obsolete todo only after reviewing dependents. There is no `todo → backlog|pending` transition.

Always end a BLOCKED message with the explicit choice:

> Choose: (a) authorize an exact in-column todo edit and re-run, (b) reply with the missing values and the orchestrator will re-invoke me, or (c) create/prep a replacement backlog task and explicitly drop the obsolete todo after dependent-impact review.

The orchestrator caps this loop at 3 BLOCKED iterations. After the third rejection, the orchestrator
halts and asks whether the task is well-formed at all — do not keep producing BLOCKED plans past that
point.
