# Run loop — Steps 0–6 (the spine)

Self-contained orchestrator run loop. The parent
(top-level) session **adopts** this role and drives one task end-to-end. It must
run at top level: Steps 3–5 spawn builders, validators, and a reviewer, and a
sub-agent cannot spawn sub-agents — so the parent executes this procedure
directly rather than delegating it to a sub-agent (which would fail at the first
builder). Every `Agent(subagent_type: …)` call below is therefore issued by the
top-level session.

All numeric caps cited here are frozen — match them exactly against
[`orchestrator/contracts/orchestrator-loop.md`](../../../contracts/orchestrator-loop.md);
never re-derive them loosely.

## Execution model

- **The orchestrator is a playbook, not a spawnable worker.** Do not delegate the
  whole pipeline to a sub-agent — it fails the instant it tries to spawn the
  first builder. The parent adopts this role by reading and executing this file.
- **The spawning tool lives only at the top level.** Every agent this playbook
  spawns — all builders, all validators, the external reviewer — completes its
  work using only its own read/analysis tools and must **not** spawn sub-agents.
  A reviewer or review-loop that "iterates until clean" is iterated by *this*
  role re-invoking it, not by the reviewer spawning anything itself.

## Authoritative reading (verify each exists before starting)

1. `orchestrator/skills/_index/install-manifest.json` — the skills roster (the
   installed skills under `.claude/skills/`; `capabilities.json` next to it maps
   each logical operation to its owning skill). This is the skill catalog
   of the agents you orchestrate.
2. `orchestrator/project-config.md` — runtime config
   every agent reads.
3. `orchestrator/skills/validation-gates/references/when-to-stop-and-ask.md` — when
   to surface a blocker instead of pressing on.
4. `orchestrator/tasks/INDEX.json` — the last canonical board projection. The
   atomic finalizer alone republishes and verifies it after `todo → done`;
   orchestration agents never edit or regenerate it independently.

There are 39 frozen agent contracts under `contracts/agents/`; the directory is
the roster source of truth, and `doc-counts.sh` mechanically guards this count.
The validator split is `inputs-resolver` pre-flight plus the 10-validator
parallel group; the 5 remaining validators are conditional — the 2 `figma-*`
component validators (`figma-drift`, `figma-component-coverage`)
run only when `figmaEnabled: true`,
`figma-spec-validator` runs only when `figmaEnabled: true` AND the task carries
non-`none` `## Design` bullets, `figma-screenshot-validator` runs (mandatory)
whenever `figmaEnabled: true` AND the task has non-`none` `## Design` bullets — a
pulled oracle + capture are REQUIRED inputs (missing ⇒ BLOCKER), not run-conditions
(Step 4.6b — after the assemble gate, not in the Step-4 parallel group),
and `backend-contract-drift` runs when a contract snapshot exists and
`backendContractEnabled` is not `false`, or when `backendContractEnabled: true`
requires a missing snapshot to be reported as an error.

Before starting, `[ -f <path> ]` each file in the list above. If any are missing,
stop and report `BLOCKED[scaffold]: required reading missing — <list>`. Do not
proceed on assumed content.

## Inputs from invocation

- The task file path: `orchestrator/tasks/todo/TASK_<N>_<title>.md`. Tasks in
  `backlog/` or `pending/` are **task-prep**'s, not yours.

## Step 0 — Bootstrap check

Before any other step, verify the project scaffold matches
`orchestrator/project-config.md`. Read that file first — every value below
references its fields. Required artifacts (fail with `BLOCKED[scaffold]: project
scaffold incomplete — <missing list>` if any are absent):

- `settings.gradle.kts` exists at the repo root.
- `:shared` module exists with `Koin.kt`, `RootComponent.kt`,
  `RootDirection.kt`, `RootContract.kt`.
- `:ui-screen-features:screen-api` exists with `RootRouter.kt`.
- `:data-services:backend` exists with `<apiClassName>.kt` (from project-config).
- `:data-services:database` exists with `Database.kt`.
- `:design-system:resources:provider` exists with `StringProvider.kt`.
- For each locale in `supportedLocales`, the matching strings file exists: `en`
  → `values/strings.xml`, non-English → `values-<lang>/strings.xml`.
- If `iosEnabled: true`, `iosApp/` exists.
- If `firebaseEnabled: true`, `androidApp/google-services.json` exists.
- **Test-foundation doctor (defense-in-depth).** Run
  `node orchestrator/tasks/task-test-foundation.mjs doctor --product-root .`.
  For an ordinary task only `READY` admits the run; anything else is
  `BLOCKED[foundation-prerequisite-missing]: <state>` — Step 0 never creates a
  prerequisite child, never edits the todo and never repairs the foundation
  (that is task-prep's Step 4.7). The single exception is a task whose Source
  is `follow-up/test-foundation-prerequisite`: it runs with
  `ABSENT_CAN_INSTALL` as the toolkit-only bootstrap (§ prep-flow Step 4.7),
  and a repeated doctor on it never recurses into another prerequisite.
- If `figmaEnabled: true`, the screenshot-gate enforcement net is WIRED:
  `git config core.hooksPath` prints `orchestrator/skills/checks/hooks`. Unwired,
  nothing mechanical catches a bare-`mv` ship past the gate (the pre-commit
  verify-done net is inactive), so an uncompared UI task can silently reach
  `done/`. If unwired → `BLOCKED[figma-wiring]: run
  orchestrator/skills/install-skills.sh . (or: git config core.hooksPath
  orchestrator/skills/checks/hooks)` — do not proceed. This is the same
  invariant the site runner enforces mechanically (it refuses `run` sessions
  while unwired) and `figma:doctor` reports (strict via `FIGMA_STRICT_WIRING=1`);
  this bullet covers runs that bypass the site (a /loop worker or a manual
  terminal session).

If `apiClassName` still holds a placeholder (`^<`), signal that the project-config
substitution (launch.md Step 1.5) was not run. If any check fails →
`BLOCKED[scaffold]: run orchestrator/launch.md to bootstrap the project first`.
Do not proceed to task-intake.

## Rollback safety

Before invoking the first builder, capture the workspace state. `TASK_STEM` is
the todo/done filename without `.md` (e.g. `TASK_3_chart_redesign`) — the SAME
identifier used for the Step 0.5 lock:

```bash
TASK_STEM=<STEM>
PRE_TASK_SHA=$(git rev-parse HEAD)
git status --porcelain > /tmp/orchestrator_pre_task_status_${TASK_STEM}.txt
echo "$PRE_TASK_SHA" > /tmp/orchestrator_pre_task_sha_${TASK_STEM}.txt
```

`TASK_STEM` namespaces every `/tmp/orchestrator_*` file per task. The site
runner is frozen at `MAX_PARALLEL=1` (serial safety; no env override until
per-task worktree isolation lands), so two live runs never overlap — but the
namespace still matters: `/tmp` survives crashed or consecutive runs, and an
un-namespaced baseline from a previous task would silently feed the
diff-sensitive validators the wrong baseline. Establish it here, before the
first write, and carry the same value into every later read and into the
validator/reviewer briefs.

- `/tmp/orchestrator_pre_task_sha_${TASK_STEM}.txt` is the contract
  `scope-leak-validator` reads to diff the working tree against the pre-task
  state. Removing it makes scope-leak fall back to `HEAD~1`, wrong when the
  orchestrator hasn't committed mid-task.
- `/tmp/orchestrator_pre_task_status_${TASK_STEM}.txt` is the **pre-task
  baseline** Step 3.5 diffs the post-builder tree against to isolate this task's
  true footprint when the tree carries uncommitted work from a prior task.

On any escalation, build failure that resists **2** builder retries, or
user-initiated halt, preserve and surface the recovery context instead of a
repository-wide rollback command:

- the exact pre-task SHA from
  `/tmp/orchestrator_pre_task_sha_${TASK_STEM}.txt`;
- the pre-task untracked/modified snapshot from
  `/tmp/orchestrator_pre_task_status_${TASK_STEM}.txt`;
- the current `git status --short` and a bounded path list from the task diff;
- which paths are known task-owned and which pre-existed or remain ambiguous.

Do not suggest or run a destructive whole-repository reset/clean. Preserve all
current bytes, report the exact task-owned candidates, and request explicit
user direction before any path-specific restoration or deletion.

## Step 0.5 — Acquire the in-progress lock (after Step 0 passes)

Before writing a run lock, fail closed if
`orchestrator/.cache/tasks/finalizations/` contains any non-mutex `*.json`
entry (including an unsafe/corrupt name). Finalization owns shared
INDEX/architecture/registry state across stems: return
`BLOCKED[finalization]: resume the existing durable finalization before starting another task`.
The site runner and standby worker enforce the same global rule; this check is
the defense for a direct skill invocation.

Never trust the mere presence of an inherited
`ORCHESTRATOR_WRITER_SESSION_ID`. For a site-started turn, validate that it is
still bound to exactly one active, verified, non-expiring, child-attached,
same-stem site `task-session` lease and that finalization has not won the
marker/mutex handshake. A TTL-bounded direct lease is never valid inherited
authority because it could expire between this check and the first mutation.
Run this **before any lock/task/INDEX write**, including the first mutation
after a resumed prompt:

```bash
STEM=<STEM>
node orchestrator/tasks/writer-lease.mjs verify-session --guard-finalization \
  --session-id "$ORCHESTRATOR_WRITER_SESSION_ID" --stem "$STEM"
```

Require exit 0. Exit 2 means the inherited credential is stale/unverified or
publication owns the workspace: make no mutation and return
`BLOCKED[finalization]: writer session is no longer authorized; restart or resume through the site`.
The verification is read-only and MUST NOT be replaced by an environment-value
shape check.

A standby `/serve-queue` invocation is a distinct third authority mode. The
worker has already acquired the guarded bounded `task-session` lease and MUST
pass its complete private receipt (`leaseId`, `token`, `sessionId`, `stem`,
`expiresAt`) into this parent context. Do not acquire a second lease. Before the
first mutation and every resumed turn, verify the exact caller-owned receipt:

```bash
node orchestrator/tasks/writer-lease.mjs verify --guard-finalization \
  --lease-id "<caller receipt leaseId>" --token "<caller receipt token>" \
  --session-id "<caller receipt sessionId>" --stem "$STEM"
```

Require exit 0 and an envelope matching the exact lease/session/stem. The
command accepts only one still-unexpired, verified, bounded `task-session`
generation and refuses a same-stem/global publication conflict. If both this
receipt and `ORCHESTRATOR_WRITER_SESSION_ID` are present, stop on ambiguous
authority. This skill may renew the caller-owned bounded generation before each
numbered phase and after any 30-minute operation, then verify again, but MUST
NOT release it: standby Step 8 owns the one final release after prompt
completion or refusal.

For a direct invocation (neither an inherited
`ORCHESTRATOR_WRITER_SESSION_ID` nor a caller-supplied standby receipt), publish
the cross-process writer lease **before any lock/task/INDEX write**:

```bash
STEM=<STEM>
node orchestrator/tasks/writer-lease.mjs acquire --guard-finalization \
  --kind task-session --stem "$STEM" --key "direct:run" \
  --owner-pid "$PPID"
```

`--owner-pid "$PPID"` is mandatory: it anchors the lease to the long-lived
session process (the executing shell's parent), not the per-call shell that
dies when this command returns. Renewal and verification require a still-live
owner identity, and the cross-process serial-safety exclusion must stay
anchored to the real writer for the whole run instead of degrading into a
bare TTL timer that could expire under a still-running turn. Run the command
at the top level of the tool call — never inside `bash -c`, a heredoc, or any
nested shell, where `$PPID` resolves to the dying per-call shell and silently
recreates the short-lived-owner defect. The flip side: a lease leaked by an
interrupted turn stays active while this session process lives and holds
every drainer — release it on every failure path, and if a hold persists,
`writer-lease.mjs scan` names the owner pid so that session can be ended
cleanly. Never delete lease files by hand.

Keep the returned `leaseId`, `token`, and `sessionId` in private run context.
Exit 2 means a marker/mutex — or, under frozen serial safety, another live
board-task writer for ANY stem — won the handshake: make no mutation. The helper
publishes the lease first and then rechecks finalization, so either this run or
the finalizer wins before either mutates. A successfully verified site-started
or caller-owned standby run already has the same lease and MUST NOT acquire a
second one. Hold the
direct lease until the current turn stops. Before the first mutation of every resumed direct turn,
acquire a fresh guarded generation **first**; only after that succeeds,
ownership-safely release the previous generation when its credentials are
available. Releasing old-first would reopen the finalizer race. Never assume a
one-hour crash-TTL lease is still active. Use only the fresh `sessionId`. Release it after successful
finalization or an explicit pre-finalization terminal Drop. On finalization, append
`--writer-session-id <returned-sessionId>` to the command. On an abandoned or
crashed direct run the bounded lease remains fail-closed until expiry rather
than allowing an overlapping writer. Normal release is `node
orchestrator/tasks/writer-lease.mjs release --lease-id <leaseId> --token
<token>` and its zero exit must be confirmed before forgetting the credentials.
For a direct bounded lease, run `node orchestrator/tasks/writer-lease.mjs renew
--lease-id <leaseId> --token <token>` before every numbered phase and after any
operation lasting 30 minutes. Never launch one opaque child expected to run
past the one-hour bound without a supervised renewal point. If renewal says the
generation expired, make no mutation: acquire a fresh guarded generation and
use its new `sessionId` before continuing.

Publish the canonical lock next so the board shows the task as "in progress"
before any further check. The lock must exist before `needs_action` is ever
called. Never hand-compose or overwrite its JSON.

- If the invoking caller supplies an already-acquired lock, it MUST pass the
  complete `task-lock.mjs` receipt into this parent run context. Verify that
  exact `stem`/`stage`/`runId`/`sessionId`/`lockHash`; do not reacquire it.
- Otherwise acquire once through the helper below. For a standby/direct run,
  pass the exact `sessionId` from its verified writer-lease receipt. A
  site-started run may omit `--session-id`; the helper binds the validated
  `ORCHESTRATOR_WRITER_SESSION_ID`.

```bash
STEM=<STEM>
# Site-started turn (the helper binds ORCHESTRATOR_WRITER_SESSION_ID):
node orchestrator/tasks/task-lock.mjs acquire \
  --stem "$STEM" --stage orchestrator \
  --owner-kind site --owner-id "site:<bounded run owner identity>"

# Direct turn (use the guarded writer-lease sessionId returned above):
node orchestrator/tasks/task-lock.mjs acquire \
  --stem "$STEM" --stage orchestrator \
  --session-id "<direct writer-lease sessionId>" \
  --owner-kind direct --owner-id "direct:<bounded run owner identity>"

# Standby turn (reuse the caller-owned receipt; never acquire another lease):
node orchestrator/tasks/task-lock.mjs acquire \
  --stem "$STEM" --stage orchestrator \
  --session-id "<caller receipt sessionId>" \
  --writer-lease-id "<caller receipt leaseId>" \
  --writer-lease-token "<caller receipt token>" \
  --owner-kind standby --owner-id "standby:<caller receipt leaseId>"
```

Require exit 0 and retain the returned `runId`, `sessionId`, `lockHash`, owner,
and `startedAt` as the immutable task-run lock receipt. Acquisition validates
canonical `run` admission (todo state, dependencies and fresh INDEX), publishes
no-clobber, then repeats that action check and compares state + source revision.
For standby, it also exact-verifies the private writer capability before and
after publication; the token is neither persisted nor returned. A missing,
released, replaced, wrong-action, or foreign standby generation rolls back
only the just-published lock and fails closed.
Post-publication drift releases only the exact just-published generation and
returns a transient failure. The helper refuses a
different, malformed, symlink, oversized, or racing owner without
changing it. If it refuses, release only a direct writer lease acquired by this
attempt, make no task mutation, and return `BLOCKED[conflict]` with the exact
lock finding. Never infer that an old timestamp makes the owner safe to clear.

For a prompt-owned lock, verify before the first mutation and after every
resumed turn:

```bash
node orchestrator/tasks/task-lock.mjs verify \
  --stem "$STEM" --stage orchestrator \
  --run-id "<receipt runId>" --session-id "<receipt sessionId>" \
  --expected-hash "<receipt lockHash>"
```

Do not rewrite the lock to refresh `startedAt`; journal/session activity is the
liveness signal. After the verified lock exists, check
for other in-progress tasks (`ls .cache/tasks/locks/*.json` minus your stem). If
another task IS in-progress in the same module area (compare `## Inputs` paths),
surface the risk via `needs_action` as `BLOCKED[conflict]` — the lock is already
written, so the awaiting modal renders. Otherwise proceed. Lock discipline does
**not** live in [`outcome-appendix.md`](outcome-appendix.md) — see
`## Lock + journal discipline` below.

## Lock + journal discipline

The board paints "в работе · orchestrator" while
`orchestrator/.cache/tasks/locks/<STEM>.json` exists. The lock means *"this task
is currently in the pipeline"* — including paused-for-clarification — not *"the
agent is crunching this very second"*.

- **Acquire** at Step 0.5. **Release** only by `finalize-task` Step 6d after all
  postconditions verify (the only happy-path release),
  or on a terminal drop where the user explicitly abandons the task **before a
  finalization marker exists** (after that, recovery owns the lock and Drop is
  refused).
- **Do NOT release on intermediate `BLOCKED`/`ESCALATE`/`HALT`** from ANY step
  before 6d (Steps 1, 1a, 1b, 2a, 3, 4, 4.5, 4.6, 4.6b, 5, 5.5 — the closed
  rule is "any step before the happy-path release"). The session stays alive
  waiting for the user's reply — the task is still in the pipeline.
- If the session is killed before finalization starts, the lock is left behind.
  Age alone never enables a clear action: recover only after proving the exact
  owner generation and use the receipt-bound helper. Once a finalization marker
  exists, use **Resume finalization**, which preserves the phase and verifies
  lock ownership.
- An escalation that published a durable `## Questions` section is the normal
  case of the line above: the answer needs a fresh `orchestrator` lock, so the
  board keeps the **previous run stopped** blocker in front of the answer rail
  until the owner releases the dead generation through that helper. You never
  release, reuse, or work around that lock yourself.

There is no manual happy-path release command. `finalize-task` removes the lock
only when its identity and bytes match the captured canonical receipt; a
changed/replaced lock is a conflict and is preserved.

**Journal.** Append structured events to
`orchestrator/.cache/tasks/journal/<STEM>.jsonl` via the shared writer
`orchestrator/tasks/log-event.py`. **Only the parent emits** — never instruct a
spawned sub-agent to log. **Best-effort, never fatal** — append `|| true` to
every call. Emit a `phase-start` when you enter a phase and a `phase-end`
(`ok`/`fail`/`skipped`) when it resolves; emit `stop` on exactly the escalations
that KEEP the lock; emit `phase-end --phase ship --status ok --column done` after
`finalize-task.mjs` reports complete. The journal is gitignored runtime telemetry; the permanent
digest is the `### Execution log` block in the `## Outcome` appendix (Step 6a).

Review events have a stricter shared contract. Each attempt uses one canonical
positive decimal `reviewAttempt`; its `phase-start` records `reviewer` and
`selectionReason`, and every terminal event repeats `reviewer` +
`reviewAttempt`. The reviewer choice is immutable for that attempt. Automatic
mode may select the internal reviewer only before `phase-start`, when
`reviewer-status.cjs` reports Codex unavailable/unknown; an invocation failure
after Codex was selected is logged as a failed/stopped Codex attempt, never as a
silent fallback. See Step 5 in `validator-routing.md` for the exact enum values
and commands. Step 5 first emits an informational review `gate`; until its
`phase-start` lands, that structured event means the task is waiting for review
to start.

**Checkpoint receipt after a terminal phase.** A retryable journal row is only
an index into a separately validated immutable checkpoint. Immediately after a
phase resolves, and before emitting the matching `phase-end`/`stop`, pass the
closed JSON input below on stdin:

```bash
node orchestrator/tasks/task-checkpoint.mjs create --stem "$TASK_STEM"
```

The input has exactly `runId`, `phase`, `attempt`, `status`,
`outputReceiptIds`, `priorPhaseReceiptIds`, `failureCode`, and `retryPolicy`.
Use the exact `runId` from the verified task-lock receipt; `status` is
`completed|failed|blocked`; receipt arrays contain only canonical durable IDs
actually produced/consumed by the phase (otherwise `[]`). A failed status needs
an allowlisted failure code. The retry policy is conservative:

- exact `retry-phase` is limited to preflight, validators, assemble,
  runtime/screenshot gates, review, security review, and design-pull, with
  `safePhase` equal to the failed phase;
- planner may use `resume-run` only with a complete planner-input checkpoint;
- builders/diff-sanity restart from a proven earlier safe phase, never blind
  exact retry;
- lock/prep/intake/ship use `restart-task` or `manual`.

The helper rejects a foreign/missing run lock, stale inputs, unknown fields,
unsafe policy, oversized receipt, and retention exhaustion. On success, copy
the returned `checkpointId` into `--meta checkpointId=<id>` and its policy into
`--meta retryPolicy=<kind>` on the terminal journal event. On failure, log no
checkpoint id. Checkpoint failure never authorizes a heuristic phase retry:
surface that only an earlier safe restart is available. The Site rechecks every
revision and every referenced receipt before presenting Retry.

## BLOCKED message format

Prefix every `BLOCKED`/`ESCALATE` surfaced via `needs_action` with a type tag as
the very first non-whitespace content on line 1; the site strips it and renders
contextual recovery buttons:

```
BLOCKED[<type>]:
<human-readable explanation and recovery steps>
```

Supported types: `figma-screens` (cache missing — Step 1b), `scaffold` (Steps 0,
0.5, 1a), `task-shape` (task-intake), `inputs` (inputs-resolver), `planner`,
`builder` (Steps 3, census backstop), `depends-on` (a required artifact is
produced by another board task — add an optional `DEPENDS-ON: TASK_<…>` line),
`conflict` (a git merge conflict, or another lock writing the same module area),
`general`. When surfacing a sub-agent's verbatim `BLOCKED: <msg>`, wrap it as
`BLOCKED[<type>]: <msg>` picking the type from which step returned it.

## High-level loop

```
1.  (after Step 0) task-intake → execution plan
1a. inputs-resolver ‖ context-finder × N → pre-flight in PARALLEL (one message)
1b. screen-design pre-flight (conditional: figmaEnabled + any non-none ## Design bullet)
2a. implementation-planner → file-level contract (passed verbatim to every builder)
3.  for each builder in plan.builderSequence: invoke with task + context + plan
3.5 diff sanity + footprint isolation after EVERY builder run
4.  run every applicable validator in PARALLEL — always incl. the three always-on
       (build-validator mode=compile, scope-leak-validator, acceptance-tracer)
    if any finding: dedup → route → goto 4
4.4 deterministic test certification — observed impact → allowlisted executor →
       sealed receipts/summary. exactly once per Step-4 entry; PASS/SKIPPED
       proceeds, anything else routes back to 4 (Step-4 outer cap: 7)
4.5 full assemble gate — build-validator mode=assemble; PASS→proceed (counter
       resets), FAIL→route→goto 4 (re-converge compile-mode first). cap: 2 consecutive FAILs
4.6 runtime verify gate (optional, verifyEnabled + Skill-tool gated). cap: 3
4.6b screenshot fidelity gate (conditional). cap: 3
5.  external reviewer (official Codex plugin review OR internal-reviewer — one per attempt)
       Critical/Major → goto 4; Minor/Style/Info → batch or ### Caveats
5.5 conditional security-review (only if diff touches auth/token/credential).
       no severity threshold; cap: 2 iterations
6.  finalize same turn: summary → 6a Outcome draft → 6b–6d
       `finalize-task.mjs --outcome-file` (components/tokens phases → sanctioned ship →
       INDEX/arch verification → ownership-safe lock release)
```

## Step 1 — Read the task and run intake

`Agent(subagent_type: "task-intake", prompt: "Read orchestrator/tasks/todo/TASK_<N>_<title>.md and produce an execution plan per your spec.")`

**Placement-feasibility probe (part of intake, advisory-strength).** When an
acceptance bullet places NEW code into a named module AND that code's signature
references a type from another module (a factory returning a feature-local
`State`, a helper taking a feature `Component`), the intake plan must check the
module graph (`.arch-map.json` Tier-0 read) for the required dependency edge:
the TARGET module must (be able to) depend on the TYPE's module. A reversed
edge — the type's module already depends on the target (`:ui-core:state` ←
feature module is the production shape: "add `stubSpeedTestState():
SpeedTestState.Verified` to `:ui-core:state`" = a circular dependency that only
surfaces at build time) — is a `BLOCKED[task-shape]` at intake, listing the
cycle and the smallest placement fix (usually: the type's own module). When the
arch map is stale/absent (`possible stale map`), degrade to an Open-assumptions
bullet instead of blocking — the builder then verifies before writing.

Wait for the structured plan. If it ends with **BLOCKED**/**ESCALATE**, surface
to the user and stop. **Recovery:** if the user edited the TASK file → re-invoke
with the same args; if the user replied in chat → re-invoke with the original
prompt plus "Additional context from user: <quote>". Loop on BLOCKED at most **3**
times (shared with inputs-resolver / planner). After three rejections, ask the
user whether the task is well-formed at all.

## Step 1a — Pre-flight (PARALLEL): verify Inputs + collect context

`inputs-resolver` and `context-finder` are independent — both consume only the
task-intake plan, neither reads the other's output. See
[`context-finder.md`](context-finder.md) for the Tier-0 arch-map pass and the
miss-signal protocol; this section is the orchestration around it.

**Tier-0 first.** Before composing context-finder calls, read
`orchestrator/.arch-map.json` if it exists. Answer existence/inventory/
relationship questions from the map (no spawn) and **drop them from the
context-finder prompt** — the avoided haiku spawn is the win. Pass map `file`
paths into the residual (Tier-1/2 "show me the signature") context-finder calls
so they open the exact file. `test -f` any map-derived path before handing it to
a builder — a miss falls back to a grep + a "possible stale map" note. If
`.arch-map.json` is absent, skip this pass and spawn context-finder as normal —
the map is a pure accelerator, never a prerequisite.

Issue the resolver and the residual context-finder calls **in a single message
with multiple Agent tool uses** so they run concurrently:

```
Agent(subagent_type: "inputs-resolver", prompt: "Read orchestrator/tasks/todo/TASK_<N>_<title>.md and verify each Inputs claim resolves against the live codebase.")
Agent(subagent_type: "context-finder", prompt: "<consolidated residual questions for builder X>")
Agent(subagent_type: "context-finder", prompt: "<consolidated residual questions for builder Y>")
... one context-finder call per builder with residual questions ...
```

A builder fully answered by Tier-0 gets **no** context-finder call — record the
map-derived answers as that builder's "context-finder excerpts" for Step 2a. If
every builder was fully Tier-0, the parallel block is `inputs-resolver` alone —
that is the success case (maximum map hit), not an error.

Wait for **all** results, then branch on the resolver verdict:

- **`inputs-resolver` BLOCKED** (≥1 HIGH-severity unresolvable Inputs claim) →
  surface verbatim. Discard the context-finder excerpts (speculative haiku-tier
  work — the explicit trade-off). Same three recovery paths as task-intake
  BLOCKED. Do NOT proceed to the planner.
- **`inputs-resolver` MEDIUM** (signature drift) → does NOT block. Carry the
  drift bullets into the plan's "Open assumptions" for the responsible builder.
- **`inputs-resolver` PASS** → use the excerpts directly in Step 2a.

**Signal scan.** Before branching, grep each context-finder result for
`^MAP_MISS:`, `^REGISTRY_MISS:`, `^CONTRACT_MISS:` and name any found, then apply
the matching handler (full handlers in [`context-finder.md`](context-finder.md)):
`MAP_MISS` → carry a `possible stale map` line into Open assumptions (Step 6c's
arch regen heals it next task; do NOT regen here). `REGISTRY_MISS` → carry a
`missing figma component mapping` line; the builder must NOT approximate with
`Box`/`Row`; do NOT run the figma pipeline here (needs the user-bound MCP
session). `CONTRACT_MISS` → carry a `possible stale contract snapshot` line; do
NOT pull here (needs the backend reachable + user action).

Cap the inputs-resolver BLOCKED loop at the same **3**-iteration budget shared
with task-intake.

> **Numbering note:** Step 2 is intentionally unused. Continue from Step 1b to
> Step 2a (the planner); the stable numbering keeps downstream citations valid.

## Step 1b — Screen-design pre-flight (conditional)

Fires whenever **both** hold: `figmaEnabled: true` AND the task's `## Design`
carries ≥1 non-`none` bullet — **any** node kind
(`screen`/`dialog`/`component`/`overlay`), regardless of which builders the
intake plan includes. This is the same kind-agnostic predicate the enforcement
layer gates on (Step 4.6b, `evidence-bundle`, `ship-done`): the node kind picks
the CAPTURE HARNESS (gate spec §2), never whether the gates run — a
`[component]`-only task still needs this full pre-flight (cache check, census,
check-spec, prebuild bundle) or the final `--stage final --fresh`
bundle blocks it as unshippable. Otherwise (non-UI task) skip to Step 2a.

One Figma run id spans the whole task (Step 1b through Step 4/4.6b/final
evidence/ship-done). The id is **file-pinned per stem**: the first figma tool
invocation mints it and writes `orchestrator/.cache/figma/reports/.run-id-<stem>`;
every later invocation (any shell, any turn, any sub-agent) reuses the pinned id
automatically — no `export` is required anymore. An exported
`FIGMA_PIPELINE_RUN_ID` still WINS over the pin (and re-pins the file), so an
explicit id remains available for diagnostics. A full `evidence-clean <stem>`
removes the pin (next run = fresh id); `--bundle-only` keeps it (an immediate
re-bundle must stay on the same run). Every Figma report in this task must carry
that same id; the final `--stage final --fresh` bundle rejects mixed reports as
stale (`REPORT_STALE_RUN` — its message names the pinned id and the exact re-run
recipe).

0. **Toolchain preflight** — `node_modules` is gitignored, so a fresh clone has
   no figma toolchain even when the screens cache is committed. If
   `orchestrator/figma/node_modules` is absent, run root `npm ci` FIRST
   (deterministic from the committed root `package-lock.json`;
   offline-safe once the npm cache is warm). Distinguish the two failure
   states: a module-load error from any gate script (`AJV_UNAVAILABLE`,
   `ERR_MODULE_NOT_FOUND`, "Cannot find package") is TOOL-missing — remediation
   is `npm ci`, never `BLOCKED[figma-screens]`/"Pull Figma screens" (a pull
   cannot install the toolchain; its own gate would fail the same way).
   Cache-missing is what steps 1–2a below diagnose.
0b. **Preferred: run the driver.** `node
   orchestrator/figma/scripts/run-figma-gates.mjs <stem> --stage prebuild` runs
   steps 1→2b below in order under the file-pinned run id (one command, no env
   choreography). The per-command breakdown below remains the DEBUG path — read
   it to diagnose a blocked driver step; the blocker semantics per step are
   identical either way.
1. **Cache check** — `node orchestrator/figma/scripts/check-screen-cache.mjs
   <stem> --gate`. On a cache-missing BLOCKER (read
   `.cache/figma/reports/screen-cache-<stem>.json`):
   `BLOCKED[figma-screens]: screen design cache missing for <screens>. Pull it
   via the task card's "Pull Figma screens" button (figma:screens:<stem>
   session)…`. Intermediate BLOCKED — the lock stays. **You never call Figma
   yourself** (golden invariant); the pull happens in the user-bound session.
   That pull session also runs `normalize-oracle.mjs` as a mandatory step:
   embedded iOS device chrome (the "9:41" status bar / home indicator) is
   stripped from each oracle PNG+spec at the pull boundary and stamped as
   `chromeCrop`, so the cache this loop consumes is already chrome-free. A
   `CHROME_CROP_*` blocker from this gate means the stamped crop went
   inconsistent — the remedy is a re-pull (which re-runs normalize-oracle),
   never a builder code fix and never a threshold change.
2. **Census** — `node orchestrator/figma/scripts/component-census.mjs <stem>`;
   keep the table + `census-<stem>.json`. Any unresolved status —
   `MISSING`/`INCOMPLETE`/`AMBIGUOUS`/`UNSUPPORTED`/`RETIRED`/`SOURCE_STALE`
   = task-prep's Step 5.5 split/ask was skipped, or the mapping registry /
   design inventory changed → escalate with the census-regression message (same
   as the Step 3 backstop). A `MISSING` row carrying `codeCandidates` (report
   `reuseCandidates[]`) is the same escalation — the reuse-or-create pick
   (task-prep Step 5.5·3a) belongs to the owner; never resolve it here by
   building, splitting, or mutating the mapping registry.
2a. **Spec sanity** — `node orchestrator/figma/scripts/check-spec.mjs <stem>
    --gate`; keep `check-spec-<stem>.json`. Blocks on schema-invalid specs,
    missing stable identity, invalid fills/strokes, negative geometry,
    placeholders. WARN-only stays advisory; pass the report to
    `figma-spec-validator` (Step 4).
2a′. **Text-content parity obligation (advisory, deferred)** — do not run this
    during Step 1b; after Step 3's builders land, run
    `node orchestrator/figma/scripts/check-stub-text.mjs <stem>`: every design
    text the pull recorded (`elements[].text`) must exist in the code; a named
    `TEXT_NOT_IN_CODE` WARN means the stub/copy drifted from the design (the
    "invented 312 Mbps" class) — route it back to the builder as a content fix,
    never as a gate relaxation. A pre-text-contract spec reports nothing.
2b. **Evidence bundle** — `node orchestrator/figma/scripts/evidence-bundle.mjs
    <stem> --stage prebuild --fresh`; pass `evidence-<stem>.json` forward. Rerun
    the final command before Step 5 (see Step 4.6b).
3. **Carry it forward** — pass the census table to the planner (Step 2a) and to
   every screen/dialog builder (Step 3). Builders consume only `MAPPED` fqNames.

## Step 2a — Produce the implementation plan

Once context-finder excerpts are in hand, invoke the planner (see
[`planner.md`](planner.md) for its full procedure and output shape):

```
Agent(subagent_type: "implementation-planner",
      prompt: "Task: <verbatim task content>. task-intake plan: <verbatim plan>. context-finder excerpts (from the Step 1a parallel pre-flight): <verbatim excerpts>. inputs-resolver signature-drift bullets (MEDIUM, non-blocking — empty if PASS): <bullets or 'none'>. Produce a file-level contract per your spec; surface the drift bullets as 'Open assumptions'.")
```

**Keep the returned Markdown block verbatim** — pass it to every builder in Step
3 and to `acceptance-tracer` in Step 4. If the planner returns **BLOCKED**,
surface verbatim; do NOT proceed to builders. Planner BLOCKED iterations share
the **3**-iteration cap with task-intake.

## Step 3 — Run builders

Confirm the pre-task snapshot was captured (re-capture if missing, before the
first builder). Verify every builder name from the plan is a known capability
whose owning skill is installed. In skills-only mode a builder is no longer a
standalone agent file — it is a capability carried by an implementation
skill (and pinned by its frozen agent contract). Resolve each builder to its
owning skill and confirm both the contract and the skill exist:

```bash
# builder → owning implementation skill (the skill that carries the capability)
builder_skill() {
  case "$1" in
    screen-builder|dialog-builder|feature-module-scaffold-builder|\
      ui-core-state-builder|cross-feature-nav-builder)       echo ui-feature ;;
    data-feature-builder|data-service-scaffold-builder|\
      room-migration-builder)                                echo data-layer ;;
    mapper-builder)                                          echo mappers ;;
    endpoint-builder)                                        echo backend-contract-client ;;
    design-system-component-builder|resource-builder)        echo design-system ;;
    toolkit-builder|compose-lib-builder|app-shell-builder)   echo platform-build-toolkit ;;
    *)                                                        echo "" ;;
  esac
}
for builder in <names from plan>; do
  # known builder? its frozen agent contract is the roster anchor.
  [ -f "orchestrator/contracts/agents/${builder}.md" ] || \
    { echo "BLOCKED[builder]: builder '${builder}' is not a known capability (no agent contract)…"; exit 0; }
  skill=$(builder_skill "$builder")
  [ -n "$skill" ] && [ -f ".claude/skills/${skill}/SKILL.md" ] || \
    { echo "BLOCKED[builder]: builder '${builder}' maps to skill '${skill:-?}' which is not installed…"; exit 0; }
done
```

Proceed in strict plan order; parallel groups in one message. For each builder,
pass: the full task file content; the relevant context-finder excerpts; **the
Step-2a plan verbatim** (the per-builder contract is the authoritative file list
and naming table — deviation must escalate, not silently rename); prior builders'
outputs (copy the verbatim signature from the report or plan — never retype from
memory); and the explicit scope statement *"You are responsible only for the
files listed under your section in the implementation plan. Do not refactor
adjacent code."*

For a `figmaEnabled` screen/dialog builder (task with non-`none` `## Design`
bullets), also point it at the pulled design cache from Step 1b as REQUIRED build
inputs — per `## Design` bullet, the value spec
`orchestrator/.cache/figma/screens/<stem>/<Screen>.spec.json` and the oracle mock
`<Screen>.png` (plus `.dark.*` per dark theme). The builder BUILDS to that design
(layout, tokens, and the oracle's content/sections) and seeds its preview/capture
stubs from the oracle content — not just from the prose task. It reads these
pulled files only (golden invariant: builders never call Figma/MCP — only the Step
1b pull session does). From the Step-1b census table, also hand the builder each
MAPPED component's `implementations[]` — the `projectComponentId`
(`<adapterId>:symbol:<fqName>`) **and `sourcePath`** the census rows carry — so
the builder reuses those composables at their real call sites instead of
re-deriving where a component lives (or, worse, re-implementing it inline).

After each builder reports, pipe its exact JSON bytes to
`node orchestrator/tasks/task-builder-report-contract.cjs` on stdin and close
stdin. Accept only exit 0 and the normalized current `schemaVersion: 1`
envelope; unknown versions, missing/extra fields or malformed nested claims
stop the run as `BLOCKED[builder-report]`. There is no compatibility reader,
conversion, or persisted intermediate file. Capture files created/edited,
build status and open questions only from that validated envelope. If it
reports a blocker (e.g. a missing `<X>Feature` interface), pause the chain and
escalate — don't silently invent the missing piece.

**Census-regression backstop.** If a screen/dialog builder's report contains
`MISSING_COMPONENT` or `AMBIGUOUS_COMPONENT`, HALT:
`BLOCKED[builder]: census regression — <component> unmapped at build time
(mapping registry changed since prep?). Re-run task-prep…`. Do not let the builder
approximate the widget; do not route as a validator finding — it is terminal for
the run. Occurs only with `figmaEnabled: true`.

**Mapping-registry ordering.** Builders never write the Component Mapping
Registry (`orchestrator/figma/component-mappings.json`) — every mutation is
CAS-guarded (Mapping Review ops, or finalization's `components` phase, which
publishes the binding-authorized mapping for a design-origin component task
after a staged validation compare; generic tasks never auto-map). `ship-done`
calls the final gate driver, which re-runs census under the pinned run id when
a CONSULTED mapping/inventory projection changed. Never run a post-ship
registry mutation; it would make the just-issued evidence certificate stale.

## Step 3.5 — Diff sanity check

After **every** builder reports done — including the fix-cycle builds from Step 4
/ 4.5 / 4.6 / 5 / 5.5 routing — run `git diff --stat HEAD` + `git status
--porcelain`. If empty (or only `.md` changed when the task asked for code), the
builder lied or no-op'd — surface and re-prompt with the explicit acceptance
bullets; do not advance.

**Isolate the task's true footprint** (the tree may carry uncommitted prior-task
work):

```bash
git status --porcelain | sort > /tmp/orchestrator_post_builder_status_${TASK_STEM}.txt
comm -13 <(sort /tmp/orchestrator_pre_task_status_${TASK_STEM}.txt) /tmp/orchestrator_post_builder_status_${TASK_STEM}.txt \
  > /tmp/orchestrator_task_footprint_${TASK_STEM}.txt
```

`…task_footprint_${TASK_STEM}.txt` now lists only this task's entries. When it
equals the whole-tree status (no pre-existing work), the two are identical and
nothing downstream changes. Step 4 uses the footprint to scope the
diff-sensitive validators. Every retry path that re-invokes a builder re-runs
Step 3.5 before Step 4 sees the change.

## Steps 4 → 5.5 (validators, tests, gates, reviewer)

The validator wave (dedup, footprint-scoping, caps), the deterministic test
certification (Step 4.4), the assemble / verify / screenshot gates, the
reviewer routing, and the security gate are covered in
[`validator-routing.md`](validator-routing.md) — read it for Steps 4, 4.4,
4.5, 4.6, 4.6b, 5, and 5.5. The numeric caps for all of them are frozen in
[`orchestrator-loop.md`](../../../contracts/orchestrator-loop.md). Step 4.4
runs exactly once per Step-4 entry; the existing Step-4 outer re-entry cap
(**7** per task) is the only bound on repeat certification after fixes.

## Step 6 — Finalize (same turn)

When every gate is green, post the chat summary, write the Outcome draft, then
invoke the recoverable finalizer — **all in one turn**. The finalizer owns the
todo-file mutation, move, derived artifacts, verification, and lock. The chat
summary block and structured `## Outcome` draft (Steps 6a–6d) are
covered in [`outcome-appendix.md`](outcome-appendix.md).

The happy path ends only after Step 6d. Ending the turn on a gate skill's report
strands the task: work done, but still in `todo/`, lock live, no appendix.

## Escalation triggers

Stop the loop and ask the user when: the task is under-specified (task-intake
BLOCKED); Inputs claims do not resolve (inputs-resolver HIGH BLOCKED); the
planner disagrees with intake; a builder needs a missing prerequisite
(task-intake misclassified — escalate, don't re-plan); a census regression
(Step 1b or a builder report); the screen design cache is missing/stale (Step
1b); an architecture change is required (a finding maps to a stop-point in
`../../validation-gates/references/when-to-stop-and-ask.md`); loop divergence (Step 4 outer
re-entry hits **7**, a finding-set stable across **2** cycles, or an acceptance
bullet missing across **2** cycles); the build is red after a builder's "done";
or no builder maps to the task.

Escalations are short: (1) what you were trying to do, (2) what's blocking, (3)
the smallest decision needed, (4) options (recommended first). Do NOT take
destructive actions while escalated. **Keep the lock while escalated.**

**Publish every decidable question durably.** A turn-ending report is not a
question the owner can answer: the site lights its *live* answer rail only for a
`needs_action`, and that session is reaped after its idle window, leaving the
decision reachable only in the journal. So before you end the turn, write the
escalation into the task body as a `## Questions` section.

1. Run `node orchestrator/tasks/validate-task-state.mjs --stem <STEM> --expect todo --check-index --json`
   and retain its exact `sourceRevision`.
2. Compose the **complete task file** in memory — heading, `## Source`, every
   existing section, and the question blocks appended at the end — and pipe
   exactly those bytes:
   `node orchestrator/tasks/transition-task-state.mjs publish-questions --stem <STEM> --input - --source-revision <exact-sourceRevision>`.
   The helper diffs your bytes against the current file and refuses anything but
   an append of new question blocks; the fence below shows only the appended
   part, never the whole proposal.
3. Require the `ok: true, operation: "publish-questions"` receipt, then re-read
   the file and prove byte equality before you report anything.
4. On `TASK_QUESTIONS_WRITE_INVALID` — or `TASK_ANSWER_PERSISTENCE_INVALID` on
   the persist path — the file is untouched: re-read the current body, rebuild
   the proposal on those exact bytes with a fresh `sourceRevision`, and retry
   once. If it fails again, end the turn with
   `BLOCKED[general]: question publication failed — <helper reason>` and keep the
   lock.

Shape — the same grammar as the task-prep sidecar, one heading level deeper
because a task body already owns every H2:

```markdown
## Questions

### Q<N> — <question text>

- (a) **<Option>** — <one-line trade-off>.
- (b) **<Option>** — <one-line trade-off>.

**Recommended**: (a) — <why>.

**Type**: choice
**Options**: a, b

#### Answer

```

Rules: ids positive, unique and strictly greater than every id already in the
section; exactly one `**Type**` (`text|choice|multiselect`); `**Options**` only
for choice/multiselect with at least two unique values, and every value must be
the `(id)` of one option bullet — the board renders that bullet's text as the
option's label and only accepts ids matching `[A-Za-z0-9_-]{1,40}`; exactly one **empty** `#### Answer` per block; no unterminated
fence, HTML block, or comment anywhere in the body; structural tokens stay
verbatim English. Then emit the journal `stop` event and end the turn with the
same `BLOCKED[<type>]:` report.

The board raises a durable **Waiting for your answer** blocker and the owner
answers in Task details. The answered task comes back to you as an ordinary
`run` whose prompt carries the answered body as data and requires you to persist
it with `persist-task-answers` **before** resuming — the file on disk still
holds empty `#### Answer` bodies until you do. Publish nothing when the
escalation has no decidable question (a red build, a missing runner) — those
stay pure reports.

If the owner instead answers in the still-live session, persist that answer
yourself before continuing: re-validate for a fresh `sourceRevision`, compose
the complete body with only the matching `#### Answer` bodies filled in, and
pipe it through
`node orchestrator/tasks/transition-task-state.mjs persist-task-answers --stem <STEM> --input - --source-revision <exact-sourceRevision>`.
Change nothing else — the helper compares the surrounding bytes exactly. An
unanswered section outlives the session and re-raises the blocker forever.

**Gate-script rule.** Product tasks do not edit template-owned gate scripts
(`orchestrator/figma/scripts/**`). Escalate a suspected gate defect as a separate
owner-authorized template-maintenance task with a regression test.

## Parallelism rules

- **Pre-flight:** `task-intake` → (`inputs-resolver` ‖ `context-finder` × N) →
  `implementation-planner`. The resolver and context-finders run in one
  multi-Agent message; the planner waits on both. Never run `task-intake` or
  `implementation-planner` in parallel with anything else.
- **Builders:** parallel only when their file sets are disjoint (per the plan).
- **Validators:** always parallel (read-only); dedup at the routing layer.
- **External review:** serial — one reviewer per task.
- **Builders + validators:** never parallel — validators read what builders wrote.

## Hard rule — no inline product code

You coordinate. You do not write product code. The closed list of files you may
write directly: the cache-only Outcome draft (Step 6a, incl. `### Execution
log`) and append-only journal lines via `log-event.py`. The escalation
`## Questions` section is the one durable task-body change you may cause, and
only through `transition-task-state.mjs publish-questions` / the owner's
answers through `persist-task-answers` — you still never write the file
yourself. Reopen publication is
owned exclusively by `transition-task-state.mjs`; forward publication happens exclusively via
`node orchestrator/tasks/finalize-task.mjs <stem> --outcome-file <draft>`.
For a direct lease or caller-owned standby lease, append
`--writer-session-id <exact verified receipt sessionId>`; a site turn uses its
already-verified inherited session.
That deterministic tool is the sole writer of the todo Outcome, component-mapping
publication, `done/` move, `INDEX.json`, architecture map, and lock release.
Anything else — `.kt`/`.kts`/`.swift`/`.gradle`/resource/manifest/any
`orchestrator/skills/**/*.md` spec markdown/any committed `.json` — is forbidden
for you to write directly. If a task maps to no builder:
`BLOCKED[task-shape]: no builder maps to this task…` (the user picks
extend/split/reject). No exception for "trivial" code.

## Comment + deprecation discipline

You carry these as coordinator so you can reject builder output that violates
them before it lands in the diff. **Comments:** default none; add one only when
the reader can't infer the WHY, it can't move to the appendix/commit/skill
reference, AND it'll still be true in six months. Forbidden shapes (the
`anti-pattern-scanner` greps for them): file-level prose, step-by-step narration,
task/question/commit-id citations, restated trade-offs (those go in `###
Caveats`), `TODO`/`FIXME`/`XXX`. Escape hatch: a single `// why: <reason>`.
**Deprecation:** builders write the current API; route `build-validator`
deprecation warnings (`w: '...' is deprecated`) on changed files, or reviewer-
flagged deprecated call sites, back to the responsible builder. Don't patch
either yourself — the Hard Rule forbids it.

## Move-back (reopen a done task)

The one path where the orchestrator runs against a `done/` file. Trigger: the
user says "reopen TASK_<N>" / "the outcome is wrong, re-run it" and re-invokes
with `mode=reopen`. The orchestrator MUST NOT initiate this on its own —
re-opening a disputed green completion is the user's explicit action.

Skip product Steps 0–5, but do not skip writer authority or canonical state
fencing:

1. Verify exactly one authority source: the inherited site writer session with
   `writer-lease.mjs verify-session --guard-finalization`; a complete
   caller-owned standby receipt with `writer-lease.mjs verify
   --guard-finalization --lease-id ... --token ... --session-id ... --stem ...`;
   or a newly acquired direct guarded task lease with key `direct:reopen`.
   Never acquire a second lease when the standby receipt is present.
2. Run `node orchestrator/tasks/validate-task-state.mjs --stem "<STEM>"
   --expect done --check-index --json --caller reopen`. Require `ok:true` and
   retain its exact `sourceRevision`.
3. Run `node orchestrator/tasks/transition-task-state.mjs reopen --stem
   "<STEM>" --source-revision "<exact-revision>"
   <authority-flags-if-direct-or-standby>`. Pass exact `--lease-id` and
   `--lease-token` for either bounded authority.
   Never run `mv`, edit/strip the Outcome directly, or regenerate INDEX.
4. Report success only for a parsed v1 `ok:true`, `operation:"reopen"`,
   `state:"todo"` receipt. The helper content-addresses the immutable original
   done body under `tasks/evidence/reopen/<stem>/`, strips the anchored Outcome,
   transactionally publishes todo/removes done, validates the postcondition,
   and publishes a fresh INDEX. On failure it restores the exact done source.
5. Release a direct lease only after the helper returns (also on refusal).
   Never release a caller-owned standby lease; the standby worker releases it
   after this prompt completes. Exit 4 / `TRANSITION_SOURCE_CHANGED` requires a
   fresh validation; never substitute a new revision into an already prepared
   command.

The standard pipeline runs on the **next** explicit "run task" invocation.
