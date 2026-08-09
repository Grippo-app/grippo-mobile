# Tasks

A four-column kanban for tasks the agent pipeline runs. The user authors free-text ideas; `task-prep` turns them into structured work; the orchestrator delivers; the file moves through the columns until it lands in `done/` as the audit trail.

## Host support boundary

The canonical column lifecycle is supported on native Linux and macOS, and on
Windows through WSL (which exposes the required Linux filesystem semantics).
Native Windows is intentionally fail-closed with `PLATFORM_UNSUPPORTED` before
validation or mutation: the current authority depends on POSIX descriptor-
relative traversal, durable directory fsync, and no-clobber link/rename
primitives for which the repository does not yet have an end-to-end Win32
handle implementation. Windows process-identity and Job Object helpers remain
defensive building blocks; they do not by themselves make the durable task
lifecycle native-Windows safe. Do not bypass this boundary with a different
Python executable or direct filesystem writes.

## Lifecycle

```
backlog/  →  task-prep  →  pending/  →  task-prep  →  todo/  →  orchestrator  →  done/
   ↑                          ↑ (user answers)
   └── you write a free-text idea here
```

- **`backlog/`** — free-text ideas you author manually or through the site composer. The site uses a deterministic, idempotent server transaction for the number, file and INDEX; the only Markdown requirement is the first line `# TASK <N> — <title>`.
- **`pending/`** — questions sidecars `task-prep` writes when a backlog item is ambiguous. Up to 7 questions per round; you answer them in `### Answer` blocks via the site form (or by hand) and re-run `task-prep`.
- **`todo/`** — structured tasks ready for the orchestrator. `task-prep` writes the Goal / Inputs / Acceptance / Out of scope shape. The orchestrator runs only files in this folder.
- **`done/`** — immutable audit trail published by the atomic finalizer after a
  successful run. Treat the folder as history.

## File naming

Every task uses the same filename across all four folders:

```
TASK_<N>_<short_title_in_snake_case>.md
```

- `<N>` is **unique across all four folders** — only the deterministic creator scans files, INDEX, reservations and receipts under one kernel mutex, reserves the next number, publishes backlog, and refreshes INDEX. Manual allocation (`max + 1`) and manual INDEX regeneration are unsupported.
- The filename does **not** change across allow-listed lifecycle transitions.
  The canonical transition/finalization helpers keep the stem stable.
- Questions sidecars in `pending/` use the same stem with a `.questions.md` suffix: `TASK_<N>_<title>.questions.md`.

## `.cache/tasks/locks/` — in-progress markers

When the user triggers `task-prep` or `orchestrator`, a runtime ownership
projection is published at `orchestrator/.cache/tasks/locks/<STEM>.json`. The
site reads it to paint the `in progress · <stage>` badge. The lock does not
define or repair the task's durable column; the canonical task-state validator
does that from task files.

```text
orchestrator/.cache/tasks/locks/
└── TASK_3_chart_redesign.json
```

Canonical file contents:

```json
{
  "version": 1,
  "stem": "TASK_3_chart_redesign",
  "stage": "orchestrator",
  "runId": "run-<unique generation>",
  "sessionId": "ws-<writer-session identity>",
  "startedAt": "<exact ISO8601 UTC>",
  "owner": {
    "kind": "site | standby | direct | agent",
    "id": "<bounded owner identity>",
    "pid": 12345,
    "processStartId": "psid-v1:linux:<64 lowercase hex>",
    "hostname": "<host>",
    "startedAt": "<same exact timestamp>"
  }
}
```

`owner.processStartId` is always present; it is an exact platform process-start
identity when the runtime can establish one and `null` only on an unsupported
platform.

`orchestrator/tasks/task-lock.mjs` is the only ordinary lock writer. Do not
compose JSON in a prompt and do not use `printf`, redirection, `mktemp + mv`, or
an unconditional rename over an existing `<STEM>.json`.

```bash
node orchestrator/tasks/task-lock.mjs acquire \
  --stem "$STEM" --stage task-prep

node orchestrator/tasks/task-lock.mjs verify \
  --stem "$STEM" --stage task-prep \
  --run-id "$RUN_ID" --session-id "$SESSION_ID" \
  --expected-hash "$LOCK_HASH"

node orchestrator/tasks/task-lock.mjs release \
  --stem "$STEM" --run-id "$RUN_ID" --session-id "$SESSION_ID" \
  --expected-hash "$LOCK_HASH" \
  --expected-state "$FINAL_STATE" --source-revision "$FINAL_SOURCE_REVISION"

# Only when an earlier exact release detached the lock and left .release-*:
node orchestrator/tasks/task-lock.mjs recover-release \
  --stem "$STEM" --run-id "$RUN_ID" --session-id "$SESSION_ID" \
  --expected-hash "$LOCK_HASH" \
  --expected-state "$FINAL_STATE" --source-revision "$FINAL_SOURCE_REVISION"
```

Acquire first runs fresh canonical action + INDEX validation: `prep` from
`backlog`, `answers` from `pending`, or `run` from `todo` (including the run
dependency gate). Publication is bounded, no-follow and no-clobber: a different
or malformed existing owner is reported and preserved. Immediately after
publication it repeats action validation and compares state + source revision;
drift releases only the exact just-published generation and exits transiently.
A retry is idempotent only with the same explicit `runId`/`sessionId`. The JSON
receipt returns those identities and `lockHash`; the owner must retain them as
private run context.

**Release lifecycle differs per helper:**

- **task-prep** is one-shot. It releases its exact generation on every owned
  exit only through `task-lock.mjs release`, passing the receipt's exact stem,
  run id, session id and lock hash plus the final transition receipt's exact
  `state` and `sourceRevision`. On a non-transitioning `BLOCKED` exit, obtain a
  fresh green state + INDEX receipt first. A wrong identity/hash or stale final
  receipt never deletes the current owner. Release validates before prepare,
  immediately before detach, and again after detach while the private active
  release generation still excludes every new owner. A racing replacement or
  failed final verdict is retained as an explicit recovery artifact. If a
  crash leaves that `.release-*` generation, `recover-release` consumes only
  the same exact lock identity and a fresh green final state receipt; a foreign canonical lock, different inode,
  malformed/multiple quarantine, or wrong tuple preserves every generation
  and fails closed. A successful release/recovery echoes `state`,
  `sourceRevision`, and `snapshotHash`. Repeating a completed recovery is
  idempotent.
- **orchestrator** is long-running. Its recoverable `finalize-task` transaction
  is the only happy-path releaser and removes the lock only after done state,
  INDEX, architecture, registry and ship evidence pass their final
  postconditions. Intermediate `BLOCKED` results keep ownership. A bounded
  same-state todo editor or an explicitly aborted pre-finalization run may use
  ordinary exact release only while the fresh final receipt still proves
  `todo`; ordinary release never accepts `done`, so it cannot bypass finalizer
  cleanup.

**Other notes:**

- **One file per in-flight stem.** A task lives in exactly one column at a time, so there is at most one lock per stem.
- **Stage values:** only `task-prep` or `orchestrator`. A stage impossible for
  the current durable state is an integrity error.
- **Staleness is not ownership loss.** Age is reported separately and never
  authorizes automatic deletion. Prove the owner/session dead or use its guided
  recovery. Clearing a lock cannot repair a task-column collision or turn an
  operation into success.
- **Finalization recovery wins.** Once
  `.cache/tasks/finalizations/<STEM>.json`, its exact pre-WAL
  `.replace-reservation.json`, or its `.replace-wal.json`
  replacement intent exists, use **Resume finalization** or
  `node orchestrator/tasks/finalize-task.mjs <STEM>`. The finalizer compares the
  exact captured lock identity and bytes before its ownership-safe release.
- **Finalize admission is conditional in `done`.** Canonical action
  `finalize` accepts ordinary `todo`; it accepts `done` only when the composite
  runtime snapshot contains exactly one active marker for the stem and that
  marker's captured owned-file generation matches the retained canonical v1
  `orchestrator` lock. Same bytes in a replacement inode do not match. Every
  unrelated runtime/integrity finding remains blocking.
- **Not committed.** `.cache/tasks/locks/` is in `.gitignore` — runtime state only.

The directory is created on demand (`mkdir -p`); a fresh clone has no `.cache/tasks/locks/` until the first run.

## `.cache/tasks/finalizations/` — durable publication recovery

`finalize-task.mjs` owns the complete Outcome → registry → sanctioned ship →
INDEX → architecture → verification → unlock transition. Before every mutation
it atomically advances `<STEM>.json`; immutable
`<STEM>.<snapshot-sha256>.outcome.md` files hold the exact intended task body.
Marker advancement is itself journaled. A private, canonical
`.<STEM>.json.replace-reservation.json` is flushed before the candidate is
created; it owns the exact private name, intended bytes, mode, and source proof.
After the candidate is flushed, `.<STEM>.json.replace-wal.json` adds its exact
generation proof before the old marker can lose its canonical name. A pre-WAL
retry discards only a sole-link, same-device, same-owner candidate whose bytes
and mode match that durable reservation and whose canonical source is still
exact. Before WAL there cannot yet be an inode proof, so the reservation
deliberately owns the exact random private name rather than an inode: a new
same-owner, sole-link inode with the exact intended bytes and no extra mode bits
is treated as an unauthorized claimant to that reserved namespace and discarded,
never adopted or published. Any distinguishable mismatch is preserved and fails
closed. A WAL retry rolls the intent
forward without overwriting a foreign canonical generation, then flushes
private-alias removal before removing the WAL last. The Site exposes either
bounded interval as `marker-replace-recovery`, so even a crash with no
canonical `<STEM>.json` remains a one-click automatic recovery rather than an
apparently absent task. Malformed, orphan, or proof-mismatched private names
remain fail-closed and are never guessed away.
Creating a new marker requires a canonical v1 lock whose stage is exactly
`orchestrator`; any other lock shape or stage cannot authorize
publication. When a live writer lease authorizes the call, its session id must
also equal the lock's session id. Recovery of an existing marker remains bound
to the exact lock bytes and inode frozen when that marker was created.
Content-addressing keeps the previous recovery snapshot valid if a process dies
between publishing refreshed Outcome bytes and advancing the marker. Re-running
the same command reconciles physical postconditions, so a
crash after the move does not run the AI task again. Successful cleanup removes
the task lock, all transaction snapshots, and marker in that order (marker
last). `ship-done.mjs` accepts only the active marker's transaction/owner and
publishes an immutable transaction-owned task into `done/` with no-clobber
semantics. Figma receipts are staged under the marker directory and reach their
committed location only after that publication wins; a losing `EEXIST` attempt
cannot overwrite another ship's receipts. Todo removal uses a private rename
proof; lock release uses a no-clobber hard-link witness plus a private randomized
detach path, so a crash is distinguishable from an external deletion and a
concurrent replacement is preserved instead of unlinked. All private proofs
are removed only after the completed marker has reverified the exact done
bytes. Runtime proofs under `.cache/` are never committed.

Workspace writers and the finalizer use a two-sided lease handshake under
`.cache/tasks/finalizations/.writers/`. A Claude/Figma writer publishes a
unique lease **before** re-checking marker/mutex state; `finalize-task` acquires
the global task dependency-graph/publication mutex first and then scans those
leases. Thus the side
that arrived first is always visible to the other before either mutates. The
only exception is exactly one same-stem `task-session` lease whose random
session ID is passed to `finalize-task`; an `unverified` lease can never use
that exception. It is the owner Claude turn and remains held until that turn's
`result`, so no foreign writer is admitted. `writer-leases.cjs` is the shared
contract; `writer-lease.mjs` is its narrow acquire/verify/renew/release CLI for
the standby queue worker and direct skills. Direct skills acquire with
`--guard-finalization`: the CLI durably publishes the lease first, then performs
the mandatory fail-closed marker/stable-mutex re-check. A busy guard withdraws
the owned lease, writes no success JSON, and exits `2`; callers must stop before
their first workspace mutation. On POSIX, detached Claude PGIDs are also probed,
so a surviving tool descendant keeps the lease active after its session leader
or site process dies. Site-started skills do not trust an inherited session ID:
`writer-lease.mjs verify-session --guard-finalization` requires exactly one
active, verified, non-expiring, child-attached, same-stem site `task-session`
lease before each turn's first mutation and otherwise fails closed without
rewriting the lease. In particular, a bounded direct lease can never be
authorized by an exported/stale environment value and then expire between the
check and mutation. A standby worker instead passes its complete private bounded
receipt to the selected skill; `writer-lease.mjs verify --lease-id … --token …
--session-id … --stem … --guard-finalization` authenticates that exact active
generation read-only. The skill reuses its session/transition authority and may
renew it, but never acquires a second lease or releases the caller-owned one;
the standby worker performs the single final release. An unverified lease with
no bound child remains fail-closed
because no process-tree identity exists. After restart on POSIX, an unverified
detached Claude session lease with a durably bound child is automatically
treated as stale only when the local owner, child leader, and the child's
original process group are all proven absent; other writer kinds, remote rows,
and Windows tombstones remain fail-closed without equivalent proof. Bounded direct/standby
leases keep a one-hour crash limit; their owner refreshes that bound with
`writer-lease.mjs renew --lease-id …
--token …` before each long phase. Renewal is token-checked and refuses an
already expired or `unverified` generation, which must be replaced through a
fresh guarded acquisition instead of being revived across a possible race.
On Linux each new local owner/child binding also records a digest of the boot ID
and `/proc/<pid>/stat` start ticks; on macOS it records the boot-session UUID and
`proc_pidinfo` start seconds/microseconds. On Windows the isolated native helper
binds `OpenProcess`/`GetProcessTimes` creation ticks to the kernel boot GUID.
Liveness requires the PID and that start identity to match, so numeric PID reuse
or a later reboot cannot resurrect a crashed owner, child or publication guard.
On platforms without a start-generation primitive, PID-only liveness remains
conservative and nested site delegation is unavailable unless an equivalent
exact start/ancestry proof exists.

Lease storage is fenced below an explicit authority root. Node and Python walk
each lexical directory component with `lstat`, create one component at a time,
and recheck captured directory identities; a pre-existing symlink anywhere
below that root fails before publication. Lease renew/attach/retain/release
operations also take a per-generation atomic mutation lock, detach the exact
inode into a uniquely named recovery link, revalidate its bytes/generation,
and publish or restore with no-clobber semantics. Directory fsync ordering
keeps at least the old recovery link or the new canonical link durable; any
retained lock/recovery artifact blocks scans. Node does not expose portable
`openat`/`mkdirat`, so this component fence is not an absolute guarantee
against a malicious same-user process swapping an ancestor to a symlink and
back inside a single syscall window; deployments that include that adversary
must isolate the cache directory at the OS/container permission boundary.
On macOS or Windows, `ORCHESTRATOR_WRITER_PYTHON` may select an absolute Python
3 runtime for native identity proof. Safe Homebrew, virtualenv and system
locations are tried on macOS; Windows pins the first safe absolute `python.exe`
reported by the fixed system `where.exe` when the setting is absent. The helper
runs with isolated imports and identity verification remains fail-closed if the
runtime or any native proof is unavailable.

Site-initiated recovery on Windows is launched through `windows-job.py`. It
creates the finalizer suspended, assigns it to a kill-on-close Windows Job
Object, and only then resumes it. The server retains recovery ownership until a
nonce-authenticated wrapper message confirms the Job's `ActiveProcesses` count
reached zero; wrapper exit or `taskkill` alone is deliberately not accepted as
proof.

The owner record in `finalizations/.mutex.json` is only a conservative external
busy signal; mutation authority is the kernel-held mutex plus the exact
invocation and process-start identity. Node lifecycle transitions retain the
unreaped helper `ChildProcess`; Python create/edit publishers hold the same
inode directly. A crash releases the kernel lock, while the dead receipt is
overwritten only by the next process after it acquires that same canonical
inode. The record can refuse work but can never authorize mutation, release,
or deletion by itself.

## `.cache/tasks/creations/` and `.cache/tasks/edits/` — backlog transactions

`create-backlog.py` owns task identity, no-clobber backlog publication and
canonical INDEX regeneration. A client idempotency key maps to one exact durable
receipt under `creations/`; incomplete receipts retain the bounded canonical
intent, including the server-resolved Source envelope, so startup can resume
without the browser request, while completed
receipts shed the body and remain replay/conflict history.
The low-level create request must contain exact `version: 1` and `source`
fields. Public/manual, design, API, and task-split producers each construct
that Source through `task-source-contract.cjs` before invoking the creator;
`create-backlog.py` validates it and never infers provenance from the key or
parent stem.

`edit-backlog.py` applies a source-hash compare-and-swap to an idle backlog task.
It writes `<stem>.json` under `edits/` before replacing Markdown, then completes
forward and verifies INDEX. Incomplete edit receipts retain canonical base64
recovery bytes; completed receipts shed them. Create and edit first serialize
through `creations/.mutex`, then take the shared terminal-inner
`finalizations/.mutex.json` dependency-graph fence for validation, task bytes,
INDEX publication, postconditions, and recovery. `transition-task-state.mjs`
takes that same fence after its per-stem guard; finalization already holds it
for its complete transaction. No path takes the graph fence before a local
mutex/guard, so the lock graph has no reverse edge. `regen-index.py` additionally
serializes scan→publish with `index.lock`. Site startup uses one
ordered authority (create first, edit second) for both valid incomplete WAL
sets. Corrupt markers or task bytes matching neither side of the recorded CAS
remain intentionally fail-closed and are surfaced on the Board instead of
guessed.

The fence makes Drop impact a serializable full-corpus snapshot: every live
dependency publisher that linearizes first is included and requires the fresh
impact acknowledgement; a publisher that waits and linearizes after Drop sees
the stem as absent and emits the canonical `DEPENDENCY_UNRESOLVED` warning.
That later warning is not retroactively part of the completed Drop impact and
does not create a permanent tombstone policy for backlog references.

Conditional replacement uses a crash-recoverable private directory named
`.durable-cas-<target-digest>-<owner-digest>-<generation>` beside the file being replaced.
Such directories may therefore appear in `backlog/`, `creations/`, or `edits/`.
`durable-cas-contract.cjs` owns the exact name, artifact-set, canonical manifest
and lossless source-proof vocabulary shared by the Python writers and the Node
integrity readers. An existing canonical operation always means
**recovery required** until its create/edit owner reconciles and removes that
exact generation; age never authorizes cleanup. A prefix lookalike, symlink,
special entry, unexpected artifact, non-canonical manifest, changed candidate
or detached-source hash, or an operation beyond the eight-entry bound is a
fail-closed blocker. Do not edit or remove private CAS files manually.

A task-prep Figma split reuses its exact authenticated parent task session for
authorization, but a task-scoped lease alone is not global allocator/INDEX
exclusion. Before its second writer scan, `create-backlog.py` therefore asks
`writer-lease.mjs acquire-publication-guard` to publish a supplemental
`lock-writer` with deterministic key `task:create-backlog`. Admission permits
exactly that guard and one matching parent generation. A site child receives
its exact lease ID and a one-time delegation capability in its private
environment; only the capability hash is durable. Guard acquisition requires
that receipt, exact process-start identities, and a bounded revalidated ancestry
chain from the creator to the recorded site child. A copied public
stem/sessionId, even from a live sibling process, is therefore insufficient.
Bounded standby/direct parents instead present their complete
id/token/session/stem receipt. The successful caller receives a new guard
delegation token while its lease stores only the hash; `create-backlog.py`
rechecks that token, caller generation and parent metadata before mutation.
Every later writer sees the deterministic key and withdraws; the creator
releases only the supplemental guard by its private ownership token. This never
creates a second Claude or task session.

The separate shallow intake stores only advisory, source-hash-bound JSON under
`.cache/tasks/intake/`. It has no tools or repository cwd and never changes a
task column, readiness authority, task Markdown or INDEX.
Its global worker and per-stem owners carry exact process-start identities. The
global record first binds the wrapper generation, then binds the direct model
generation through a nonce-authenticated wrapper-only fd; the POSIX wrapper
accepts a `BOUND` acknowledgement only after the Site has committed that second
identity with an exact CAS. The model inherits neither the fd nor its nonce.
Linux executes the already-opened model fd. macOS instead double-verifies one
bounded source-fd image, fsyncs it as the sole private `.model-executable` in
the request scratch, and gives only that pinned path to the root-owned no-fork
sandbox; normal drain removes the exact copied inode, while crash recovery
recognizes that one reserved name without deleting unknown scratch content.
`spawnState` advances from `not-started` to `started` in the same exact CAS that
binds the wrapper and always before `GO`; this makes only the former null/null
generation safe for automatic recovery.
Wrapper death is therefore never model-death proof: a wrapper-bound/model-null
record remains fail-closed, and a fully bound record remains owned while either
exact generation or its containment group can still be live. Restart recovery
never treats age as authority. Normally the bounded model wrapper enforces its
own timeout and recovery only waits for exact absence. The Darwin double-crash
case is the narrow exception: only an unchanged private canonical worker inode
with exact dead site + wrapper generations, an exact live model start identity,
and that model in the wrapper's isolated PGID authorizes bounded TERM→KILL; the
record and all identities are re-proved immediately before each signal. Any
reuse, replacement, remote host, or ambiguous proof retains the owner without
signalling. Recovery then removes only the exact byte-and-stat owner generation.
Owner release and stale
recovery use the shared file-guard transaction engine: deterministic target-keyed
WAL, exact compare-and-detach, durable decision/receipt settlement, and bounded
reconciliation before every later publication. A lost response can recognize
only its own committed WAL; a same-bytes replacement inode is retained.
Normal release uses the same primitive, so every publication/delete crash
boundary rolls forward automatically without age guesses or a second owner.
A live, remote, malformed, foreign, or unverifiable generation remains
fail-closed; a reused PID generation is treated as the recorded owner being
gone, never as the new process inheriting that ownership. Unknown runtime
artifacts are preserved for diagnosis and never receive mutation authority.
On Windows, `intake-windows-job.py` owns the model process in a kill-on-close
Job Object and releases the durable global intake slot only after a
nonce-authenticated `ActiveProcesses == 0` drain proof; wrapper exit alone is
never treated as descendant death.

## `.cache/tasks/journal/` — per-task pipeline event log

While a task moves through the pipeline, the helpers append structured events to `orchestrator/.cache/tasks/journal/<STEM>.jsonl` (one JSON object per line) via the shared writer `orchestrator/tasks/log-event.py`. The site board's task-detail **History** tab renders these as a per-phase timeline — phases reached, how long each took, every stop (with reason) and retry, when design was pulled, and which tasks were split off — nested under the task's lifecycle columns.

```
orchestrator/.cache/tasks/journal/
└── TASK_3_chart_redesign.jsonl   # {"ts","stem","kind","phase","status",...} per line
```

- **Who writes it.** Only the *parent* `orchestrator` / `task-prep` session emits (never the parallel-spawned sub-agents — the parent serialises between phases, so it is the single writer that owns the lifecycle narrative). The `figma:screens` pull session emits one `design-pulled` event. `task-journal-contract.cjs` is the shared machine schema; `log-event.py` is its CLI, `task-journal.mjs` owns the rooted append, and `orchestrator/site/server/tasks-log.js` owns the bounded public projection. See the `task-prep` skill's `references/prep-flow.md` "Journal discipline" and the `task-orchestrator` skill (`references/run-loop.md`).
- **Keyed by stem**, like `.cache/tasks/locks/` — the journal stays at the same JSONL path while the task markdown moves `backlog → pending → todo → done` under a stable stem.
- **Best-effort and bounded.** Every write is `|| true`; a logging hiccup never fails a step. Unknown schema fields are dropped, the append rejects symlink/hardlink redirection, and the site reads only a bounded canonical tail; omitted/corrupt history is explicitly marked truncated.
- **Structured review attempts.** A review `phase-start` records
  `reviewer=codex|internal-reviewer`, a canonical positive decimal
  `reviewAttempt`, and `selectionReason=codex-available|codex-unavailable|forced-codex|forced-internal`.
  Terminal review events repeat `reviewer` + `reviewAttempt` and may carry a
  bounded `reasonCode`. This lets the Reviewer screen preserve the reviewer
  selected at attempt start even if detector status changes later. Review
  lifecycle events without the structured pair are rejected rather than
  matched heuristically. The same contract also reserves bounded
  `checkpointId`, `reportId`, and `retryPolicy`; `task-journal-contract.cjs`
  remains the single registry and caps an event at eight metadata entries. An
  informational review `gate` immediately before selection represents “waiting
  for review to start” and is superseded by the next `phase-start`.
- **Shared Codex readiness.** `node orchestrator/tasks/reviewer-status.cjs`
  reports installed vs available without network access or a trial review. It
  consumes Claude Code's active installed-plugin inventory and the active
  official plugin's bounded `setup --json` readiness contract; retained
  plugin-cache directories are never accepted as installations. Both the task
  pipeline and Site consume this contract; ad-hoc directory or PATH greps are
  not policy.
- **Lifecycle.** `transition-task-state.mjs drop` transactionally removes a
  dropped task's journal, but refuses any existing task lock because Drop does
  not own its immutable release receipt. The *permanent* digest of a shipped
  task is the `### Execution log` block in its `done/` `## Outcome` appendix
  (committed), so the run's shape survives even though
  `.cache/tasks/journal/` is gitignored.
- **Not committed.** `.cache/tasks/journal/` is in `.gitignore` — runtime state only; created on demand, absent in a fresh clone.

## INDEX.json — source of truth for the site board

`orchestrator/tasks/INDEX.json` is published only inside deterministic lifecycle
transactions: backlog create/edit, `transition-task-state.mjs` for
ask/promote/reopen/drop, and `finalize-task.mjs` for completion. Skills and
prompts never regenerate it independently. The site's kanban panel reads this
file. Shape:

```jsonc
{
  "version": 2,
  "generatedAt": "<ISO8601>",
  "backlog": [
    {"stem": "TASK_1_dark_mode", "title": "Dark mode toggle", "state": "backlog",
     "sourceRevision": "sha256:<64 hex>", "createdAt": "<ISO8601>", "doneAt": null,
     "origin": {"kind": "manual", "type": "manual", "ref": "intent-…", "fingerprint": "sha256:<64 hex>"},
     "dependsOn": [], "splitFrom": null, "outcomeStatus": null, "questionsCount": null, "round": null}
  ],
  "pending": [
    {"stem": "TASK_2_avatars", "title": "User avatars", "state": "pending", "sourceRevision": "sha256:<64 hex>",
     "createdAt": "<ISO8601>", "doneAt": null, "origin": {"kind": "figma", "type": "design-finding", "ref": "…", "fingerprint": "sha256:<64 hex>"},
     "dependsOn": [], "splitFrom": null, "outcomeStatus": null, "questionsCount": 4, "round": 1}
  ],
  "todo": [],
  "done": []
}
```

A task appears in **exactly one** column at a time. Every row carries the
same complete field set; non-applicable values are `null` or `[]`. `origin` is
the canonical mandatory `## Source` projection. Missing or malformed Source is
an integrity error and is never inferred. Readers and the publisher accept only
the current version-2 schema.

The generation algorithm scans all four folders, parses the title from the `# TASK <N> — <title>` first line, counts canonical `## Q<N> — ` / `## Q<N> - ` questions, reads pending counters, dependencies, lineage and Source, and validates the complete done Outcome contract before deriving `outcomeStatus`. `task-state-core.cjs` is the sole derivation/validation authority; `task-index.mjs` owns fenced publication and `regen-index.py` is its CLI entrypoint. Lifecycle helpers invoke that entrypoint only inside their durable mutation/recovery transaction. The browser reads the bounded `/api/tasks/summary` projection; it does not recreate dependency, blocker or primary-action rules. `test-regen-index.sh` guards core semantic parity, including contract-owned status, reviewer and acceptance-verdict sets.

> `createdAt` / `doneAt` use the file's local mtime, which `git checkout` resets to the checkout time. Treat these as "when this clone last saw the file change", not "when the task originally shipped" — for canonical history use `git log`. Only `TASK_<N>_*.md` files (and `TASK_<N>_*.questions.md` sidecars in pending) are indexed; stray `notes.md` or untyped markdown is ignored.

## Canonical validation scope and read budget

`validate-task-state.mjs --stem <STEM>` is the admission hot path. It first
inventories task-like **names** across the root and every lifecycle column, so
invalid placement, Unicode/case shadows, number collisions and cross-column
presence remain global blockers. It then reads and parses only:

- the selected task and its transitive `## Depends on` closure;
- `INDEX.json` when `--check-index` is requested, comparing only rows for that
  closure while still rejecting globally invalid/duplicate index identities;
- the selected task's lock generation (plus global lock filename inventory)
  when runtime validation is enabled.

Unrelated task body bytes and unrelated valid lock bodies are intentionally not
part of a single-stem verdict or `snapshotHash`. Adding/removing/renaming an
artifact still changes the global directory inventory. `--all` performs full
body and full INDEX equivalence; `drop` also performs a full body scan because
reverse dependents cannot be proven from an outgoing dependency closure.

The JSON `stats` expose deterministic `scanMode`, `inventoryEntries`,
`taskRelatedEntries`, `taskBodyReads`, and `taskBodyBytes` values for regression budgets. Wall-clock
`durationMs` is observational only. The CLI emits a bounded stderr observation;
`TASK_STATE_SLOW_MS` (default `100`, range `0..60000`) controls its `slow` flag
without changing findings, task `ok`, `sourceRevision`, or `snapshotHash`.
Every lifecycle caller emits the same one-line `[task-state]` v1 event for each
actual validation invocation: server admission, deterministic create/edit,
task-prep lock/transition, runner/finalizer, drop/reopen, and manual CLI. The
event contains only caller/mode, stem scope, expected/observed state,
transition/phase, snapshot hash, bounded timing/read counts, final result, and
finding `{code,severity}` pairs. Task bodies, answers, prompts, finding prose,
absolute paths, credentials, and runtime authority tokens are never projected.
Python and JavaScript parent helpers parse and re-project child events instead
of forwarding arbitrary child stderr. Canonical INDEX checking emits two observations
(`pre`, `post`) and publication emits six (`pre` × 3, `post` × 3); a successful
child with a missing, malformed, or wrong-count event stream is replaced by the
same number of synthetic fail-closed observations.
Safe reads bind the opened descriptor to the pathname before and after reading;
an atomic replacement exits `4` as `SNAPSHOT_RACE`, never as a mixed green
snapshot.

The core runtime slice is deliberately the canonical lock projection only.
Requests, reservations, sessions, shallow-intake records, finalization markers,
transition recovery and writer leases retain their owning bounded validators;
the site runtime-integrity composite is the handoff boundary that must merge
those owner verdicts rather than re-parse their schemas in this core. The
handoff receives an immutable exact set of project, task, runtime and authority
roots derived from the validation `repoRoot` plus explicit environment
overrides; the composite rejects any mismatch with its already loaded owner
modules. This prevents a fixture or alternate project from reading or hashing
the canonical workspace runtime by accident.
The composite contributes a synthetic `composite/verdict` snapshot input that
hashes its normalized owner contexts, combined-mode flag, lease inspection,
bounded statuses, and finding code/severity set. Consequently two scans over
identical files but different runtime authority/liveness evidence cannot share
the same `snapshotHash`; raw tokens, external scratch paths, and private
context values remain hash-only and never appear in the public result.

Durable CAS ownership is the exception to the `includeRuntime` switch: the core
always inventories the exact CAS prefix in `backlog/`, `creations/`, and
`edits/`, because a hidden conditional publication can invalidate a task action
even when other runtime diagnostics were intentionally omitted. The scan is
bounded and no-follow, validates through `durable-cas-contract.cjs`, and adds
every operation name and artifact byte/metadata input to `snapshotHash`.
Scoped admission cannot hide an operation for its stem or an operation whose
target cannot be attributed safely; only the owning recovery flow can make the
verdict green.

## `content-snapshot.cjs` — canonical byte-content snapshot primitive

The one frozen engine (v1, hash domain `content-snapshot-v1\0`) that turns an
explicit file list under an explicit root into an immutable content-addressed
manifest (`captureSnapshot` / `validateManifest` / `verifySnapshot`).
Downstream evidence pipelines (the mandatory-test certification track) consume
this module for their own domain-separated manifests instead of inventing a
local hash engine, diffing `git status` sets, or trusting filesystem metadata.
Every entry hash is computed over file bytes read through an
`O_NOFOLLOW` descriptor; symlinks (leaf or ancestor), hardlinks,
FIFOs, traversal and oversize inputs are rejected fail-closed, and a file
replaced mid-read is a race error — there is deliberately no metadata-only
mode. Its aggregate `snapshotHash` is never comparable with hashes from other
domains (`task-state-snapshot-v1`, `project-source-revision`, raw byte
sha256). Self-tests: `tests/test-content-snapshot.mjs` (leaf suite `tasks`).

## `task-test-foundation.mjs` — test-foundation doctor + bootstrap coordinator

Phase-2 primitives of the mandatory-test pipeline (not yet routed into the
task lifecycle). `doctor --product-root <root>` statically inspects a
generated product (never the template, never via Gradle) and returns exactly
one typed state — `READY`, `ABSENT_CAN_INSTALL`, `PARTIAL_CORRUPT`,
`CONFLICTING_STACK`, `UNSUPPORTED_VERSION`, `TOOLCHAIN_UNAVAILABLE` — plus a
content-hashed inventory (`test-foundation-inventory\0`). The bootstrap
coordinator (`claim`/`advance`/`inspect`) owns the durable no-clobber marker
`.cache/tasks/test-foundation/<intent-hex64>.json` with bounded phases
`claimed → child-created → child-promoted → ready`: one global intent
(`test-foundation-intent\0` over policy + inventory hashes) can have at
most one live installer; a rival claim gets a typed `FOUNDATION_IN_PROGRESS`
with the exact owner, never adoption. `task-test-foundation-contract.cjs`
owns shapes/domains/transitions; markers are symlink/hardlink/tamper-proof
fail-closed (O_EXCL create, O_NOFOLLOW reads, nlink=1, marker hash).
Tests: `tests/test-test-policy-contract.mjs` (leaf suite `tasks`).

## Test certification runtime — trusted caller, producers, receipts, aggregate

Deterministic evidence boundary of the mandatory-test pipeline. One canonical
owner per concern:

- `resolve-test-impact.mjs` + `task-test-impact-contract.cjs` +
  `test-impact.schema.json` — the planner PROPOSES, the resolver applies the
  machine policy and may only widen (lanes from change-kind minimums, reverse
  consumer closure, escalation/unknown-dependency → full suite); observed
  never narrows planned (`checkWidening`), and every artifact is hash-bound in
  the `test-impact\0` domain.
- `run-test-certification-request.mjs` — the canonical orchestrator CLI:
  `node orchestrator/tasks/run-test-certification-request.mjs --request <relative-json>`.
  It verifies the exact canonical task-lock identity/hash, recomputes the
  domain-separated pre-Outcome task hash from the current canonical todo,
  safely reads the source/impact/inventory artifacts, proves planned→observed
  widening, and accepts only inventory task paths plus policy structural gate
  ids. The request has no executable/tool, receipt, verdict or summary field.
- `task-test-input-contract.cjs` — the single `test-task-input` adapter over
  the shared structural Outcome parser. Adding/replacing the anchored Outcome
  preserves the hash; changing canonical pre-Outcome task content does not.
- `run-test-certification.mjs` — the allowlisted command and structural-gate
  receipt producer: exact Gradle task
  paths against a caller allowlist (argv array, task text can never become
  shell), sanitized env allowlist + hashed-value fingerprint, detached process
  group with SIGTERM→SIGKILL escalation and orphan sweep, sealed report
  ingestion (O_NOFOLLOW, nlink=1, bounded, TOCTOU re-hash), redaction before
  persistence and immutable started→terminal receipts. Report inputs carry an
  explicit JUnit/Kotlin-Native/Android-connected/Roborazzi adapter; unknown
  adapters fail closed. Structural tools are selected from a closed gate-id
  table, never from the request.
- `aggregate-test-certification.mjs` — discovers every current run receipt,
  revalidates its content-addressed filename, started→terminal pair, context
  hashes and sealed output/report bytes, rechecks source freshness and derives
  every summary field. This is the only summary writer; caller-supplied PASS
  documents cannot be represented. Publication is no-clobber at
  `.cache/tasks/test-certification/<stem>/<run-id>/summary.json`.
- `task-test-receipt-contract.cjs` + `test-command-receipt.schema.json` +
  `test-structural-receipt.schema.json` — two strict receipt kinds
  (`test-command-receipt\0` / `test-structural-receipt\0`);
  `evaluateCommandReceipt` is the single fail-closed reading (zero-discovered,
  NO-SOURCE-with-required-tests, all-skipped, cache substitution on the signed
  `certification-direct` tier, timeout, flaky fail→pass = typed violations). A structural receipt has
  no test-count surface by construction.
- `task-test-summary-contract.cjs` + `test-summary.schema.json` — the sealed
  aggregate (`test-summary\0`); the verdict grammar makes an inconsistent
  green inexpressible (PASS requires current snapshot + every lane/suite/
  anchor; SKIPPED is a proven typed N/A with structural receipts only).
- `task-receipt-registry.cjs` — the ONE typed receipt-kind dispatcher closing
  the checkpoint `receiptVerifier` seam; ids are `<kind>:<receiptHash>` and a
  forged/re-keyed/non-terminal receipt never verifies. Summary verification
  reconstructs policy/source/planned/observed context plus every exact
  started→terminal receipt; a standalone self-hashed PASS is never evidence.
  Foreign receipt families stay typed refusals for their own owners.
- `finalize-task.mjs` — before publishing a new transaction marker, discovers
  the summary from the active lock run, recomputes task/source freshness, and
  freezes task/source/policy/summary/run/session/lock hashes as one all-or-none
  binding. Recovery repeats the graph and freshness checks before unlock.

Tests: `tests/test-test-impact-resolver.mjs`, `tests/test-test-executor.mjs`,
`tests/test-test-evidence.mjs` (leaf suite `tasks`).

## `regen-arch.py` — the derived architecture map

A sibling generator lives here too. `regen-arch.py` rebuilds the canonical
Architecture Map v2 at `orchestrator/.arch-map.json`. The map is a deterministic
structural index of the **bootstrapped product**: typed modules, features,
screens, components, repositories/data sources, API consumers, database
entities, evidence-backed relations, analysis coverage and open findings.
`orchestrator/contracts/architecture-map.schema.json` defines the closed
envelope and `architecture_analysis.py` enforces the semantic contract
(ordering, cross-references, ownership cardinality, fingerprints, summary and
structural hash). `orchestrator/architecture-rules.json` is validated against
the non-executable rules v1 schema.

The generated revision is derived from one bounded canonical input receipt,
not mtimes or VCS state. Reads are no-follow and reject symlinks, hardlinks,
unknown roots, collisions and aggregate caps. A generation first validates and
re-proves that receipt, writes one compact immutable diff v2 with complete
change totals, bounded identifier lists and an explicit truncation state, atomically replaces
the canonical map, then publishes latest/task pointers and a bounded history
index. Manual Site jobs additionally prove their exact shared writer lease in
an independent child-side fence immediately before history and map
publication; task finalization already runs inside its global transaction.

`finalize-task.mjs` invokes the generator with exact transaction id and task
stem after INDEX publication. Manual Site jobs use `manual-refresh` and their
typed job id; they never accept a browser-supplied command or path.
`regen-arch.py --check` is the freshness gate used by `lint.sh`;
`--check-json` and `--revision-json` expose the same bounded comparison as
machine-readable contracts. `validate-task-state.mjs --all --check-arch`
reports it under `derivedState.arch`; task-column validity remains independent,
while an explicitly requested stale map makes `overallOk` and the CLI exit red.
Both read-only modes return `absent` before bootstrap (no
`settings.gradle.kts`).

Every Architecture reader accepts only the canonical v2 map. Unsupported
shapes fail closed and require regeneration; there are no alternate
converters, payload projections or duplicate endpoints. Regression coverage is
in `test-regen-arch.sh`,
`site/tests/architecture.test.mjs`,
`site/tests/architecture-lease-verifier.test.mjs` and
`site/tests/architecture-ui-contract.test.mjs`.

## Backlog shape

Every deterministic creator writes one canonical Source block immediately after
the H1. The browser/user body never supplies raw Source Markdown; the server or
creator resolves a typed envelope and injects it. The minimum newly-created
file is therefore:

```markdown
# TASK 7 — Add a dark-mode toggle

## Source

- Kind: manual
- Type: manual
- Ref: intent-<stable creation intent id>
- Fingerprint: sha256:<64 lowercase hex>
```

The Source field order and blank lines are exact; duplicate/extra fields or
prose are invalid. Allowed Kind/Type pairs are registry-owned by
`task-source-contract.cjs`: Manual (`manual`, `architecture-finding`), Figma
(`design-finding`, `figma-drift`, `figma-missing-component`,
`figma-component-split`), API (`api-missing`, `api-change`, `api-mismatch`,
`api-work-package`) and
Follow-up (`outcome-follow-up`, `reviewer-follow-up`, `task-split`). Source
describes why the task exists; `## Origin` remains separate structural lineage.
It is immutable across ask/promote/edit/reopen/finalize. Missing or malformed
provenance blocks the task until its canonical Source is repaired explicitly.
For Source Type `api-work-package`, the task must also contain one canonical
`## API Work Package` JSON section immediately after Source whose package id
matches `Source.ref`. Its sorted source aliases are validated by the shared
task contract and the complete section is preserved byte-for-byte through
lifecycle body changes. Project → API treats checked findings as the exact
requested scope: it never adds related findings implicitly. Uncovered findings
are grouped by their current semantic API group and produce exactly one package
per group; the planner never partitions one selected group into several tasks.
If a group cannot fit the canonical task-file envelope, preview fails closed and
the user must explicitly narrow that group. Covered findings remain linked to
their existing task, while a one-finding group requires the explicit hotfix
action.

Everything after Source is optional in backlog. Add as much or as little context as you want — `task-prep` reads the prose and asks clarifying questions for anything ambiguous. Examples of useful free-text bullets the agent will pick up:

- "Lives on the profile screen, next to language."
- "Should respect the system dark-mode by default."
- "No-op if dark-mode colors aren't in the design system yet."

## Pending shape (questions sidecar)

`task-prep` writes this file when a backlog item needs clarification. Shape:

```markdown
---
forTask: TASK_<N>_<title>
createdAt: <ISO8601 — first round's timestamp; never changes>
updatedAt: <ISO8601 — this round's write>
round: <N>
gapCount: <total gaps found this round (the logical carry-forward total, can exceed 7), NOT the count of `## Q<N>` headings rendered — the 7-question cap is a render/UX limit only>
prevGapCount: <previous round's gapCount; omitted on the first round>
---

## Q1 — <question text>
**Type**: text | choice | multiselect
**Options**: a, b, c

### Answer


## Q2 — <question text>
**Type**: text

### Answer

```

Fill answers between `### Answer` and the next `## Q` (or EOF). The site form parses this; you can also edit by hand.

Re-run `task-prep` on the same stem and the loop iterates:

- Each answer is classified as **clear** (resolved), **partial** (answers only part — re-asked with refinement), **off-target** (answered something different — re-asked with the original phrasing quoted back), **empty** (re-asked verbatim), or **SKIP** (permanently waived — the literal token `SKIP` stops re-asking).
- Resolved questions drop. Unresolved/refined questions carry forward (same `Q` number, refined wording). New gaps that the fresh answers expose are added.
- The cap is **convergence**, not round count: as long as the gap count strictly shrinks each round, iteration continues. When the count stops shrinking for 2 consecutive rounds, `task-prep` escalates ("the task may be structurally too big — split, rewrite, or drop"). A soft warning appears at round 5+, but only the convergence check is a hard stop.
- The 7-question cap is **per round** (a UX cap to keep the form digestible), not a total cap — the loop can ask 30 questions across 5 rounds if the task needs it.

The `**Options**:` line is omitted entirely for `text` questions. `gapCount` and `prevGapCount` are bookkeeping for the convergence cap and are ignored by the site form.

## Questions inside a running task

A task that is already in `todo/` cannot own a `pending/` sidecar — the pair
classifies as `corrupt` — so an orchestrator escalation publishes its question
into the task body as one `## Questions` section, one heading level deeper than
the sidecar:

```markdown
## Questions

### Q<N> — <question text>

- (a) **<Option>** — <one-line trade-off>.
- (b) **<Option>** — <one-line trade-off>.

**Recommended**: (a) — <why>.

**Type**: text | choice | multiselect
**Options**: a, b

#### Answer

<answer text; empty until the owner answers>
```

Exactly one section per task body, at most 99 blocks. Ids are positive, unique
and strictly increasing; exactly one `**Type**`; `**Options**` only for
choice/multiselect with at least two unique values; exactly one `#### Answer`
per block. The board reads it as the same questions form the pending rail uses,
and the answered section stays in the body — through `done/` — as the durable
record of the decision.

Two write intents own it, both riding the `edit` transition and both validated
before any mutation:

- `transition-task-state.mjs publish-questions` — may only append new blocks.
  Every byte outside the section, the section preamble, and every existing
  block including its recorded answer stay identical (the last existing block
  may grow only the newlines that separate it from the new one). A newly
  published block must carry an empty `#### Answer`, and its id must be greater
  than every existing id.
- `transition-task-state.mjs persist-task-answers` — may only change existing
  `#### Answer` bodies, must change at least one, and must keep question
  identity byte-identical.

On the first publication the one difference the contract tolerates outside the
section is the count of trailing newlines at the end of the previous body — the
blank line that separates the new section from it. Trailing spaces or tabs are
not tolerated.

Both intents refuse a body that leaves a fence, HTML block, or HTML comment open
at the end: an unterminated container masks every later heading, including the
next question round and the `## Outcome` appendix. They also refuse CR bytes and
a leading BOM, like every other canonical task write.

A section that is already malformed is reported as the advisory
`TODO_QUESTIONS_INVALID` finding. Both intents refuse to write while it stands
and the board offers no answer rail for it at all, so the escalation path stays
closed until the section is repaired or removed through an authorized in-column
`edit`.

## Todo shape (structured task)

The orchestrator reads only files in `todo/`. Shape:

```markdown
# TASK <N> — <title>

## Goal
One paragraph. What capability does the user gain when this lands?

## Inputs
- Data this depends on (existing `*Feature` interface, existing route, existing widget).
- Where the entry point lives (which screen launches it, which menu item, which deeplink).
- Any constraints not derivable from the skills' references (product-specific copy, design figure references, accessibility callouts).

## Design
- **Conditional — present only when `figmaEnabled: true` and the task creates or visually modifies UI nodes (`screen`, `dialog`, `component`, or `overlay`).** One bullet per node: `- <Name> [kind] — <figma node URL>` (`[screen]` is optional). The explicit escape `- <Name> — none (<why no mock>)` exempts that node from the census and the spec/screenshot gates (surfaced in the ship summary's `### Spec fidelity` block as `SKIPPED — no mock`, status-neutral). It must be an owner/source decision: an empty composer URL and task-prep silence do not auto-create the exemption. Omit the whole section for non-UI or non-Figma tasks — its presence is exactly what marks a task as carrying Figma designs. See the implement-figma skill.

## Acceptance

### Automated
- Bulleted, automatically verifiable by reading the diff or running a build task. Every bullet must carry at least one automation anchor: a file path (`<path>`), a class/method reference (`<Name>.<member>`), or a gradle task (`./gradlew :...`).
- Includes the build gate: "`./gradlew :shared:assembleSharedDebugXCFramework` and `./gradlew :androidApp:assembleDebug` both green." (keep the module path `:shared`; substitute only the `Shared` task-name segment if `iosFrameworkName` was changed in `orchestrator/project-config.md`.)
- When `figmaEnabled: true`, `task-prep` additionally generates one spec-gate bullet per non-`none` `## Design` bullet — verbatim: ``- Spec: <NodeName> matches its Figma node values (<url>) — `figma-spec-validator` green (Minors allowed).`` — deferred to `figma-spec-validator` the same way the build gate defers to `build-validator`.
- When `figmaEnabled: true` (same condition as the spec-gate bullet above), `task-prep` also generates one screenshot-gate bullet per non-`none` `## Design` bullet — verbatim: ``- Screenshot: <NodeName> (primary state) has complete Figma oracle/capture evidence and matches its Figma oracle(s) under the project `screenshotPixelGate` policy — `figma-screenshot-validator` passes.`` — deferred to `figma-screenshot-validator` (Step 4.6b, post-assemble; a declared design with missing oracle/capture blocks instead of self-skipping).

### Manual
- Bulleted, requires a human action on device / browser / CLI to confirm. Optional section — omit it entirely when the task is pure infrastructure / codegen with nothing to verify by hand. Do NOT write `- none` and do NOT keep an empty `### Manual` heading.
- Examples: "Tapping the archive card on `ProfileOverviewScreen` pushes the new route." / "The new icon renders at every supported screen density." / "Encrypted file on disk is unreadable without the key."

## Out of scope
- What the task does NOT cover. Cuts off scope creep. **Required** — write "nothing else" if the boundary is trivial, but the section must be present.
- **Defensive bullets** — every promoted todo includes these five strings verbatim (unless the task explicitly authorises the gated surface). The first four are `scope-leak-validator` path/surface anchors; the TODO/FIXME marker bullet is owned by `anti-pattern-scanner` to avoid duplicate routing:
  - `no new entries in gradle/libs.versions.toml`
  - `no changes to CLAUDE.md or orchestrator/**`
  - `no changes to build-logic/** convention plugins`
  - `no schema migration (Database.kt version, migrations/*) — separate task`
  - `no TODO/FIXME markers left in code the task claims as done`

## Depends on (optional)
- TASK_<M>_<title> — must be in `tasks/done/` before this task runs.

## Origin (optional)
- split from TASK_<P>_<title> — records the parent this task was split off from (task lineage). One bullet, the parent stem only. Lenient aliases `- from …` / `- parent …` are also accepted (an optional colon after the keyword is accepted, e.g. `- from: TASK_…`).

## Preview states (conditional — figmaEnabled: true + a non-`none` screen-kind `## Design` bullet)
- **Generated by `task-prep` Step 5.5a** when `figmaEnabled: true` and the task carries non-`none` screen-kind `## Design` bullets (`[screen]` or untagged, because `[screen]` is the default). One bullet per screen: `- <ScreenName> — loaded (primary, compared vs oracle), empty, loading`. The `figma-screenshot-validator` checks that `ScreenshotTest.kt` has entries for every listed state; any missing state is a Minor finding. Omit entirely when `figmaEnabled: false`, for non-UI tasks, or for dialog/component/overlay-only tasks.
```

All four sections (`## Goal`, `## Inputs`, `## Acceptance`, `## Out of scope`) are required. `task-intake` returns BLOCKED if any are missing. The canonical `## Source` block remains immediately after the H1 and must be copied byte-for-byte from backlog during promotion. `## Design` is conditional (present only for UI tasks on `figmaEnabled` projects — see the Figma split below); `## Depends on` is optional. Unresolved dependencies are warnings during preparation and promotion; canonical Run admission verifies each listed task is accepted in `done/` and blocks execution otherwise. `## Origin` is optional structural lineage — `task-state-core.cjs` derives its parent stem into the INDEX `splitFrom` field. For a task split, Source must be Follow-up/task-split with the same parent Ref; neither section substitutes for the other.

### Design-system-first split (figmaEnabled only)

When a promoted UI task's screens reference design-system components that don't exist yet, `task-prep` Step 5.5 runs the component census (`orchestrator/figma/scripts/component-census.mjs` over the task's screen cache) and **splits**: each `MISSING`/`INCOMPLETE` component becomes its own `TASK_<M>_component_<widget>.md` in `todo/` (prep-generated Goal/Inputs/Acceptance per the `design-system-component-builder` recipe, `## Origin` pointing at the parent), and the parent screen task gets a `## Depends on` bullet per component task. The orchestrator then refuses the parent until the component tasks ship (`task-intake`'s standard dependency gate), so screens are always composed from matrix-true components instead of freehand approximations. `MAPPED` components never split; ambiguous matches become a `choice` question round instead. Contract: the implement-figma skill.

## Done

`finalize-task.mjs` publishes a task here as one recoverable transaction after
Outcome and all gates pass. It owns the no-clobber todo→done transition,
registry/INDEX/architecture publication, final post-validation and exact lock
release; no prompt performs those mutations independently. Treat the folder as
audit history:

- Don't edit files in place.
- Don't delete entries unless you're rolling back a completion.
- Re-running an already-moved task is rejected by `task-intake` (the file isn't where it expects).

If you want to discard a task without running it, use the board's **Drop task**
button (available on `todo/`, `backlog/`, and `pending/` cards — never on
`done/`). It shows dependent impact first, then the authorized transition
helper atomically publishes `live → absent` and a fresh INDEX. There is no
equivalent manual delete flow.

### Outcome appendix

Every done file ends with a structured `## Outcome` appendix the orchestrator writes before the move (see the task-orchestrator skill (references/run-loop.md) Step 6a). Shape:

```markdown
... original task body (Goal / Inputs / Acceptance / Out of scope) ...

---

## Outcome

**Status**: completed | completed-with-caveats | partially-completed
**Completed at**: <ISO8601 UTC>
**Reviewer**: codex | internal-reviewer
**Review iterations**: <integer>

### Build gates
- `<gradle task>` — pass | fail | skipped (<reason>)

### Runtime verify
- Gate: ran | deferred | skipped (<reason>)
- Result: pass | fail | n/a — <one-line note; include screenshot path if emitted>

### Acceptance trace
- `<first 80 chars of original bullet>` — verified | manual | deferred — <note>

### Caveats
- <one-line caveat, ≤120 chars>

### Follow-ups
- `TASK_<N>_<title>` — backlog | pending | todo | done

### Files touched
- `<repo-relative path>` — created | modified | deleted | renamed
```

Rules:

- All seven headings (`## Outcome`, `### Build gates`, `### Runtime verify`, `### Acceptance trace`, `### Caveats`, `### Follow-ups`, `### Files touched`) are required in this exact order. The only additional subsection is an optional final `### Execution log`. Empty semantic content → exactly one `- none` bullet; never leave a required section blank. `### Runtime verify` is the lone exception — it always carries exactly two bullets (`Gate:` + `Result:`), never `- none`, since the gate's reason for skipping is itself recorded in the `Gate:` line.
- A parser-optional **eighth** heading, `### Execution log`, MAY follow `### Files touched` (always last) — the durable digest of the pipeline journal (`.cache/tasks/journal/<STEM>.jsonl`). It is REQUIRED before finalizing a Figma-enabled UI task because `ship-done` inserts only the authenticated digest line and preserves every surrounding byte; it remains optional for non-UI tasks. It is deliberately NOT in the required-heading set of either parser, so a non-UI done file without it stays valid (`outcomeStatus` never flips to `malformed`). Keep it out of the canonical `headings` list in `contracts/outcome-shape.json`. See the task-orchestrator skill (references/run-loop.md) Step 6a.
- The `---` separator above `## Outcome` is the parser anchor — the site finds the appendix by the **last** `---` line in the file.
- `Status` is exactly one of three (matched case-insensitively): `completed`, `completed-with-caveats`, `partially-completed`. `INDEX.json` records this as `outcomeStatus`; if the appendix is missing or malformed, `outcomeStatus` is `malformed` and the site shows a grey badge.
- `Reviewer` is a required gate exactly like `Status`: it must be present and exactly one of (matched case-insensitively) `codex` or `internal-reviewer`. A missing `**Reviewer**` line or an out-of-set value makes `outcomeStatus` `malformed` (grey badge), even when `Status` itself is valid. The canonical core enforces the contract and the Site consumes its normalized server projection.
- All Outcome value sets (status, reviewer, acceptance/build/runtime verdicts, follow-up columns, and file changes) plus the required-heading list flow from `orchestrator/contracts/outcome-shape.json`. `task-state-core.cjs` and `figma/scripts/ship-done.mjs` read it at runtime; the site consumes the canonical server projection and has no browser Outcome parser or generated enum copy. Changing the sets means editing that JSON and passing the canonical parser/finalizer contract tests — never hard-code a parser-local enum.
- Once written, the appendix is immutable. Reopen only through the explicit
  `done → todo` action owned by `transition-task-state.mjs`; it captures the
  exact done bytes in content-addressed history and strips Outcome while
  publishing the validated todo. `task-intake` rejects a todo file that still
  carries an appendix.

## Workflow — what the user actually does

The user touches the pipeline at three points only: writes an idea, answers clarifying questions (if any), and asks Claude to run the task. The rest is automated.

### Step 1 — Drop the idea into `backlog/`

1. The user has an idea ("add a note-archive screen").
2. Open the site → **+ New backlog item** → enter the title and idea. The deterministic transaction reserves the next globally unique number, writes `backlog/<stem>.md`, and regenerates `INDEX.json` before returning.
3. The card is immediately available. A separate tool-less shallow intake may add a source-bound advisory preview, but its absence or failure never rolls back or blocks the task.

### Step 2 — Turn the idea into a structured task

1. Ask Claude: *"Run task-prep on TASK_<N>_<title>."*
2. `task-prep` reads the idea, classifies it against the change taxonomy, and checks every named artifact against the live codebase.
3. Two outcomes:
    - **Everything is clear** → `task-prep` gives the structured Goal / Inputs /
      Acceptance / Out of scope candidate to the canonical transition helper;
      only a validated promotion + fresh INDEX is reported successful. Jump to
      Step 4.
    - **Something is ambiguous** → `task-prep` submits a questions candidate to the canonical transition helper; only the helper may publish `orchestrator/tasks/pending/<stem>.questions.md` and the matching fresh `INDEX.json`. Go to Step 3.

### Step 3 — Answer the questions (only if any were written) — iterative

This step loops: you answer → Claude re-evaluates → either promotes or refines. Repeat until 0 questions remain.

1. Open the site board → find the task in the `pending` column → open it → fill the form (or edit `pending/<stem>.questions.md` by hand and write between `### Answer` and the next `## Q`). You can leave an answer blank — `task-prep` will re-ask it. You can write the literal token `SKIP` to permanently waive a question.
2. Ask Claude again: *"Run task-prep on TASK_<N>_<title>."*
3. `task-prep` classifies every answer:
    - **Clear** answer → question drops out of the next round.
    - **Partial** answer (you addressed only part of it) → re-asked next round with just the missing sub-bullet, your partial answer quoted back as context.
    - **Off-target** answer (you answered something different than what was asked) → re-asked next round with refined wording, your reply quoted back.
    - **Empty** → re-asked verbatim.
    - `SKIP` → permanently waived; never re-asked.
4. Sometimes a clear answer **exposes a new gap** ("use a bottom sheet" → now Claude needs to know whether it's blocking or dismissable). New questions appear in the next round alongside the carried-forward ones.
5. Two outcomes per iteration:
    - **0 questions left** → `task-prep` promotes to `todo/`, deletes the sidecar.
    - **Some questions left, gap count shrinking** → fresh round written; go back to step 1.
6. Cap: the loop iterates as many rounds as needed **as long as the `gapCount` strictly shrinks each round**. If the count stops shrinking for 2 consecutive rounds (clarification isn't converging), `task-prep` escalates — the task is likely structurally too big or contradictory; you decide whether to split, rewrite, or drop. There is a soft warning at round 5+, but the only hard stop is convergence.
7. The 7-questions-per-round cap is a **UX cap** (don't drown you with one giant form), not a total cap — the loop can ask 30 questions across 5 rounds if the task needs it.

### Step 4 — Run the task

1. Ask Claude: *"Run task `TASK_<N>_<title>.md`."*
2. Claude invokes `orchestrator`. From here, you wait. Under the hood, in order:
    - `task-intake` — decides which builders and validators apply.
    - **Pre-flight (parallel)** — `inputs-resolver` verifies every artifact named in `## Inputs` actually exists in the codebase; `context-finder` locates the files that will be touched (routers, components, existing features). Both run in a single Agent-tool batch. If inputs-resolver finds anything unresolvable → **STOPS, escalates to you** (context-finder output is discarded).
    - **(conditional) Figma-design pre-flight (Step 1b)** — when `figmaEnabled: true` and the task carries non-`none` `## Design` bullets: checks the `.cache/figma/screens/<stem>/` files exist (**BLOCKED** with a "Pull Figma screens" instruction if not), then runs the component census and feeds it to the planner and builders.
    - `implementation-planner` — writes the exact plan: which files to create, which canonical names to use, which public signatures.
    - **Builders** — write the actual code, following the plan as a contract (only builders write code).
    - **Validators** (10 in parallel; `build-validator` runs in compile-mode here) — check architecture, MVI shape, DI wiring, scope-leak, acceptance match, and so on. Findings get routed back to builders for fixes; the loop repeats until everything is green.
    - **Full assemble gate** — `build-validator` runs once in assemble-mode (full XCFramework + `assembleDebug` + `xcodebuild`). Catches R8 / dex / lint / native-link errors compile-mode skips.
    - **(conditional) Runtime-verify gate** (Step 4.6) — only if `verifyEnabled: true` or `verifyEnabled: auto` (read from `project-config.md`) and the `verify` Skill tool is available to the parent session; skipped otherwise.
    - **(conditional) Screenshot fidelity gate** (Step 4.6b) — `figma-screenshot-validator` runs Roborazzi (JVM/Robolectric) screenshots against the Figma oracle(s), adaptive per pulled theme. Fires when `figmaEnabled: true` AND the task carries at least one non-`none` `## Design` bullet (no separate enable flag); missing oracle/capture is a BLOCKER, not a skip. Runs after Step 4.6 (or after the assemble gate Step 4.5 when the runtime-verify gate is skipped).
    - **Review gate** — the Reviewer policy selects Codex only when the shared
      detector confirms it is available. Automatic mode otherwise selects the
      internal reviewer; Require Codex blocks; Internal review only skips Codex
      detection.
    - **(conditional) Security review** (Step 5.5) — triggered when the diff touches auth/token/credential paths.
3. When everything is clean, `finalize-task.mjs` owns the validated
   `todo → done` transaction, derived-state publication and lock release, then
   the orchestrator posts a summary.

### Step 5 — Accept the result

1. Read the summary: files changed, which `### Automated` acceptance bullets were `verified`, which `### Manual` bullets are recorded as `manual` (those you must check on-device).
2. If you're happy → `git commit` (the orchestrator never commits — that's always your call).
3. If something is off after shipping → use the board's explicit **Reopen**
   action (or ask Claude to reopen that exact stem), then re-run it. Never move
   the done file or strip Outcome by hand. If it is still in `todo/`, edit it
   through the authorized in-column flow instead.

### What the user does NOT do

- Write the structured task shape (Goal / Inputs / Acceptance / Out of scope) — `task-prep` writes it.
- Invoke builders or validators directly — `orchestrator` orchestrates them.
- Edit or regenerate `INDEX.json` — its owning create/edit/transition/finalizer
  transaction publishes and verifies it before reporting success.
- Commit, push, or open PRs as part of the run — those are explicit, manual user actions after the summary lands.

See the task-prep skill for the prep agent's full spec and the task-orchestrator skill (references/run-loop.md) for the execution-loop spec.

## Lifecycle changes — no manual folder moves

Do not use `mv`, `git mv`, `rm`, or a direct INDEX regeneration to change a
task column. The allow-list is executable and fail-closed:

- create: `absent → backlog` through the deterministic backlog creator;
- clarify/promote: `backlog → pending|todo` and `pending → pending|todo`
  through `transition-task-state.mjs` as owned by task-prep;
- finish: `todo → done` only through `finalize-task.mjs`;
- reopen: `done → todo` only after an explicit user action through
  `transition-task-state.mjs reopen`;
- drop: `backlog|pending|todo → absent` only after explicit impact review
  through `transition-task-state.mjs drop`.

For `ask`, `persist-answers`, `publish-questions`, `persist-task-answers`, `promote`, and the authorized in-column `edit`, the canonical
proposal transport is bounded stdin: compose the complete Markdown in memory,
pipe its exact UTF-8 bytes with `--input -`, and close stdin. The helper consumes
the whole stream (maximum 8 MiB) and rejects empty input, malformed UTF-8, NUL,
or overflow **before** it acquires a transition guard or touches task/runtime
state. A producer that has not closed stdin therefore cannot make a transition
visible.

`ask` publishes initial questions (`round: 1`, no `prevGapCount`) or a new
question generation (exactly `old round + 1`, original `createdAt`, and old
`gapCount` shifted to `prevGapCount`). `persist-answers` is the separate Mode-B
write intent: it keeps the current generation byte-identical outside existing
`### Answer` bodies. The rendered CommonMark question identity must also stay
identical: answer markup cannot hide, expose, add, or remove a question or
Answer heading. The helper rejects cross-intent rewrites before detaching the
durable sidecar.

`--input -` is the only proposal transport. Command-specific options and the
required source-revision shape are validated before stdin or any transition
state. Skills and prompts keep the proposal in memory, close stdin after the
exact bytes, and never stage temporary proposal files.

`todo → backlog` is intentionally unsupported. If a todo needs correction,
use the authorized in-column edit and revalidate it; if it needs discovery
again, create/re-prep a new backlog task and explicitly drop the obsolete todo
after reviewing dependents. A helper publishes INDEX only after its filesystem
postcondition passes, so there is no separate manual regen step.

If the task surfaced architecture drift while in flight, update the corresponding skill reference (under `orchestrator/skills/<skill>/references/`) manually — that's separate from the task lifecycle.
