# task-drop — remove an obsolete task from the board

Self-contained two-phase drop procedure. It removes a live task only after the
user explicitly confirms the exact current dependency impact. It never edits
product code, task Markdown, dependencies, or done history.

`<STEM>` is the exact canonical `TASK_<N>_<snake_case>` identity supplied by the
caller. All durable effects are owned by
`orchestrator/tasks/transition-task-state.mjs`; this procedure never runs
`git rm`, `rm` against task/lock/journal files, `mv`, or `regen-index.py`.
An existing task lock is an ownership refusal, not cleanup input: Drop leaves
it byte-for-byte intact and stops until its exact owning flow resumes or
recovers it.

## Hard limits

- `done` is immutable here. `inspect-drop` rejects it; reopening is a separate
  explicit operation.
- Do not infer task state with grep/folder checks. Do not parse dependencies
  independently. The canonical validator is the sole state/graph authority.
- Do not mutate or auto-repair a collision, malformed sidecar, stale INDEX, or
  unsafe runtime artifact. Return the helper's exact bounded findings.
- Never clear or adopt an existing task lock. Age, a stopped UI session, and
  the user's Drop confirmation do not prove ownership of its exact generation.
- Never invoke task-prep, builders, product validators, or sub-agents.

## Step 0 — writer authority

Run from the repository root and verify
`orchestrator/tasks/transition-task-state.mjs` exists.

For a site-started turn, retain the inherited
`ORCHESTRATOR_WRITER_SESSION_ID`; the mutation helper verifies that it is a
single active, attached, non-expiring same-stem session and that finalization
does not own publication. Presence of the environment variable alone is not
authority.

For a direct invocation, acquire one guarded lease before mutation:

```bash
node orchestrator/tasks/writer-lease.mjs acquire --guard-finalization \
  --kind task-session --stem "<STEM>" --key "direct:drop" \
  --owner-pid "$PPID"
```

`--owner-pid "$PPID"` is mandatory: it anchors the lease to the long-lived
session process (the executing shell's parent), not the per-call shell that
dies when the command returns — verification and the cross-process
serial-safety exclusion require a still-live owner identity. Run the command
at the top level of the tool call — never inside `bash -c`, a heredoc, or any
nested shell, where `$PPID` resolves to the dying per-call shell. Release the
lease on every failure path: one leaked by an interrupted turn stays active
while this session process lives and holds every drainer.

Retain its `leaseId` and `token` privately and pass them as
`--lease-id <id> --lease-token <token>`. Release that exact generation on every
return. Never log the token or put it in a task/journal file.

## Step 1 — read-only impact receipt

Run exactly:

```bash
node orchestrator/tasks/transition-task-state.mjs inspect-drop --stem "<STEM>"
```

Require a parsed v1 receipt with `ok:true`, `operation:"inspect-drop"`, the
matching stem, a live `state` (`backlog`, `pending`, or `todo`), an exact
`sourceRevision`, sorted `dependents`, and `impactHash`. The helper validates
the complete task corpus and fresh INDEX. A non-zero exit is a refusal; make no
mutation.

This step is strictly read-only. Show the user the exact state and every
dependent stem before mutation. If `dependents` is non-empty, explain that
those live tasks will remain but their dependency will be unresolved. Do not
edit or drop dependents.

## Step 2 — explicit acknowledgement

The user must explicitly confirm the exact current impact receipt. The
confirmation is bound to `impactHash`, not a generic earlier "drop" click or a
reconstructed list. If the user has not seen and acknowledged that receipt,
stop after Step 1.

Never reuse a previous receipt. Any task/source/dependent change produces a new
revision or impact hash and therefore requires a fresh inspection and
confirmation.

## Step 3 — transactional drop

After acknowledgement, invoke:

```bash
node orchestrator/tasks/transition-task-state.mjs drop \
  --stem "<STEM>" \
  --source-revision "<sourceRevision from Step 1>" \
  --ack-impact "<impactHash from Step 1>" \
  <authority-flags-if-direct>
```

The helper revalidates authority, state, source revision, dependency impact,
lock absence, and INDEX immediately before mutation. It transactionally
detaches the exact live task artifact(s) and same-stem journal, validates
`absent`, publishes INDEX fail-closed, rechecks lock absence, and validates
final equivalence. It never removes a task lock without its immutable owner
receipt. On failure it rolls back owned task/runtime artifacts and never emits
a success receipt.

`TRANSITION_SOURCE_CHANGED` (exit 4) or `DROP_DEPENDENTS_PRESENT` means the
inspection became stale: return to Step 1 and show the new impact. Do not retry
with a substituted revision/hash. Other failures are reported with their exact
finding codes and safe recovery guidance.

## Step 4 — report

Report success only for a parsed v1 receipt with `ok:true`,
`operation:"drop"`, matching stem, and `state:"absent"`. Include its
`dependents` and `runtimeRemoved` fields. No follow-up creation/prep offer is
needed.
