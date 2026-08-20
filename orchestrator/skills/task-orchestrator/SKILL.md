---
name: task-orchestrator
description: >-
  Run-loop mechanics for a single task — adopt when you need to run a task,
  orchestrate the build loop, drive builders to done, route validators, or
  drop a task. Covers the full lifecycle from a `todo/TASK_*.md` to a green,
  validated, reviewer-approved ship: task-intake → context-finder → planner →
  builders → validators → assemble → verify → screenshot → review → security →
  done. The parent session ADOPTS this role (it is a playbook, not a spawnable
  sub-agent). Use for "run task TASK_N", "orchestrate the build loop", "route
  validators", "drive builders to done", "reopen TASK_N", or "drop a task".
---

# task-orchestrator

The run-loop spine. The parent (top-level) session **adopts** this role and
drives one task end-to-end. It must run at top level: it spawns builders,
validators, and a reviewer, and a sub-agent cannot spawn sub-agents.
Operational entrypoint — this skill routes, it does not redefine the loop.

## When to use

A `todo/TASK_<N>_<title>.md` must reach "done": green build, validators clean,
reviewer-approved, outcome appendix written. Adopt this role to drive that loop,
to route validator/reviewer findings back to builders, or — on explicit user
instruction — to reopen or drop a task. Tasks in `backlog/`/`pending/` are
**task-prep**'s, not yours.

Do NOT write product code here. You coordinate; builders write, validators
check (see `## Stop and ask`, "no inline product code").

## Required inputs

- The task file path `orchestrator/tasks/todo/TASK_<N>_<title>.md`.
- A passing Step 0 scaffold check + `orchestrator/project-config.md` flags
  (`figmaEnabled`, `iosEnabled`, `verifyEnabled`, `codexEnabled`,
  `backendContractEnabled`, `supportedLocales`).
- `TASK_STEM` = the todo/done filename without `.md` — the identity for the
  lock, journal and checkpoints under the CONTROL cache
  (`.cache/tasks/locks|journal/<STEM>`). A run executes in its own worktree at
  the sealed base commit, so there is no shared baseline to namespace: the
  base commit IS the baseline and the candidate diff IS the footprint.
## Workflow

Step 0 bootstrap check → Step 0.5 verify writer authority, then acquire/verify
the canonical `task-lock.mjs` receipt + journal → then the loop. Never
hand-write or overwrite `.cache/tasks/locks/<STEM>.json`. A standby caller's
complete guarded writer-lease receipt is exact-verified and reused; its exact
`sessionId`, `leaseId`, and private `token` are passed to standby task-lock
acquire so the helper can verify the capability before and after publication
without persisting it. This skill neither acquires a second lease nor releases
the caller-owned generation. The
**parent adopts this role**; every `Agent(...)` below is a top-level spawn.

1. **task-intake** → execution plan (classification, builder routing, validator
   set, gate sequence; see `## Output contract`).
1a. **Pre-flight (PARALLEL, single message):** `inputs-resolver` ‖
   `context-finder` × N. Resolve Tier-0 existence questions from
   `.arch-map.json` first (drop those from context-finder). Branch on the
   resolver verdict; scan results for `MAP_MISS`/`REGISTRY_MISS`/`CONTRACT_MISS`
   and carry each forward (never silently drop).
1b. **Screen pre-flight (conditional):** `figmaEnabled` + any non-`none`
   `## Design` bullet (any node kind; kind picks the capture harness, not whether
   gates run) → check screen cache, run census, spec-sanity, evidence-bundle.
   Carry the census table into the planner + builders.
2a. **implementation-planner** → file-level contract (names/paths/signatures/
   acceptance mapping; there is no standalone Step 2 — the number is kept for
   citation stability). Pass it **verbatim** to every builder and to
   `acceptance-tracer`.
3. **Builders** in `builderSequence`; parallel only on disjoint file sets. After
   each builder run **Step 3.5 diff-sanity**: in the execution root the whole
   `git status --porcelain` IS this task's footprint (nothing to subtract).
4. **Validators (parallel):** always-on three (`build-validator`
   mode=compile, `scope-leak-validator`, `acceptance-tracer`) + the conditional
   set. **Dedup by `(file, rule_id)`**, highest severity wins; same
   `(file,rule_id)` from two validators counts once. Adjudicate *opposite*
   verdicts against the cited skill reference (not blind-route). Route deduped
   findings grouped by builder → re-run.
4.5. **Assemble gate:** `build-validator` mode=assemble. FAIL → route →
   re-converge in Step 4 first.
4.6. **Runtime verify** (optional; `verifyEnabled` + `Skill` tool). FAIL
   → route → goto Step 4.
4.6b. **Screenshot fidelity** (conditional; `figmaEnabled` + non-`none`
   Design). Missing oracle/capture is a Blocker, not a skip; pixel-similarity
   severity follows `screenshotPixelGate` (`strict` by default — blocks;
   `advisory` warns). Blocker → route → goto Step 4; advisory findings → `### Caveats`.
5. **Review:** select one reviewer per attempt using the shared
   `reviewer-status.cjs` availability contract, then journal the locked
   `reviewer` / `reviewAttempt` / `selectionReason`. Critical/Major → goto Step 4;
   Minor/Style/Info → batch or `### Caveats`. The loop is **this role's**, not
   the reviewer's — the reviewer is single-pass. Never switch reviewers after
   an attempt starts; an invocation failure closes that attempt as failed.
5.5. **Security review** (conditional; diff touches
    token/auth/credential paths). **No severity threshold** — any finding routes.
6. **Finish (same turn):** current sealed test-summary → strict Outcome-shape
    validation → completed `ship` checkpoint bound to the exact run/worktree/
    execution tree → generation-bound Outcome draft in the control cache →
    6b–6d hand-off. Missing or stale evidence is not completion and MUST NOT
    leave the exact draft. The manager seals the candidate; the owner's **Integrate**
    runs the whole publication (product apply → finalizer prepare → ONE canonical
    commit → finalizer confirm → lock release). A run never publishes.

**Numeric caps (do not change — frozen in `orchestrator-loop.md`):**

| Cap | Value | Reset scope |
|---|---|---|
| build-failure rollback hint | resists **2** builder retries | per task |
| BLOCKED loop (intake / inputs-resolver / planner) | **3** iterations | shared, per task |
| Step 4 rotation (stable finding-set) | no shrink across **2** consecutive cycles | per Step-4 entry session |
| Step 4 outer re-entry | **7** | per task, never reset mid-task |
| Step 4.5 assemble | **2** consecutive FAILs (PASS resets) | per task |
| Step 4.6 runtime verify | **3** invocations | per task |
| Step 4.6b screenshot | **3** invocations | per task |
| Step 5.5 security review | **2** iterations | per task |
| acceptance bullet repeatedly missing | **2** consecutive validator cycles | per task |
| site runner concurrency | `MAX_PARALLEL=2` (canary; source constant, no env override) | n/a |

**Lock + journal discipline:** acquire the lock at Step 0.5; on the happy path,
only the integration transaction's finalizer confirm releases it, after the
canonical commit verifies (an explicit terminal drop **before finalization
starts** is the other owner action; a marker makes Drop unavailable) — never on an intermediate
`BLOCKED`/`ESCALATE` (the lock means "in the pipeline", including
paused-for-clarification). Only the parent emits journal events
(`log-event.py`, best-effort `|| true`).

**Durable retry checkpoints:** after a phase reaches a terminal
completed/failed/blocked verdict, the parent may publish its receipt only
through `node orchestrator/tasks/task-checkpoint.mjs create --stem "$TASK_STEM"`.
The producer proves the current task-lock `runId`, task/source/project/config/
dependency revisions and the server-owned retry matrix; journal text is never
checkpoint authority. Record the returned `checkpointId` on the matching
journal terminal event. If publication or receipt verification is unavailable,
continue or stop according to the phase verdict but advertise only a safe
restart — never infer repeatability from the journal.

## Stop and ask

Keep the lock while escalated. Surface a `BLOCKED[<type>]:` (tag first on line 1)
when:

- **task-intake / inputs-resolver / planner BLOCKED** — under-specified task,
  unresolvable HIGH Inputs claim, or planner⇄intake disagreement. Loop ≤ **3**.
- **Census regression** — `MISSING`/`INCOMPLETE`/`AMBIGUOUS` (Step 1b) or a
  builder's `MISSING_COMPONENT`/`AMBIGUOUS_COMPONENT`. Never let a builder
  approximate the widget.
- **Screen cache missing/stale** — `BLOCKED[figma-screens]`; user pulls via the
  card's "Pull Figma screens" button. You never call Figma yourself.
- **Architecture change required** — a finding hits a stop-point in
  [`../validation-gates/references/when-to-stop-and-ask.md`](../validation-gates/references/when-to-stop-and-ask.md).
- **Loop divergence** — Step 4 outer re-entry hits **7**, or a finding-set
  stable across **2** cycles, or an acceptance bullet missing across **2** cycles.
- **No builder maps to the task** — `BLOCKED[task-shape]`. Never inline-write
  product code; the user picks extend / split / reject.
- **Build red after a builder's "done"**, or `verifyEnabled=true`/
  `codexEnabled=true` with detector availability other than `available` → HALT
  (never silently fall back).

Whenever the escalation carries a decidable question, publish it durably into
the task body first — `transition-task-state.mjs publish-questions`, grammar and
rules in [`references/run-loop.md`](references/run-loop.md) § Escalation
triggers — then end the turn with the same `BLOCKED[<type>]:` report. A
turn-ending report alone gives the owner no answer rail. The answered task comes
back to you as an ordinary `run`.

Never auto-rollback, never commit/push, never edit product code, never
self-review.

## References to read

This skill is self-contained — it carries its own rules and reads **only** these
references + the frozen contracts at runtime.

- Routing table (loop step / topic → reference file + frozen contract):
  `references/index.md`.
- Self-contained procedure references:
  - `references/run-loop.md` — Steps 0–6 spine, lock/journal discipline,
    escalation, hard rules, move-back.
  - `references/validator-routing.md` — Steps 4 / 4.5 / 4.6 / 4.6b / 5 / 5.5:
    validator wave, dedup, all caps, gate routing matrices.
  - `references/outcome-appendix.md` — Step 6 chat summary + `## Outcome`
    appendix (6a–6d).
  - `references/planner.md` — implementation-planner file-level contract.
  - `references/context-finder.md` — arch-map-first context lookup + miss
    signals.
  - `references/requirements-lookup.md` — keyword → skill-reference
    dispatcher.
  - `references/task-drop.md` — drop / terminal-abandon a task.
- Frozen contracts (the authority for caps + output shapes — cite, don't
  re-derive):
  - `orchestrator/contracts/orchestrator-loop.md` — numeric caps + dedup +
    footprint + parent-adopts-role pins.
  - `orchestrator/contracts/execution-plan.md`,
    `orchestrator/contracts/planner-output.md` — routing + planner shapes.
  - `orchestrator/contracts/validation-run.md`,
    `orchestrator/contracts/reviewer-output.md` — validator + reviewer
    output envelopes.
  - `orchestrator/contracts/acceptance-trace.md` — final `## Outcome` →
    `### Acceptance trace` shape.

## Validators / gates

This skill **invokes** the `validation-gates` skill — it does not redefine the
gates. The Step-4 always-on three (`build-validator`, `scope-leak-validator`,
`acceptance-tracer`) run on **every** task regardless of intake; conditional
validators run when their kind applies and fail-closed on skip. The reviewer
(Step 5) and the security gate (Step 5.5) route per the matrices in
`references/validator-routing.md`.

## Output contract

- Resolved per-task routing (classification, builder order, validator set, gate
  sequence) → `orchestrator/contracts/execution-plan.md`, which references the
  `orchestrator/contracts/planner-output.md` plan it routes.
- The loop mechanics (caps, dedup, footprint, parent-adopts-role) are pinned in
  `orchestrator/contracts/orchestrator-loop.md`.
- Validator + reviewer outputs → `orchestrator/contracts/validation-run.md`,
  `reviewer-output.md` (via `validation-gates`).
- The final `## Outcome` appendix (six required `###` headings + optional
  `### Execution log`; `### Acceptance trace` verdicts `verified|manual|deferred`)
  → `orchestrator/contracts/acceptance-trace.md`.
