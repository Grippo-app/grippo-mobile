---
description: Standby worker — drain the site's Run-in-Claude queue oldest-first through the durable standby transaction helper.
---

# /serve-queue — standby worker for the site's Run in Claude queue

Run this command under `/loop` from the project root, in full-auto mode. The
site publishes exact v2 request snapshots; this session executes only the
claimed `prompt`, verbatim. It does not reinterpret queue metadata and does not
edit queue, claim, heartbeat, reservation, or receipt files itself.

All standby heartbeat, claim, restore, disclosure, and consume authority belongs
to:

```bash
node orchestrator/site/scripts/standby-queue.mjs <command>
```

The helper is project-root anchored through the shared `fileGuards` boundary.
Its cross-directory claim/restore/consume transaction is descriptor-pinned,
no-clobber, fsynced, and WAL-backed. Never replace a helper call below with
`cat`, `ls`, `mv`, `rm`, `mkdir`, redirection, `node -e`, or a hand-written
link/rename sequence.

## One pass

### 1. Runner exclusion and heartbeat

Run:

```bash
node orchestrator/site/scripts/standby-queue.mjs begin-pass
```

Interpret only its exact status:

- `ready`: the CLI runner marker was absent, or its exact version-1 record
  names a process generation proven dead/reused, and the private atomic
  heartbeat is durable. Keep the returned 64-hex
  `passToken` only in this pass's private context and continue immediately.
- `runner-active`: the exact bounded version-1 runner marker names a live,
  matching process generation; report
  `runner active — standing down` and end this pass.
- `runner-unknown`: runner ownership could not be disproved: the marker is
  structurally unsafe, non-conforming, or its exact process-generation probe
  is unavailable. Marker age never grants takeover authority. Report it and
  end this pass without a heartbeat or claim; explicit operator recovery must
  remove an unprovable marker.
- `error`: stop fail-closed. Do not guess another root or path.

The helper validates that cwd is the configured project root by directory
identity, bounds the runner marker at 4096 bytes, requires its exact contract,
and writes `heartbeat.json` by an anchored atomic replace with private mode.
The heartbeat stores only a SHA-256 hash of the random pass token. A newer
`begin-pass` invalidates the previous generation; a token is fresh for at most
30 seconds and authorizes exactly one following claim attempt.

### 2. Claim exactly one oldest request

Run:

```bash
node orchestrator/site/scripts/standby-queue.mjs claim-next --pass-token "<passToken>"
```

Possible results:

- `empty`: report `queue empty` and end the pass.
- `claimed`: keep the returned opaque `handle` and prompt-free `request`
  projection in this pass's private context.
- `runner-active` or `runner-unknown`: runner ownership appeared or became
  ambiguous after Step 1; no request was moved. End the pass.
- `retry`: another drainer or cancellation won the race; end this pass and let
  `/loop` retry from Step 1.
- `recovered`: a receipt-authorized crash recovery completed; end this pass so
  the next pass observes the recovered state cleanly.
- `blocked` with code `task-writer-active`, `writer-scan-unavailable`, or
  `writer-scan-blocked`: frozen serial safety — a board-task writer lease is
  still active somewhere (a site runner session, another standby execution, or
  an orphan of a dead site process), or writer-lease state cannot be proven
  safe. No request was moved and nothing needs recovery; end the pass and let
  `/loop` retry later.
- `retained` or any other `blocked`: private evidence is ambiguous, invalid,
  foreign, or already offered to another drainer. Report the code and stop for
  explicit recovery. Never delete or restore it by age.
- `error`: stop fail-closed.

`claim-next` first validates and atomically consumes the exact heartbeat
generation, then independently rechecks the runner marker before any queue
move (and once more immediately before selecting a newly listed candidate).
Never reuse a `passToken`, including after `empty`, `retry`, `recovered`, a
runner result, a crash, or an error. Start the next pass again at Step 1.

The projection contains only:

`id, version, action, stem, expectedState, sourceRevision, createdAt,
fingerprint, promptHash`.

It never contains `prompt`, `dedupKey`, `dedupReport`, task/answer bodies, an
absolute private path, or receipt internals. The helper atomically moves the
oldest exact `^[0-9]+-[a-z0-9]+\.json$` entry into a unique private run
directory before reading it. A symlink, hardlink, special file, oversize file,
malformed JSON, unsupported shape, wrong project root, or contract mismatch is kept
privately as evidence and is never executed.

The claimed-file contract is exact: exactly `action,createdAt,dedupKey,dedupReport,expectedState,projectRoot,prompt,sourceRevision,stem,version`.
The descriptor boundary uses `O_RDONLY|O_NOFOLLOW`, proves a stable regular
inode and caps the file at 256 KiB. `version` is exactly `2`; `sourceRevision`
is `sha256:` plus 64 lowercase hex characters; the stem matches
`^TASK_([1-9][0-9]*)_[A-Za-z0-9_]+$`; and the prompt is bounded to 60,000 UTF-16 code units and 180,000 UTF-8 bytes.
A mismatched root, or unsupported `createBacklog` action is permanently invalid:
do not execute, restore, or unlink it. Retain the private claim for explicit recovery.

At any later point, this prompt-free diagnostic is safe:

```bash
node orchestrator/site/scripts/standby-queue.mjs status --handle "<handle>"
```

### 3. Pair the exact reservation

Run:

```bash
node orchestrator/site/scripts/standby-queue.mjs ensure-reservation --handle "<handle>"
```

This helper owns reservation lookup; the claim path is not exposed.

Keep the returned complete reservation receipt
`version,requestId,stem,fingerprint,token,createdAt`. The helper requires the
receipt published with the Site request. A missing receipt or a receipt for
another id/fingerprint is never synthesized or stolen.

Any failure means no prompt: leave the private claim intact and stop for
explicit recovery. Reservation and private claim have no time-based expiry.

### 4. Canonical same-stem finalization probe

Never inspect or mutate a finalization marker or mutex with ad-hoc filesystem
commands. With the exact reservation from Step 3 still active, run only:

```bash
node orchestrator/site/scripts/standby-queue.mjs \
  record-finalization-superseded --handle "<handle>"
```

Interpret its exact bounded envelope:

- `superseded-recorded` mechanically proves one active same-stem finalization
  marker and publishes the immutable terminal tombstone. Continue with the
  Step-7 reservation release and superseded consume; no writer lease exists.
- `error:finalization-supersession-unproven` is only a negative same-stem
  result. It does **not** prove that foreign markers, the global mutex, or
  deterministic publication recovery are clear; continue to the guarded
  acquisition in Step 5.
- Any other error means the finalization authority could not be classified.
  Restore through Step 6 and end the pass without acquiring a writer lease.

This first probe and the guarded acquisition below deliberately overlap. If a
same-stem finalization appears between them, Step 5 detects it and repeats this
same authoritative command before deciding terminal supersession.

### 5. Writer lease and mandatory second guards

Acquire through the canonical helper:

```bash
node orchestrator/tasks/writer-lease.mjs acquire \
  --guard-finalization \
  --kind task-session --stem "<request stem>" \
  --key "standby:<request action>" --ttl-ms 3600000 \
  --owner-pid "$PPID"
```

`--owner-pid "$PPID"` is mandatory: it anchors the lease to the long-lived
worker process (the executing shell's parent) instead of the per-call shell
that dies the moment this command returns. Later `verify`/`renew` calls and
`task-lock.mjs acquire` all require a still-live owner identity, and the
cross-process serial-safety exclusion stays anchored to the real writer for
the whole execution instead of degrading into a bare TTL timer. Run the
command at the top level of the tool call — never inside `bash -c`, a
heredoc, or any nested shell, where `$PPID` resolves to the dying per-call
shell and silently recreates the short-lived-owner defect. The flip side of
the live anchor: a lease leaked by an interrupted turn stays active while
this session process lives, holding every drainer — release it (Step 6 path)
on every failure, and if a hold persists, `writer-lease.mjs scan` names the
owner pid so the operator can end that session cleanly. Never delete lease
files by hand.

This guarded acquire is the canonical initial classification for every foreign
finalization marker, the global finalization mutex, deterministic creation/edit
recovery, and conflicting writer authority. Frozen serial safety: a
`task-session` acquire is refused (`[WORKSPACE_WRITER_ACTIVE]`) while ANY other
board-task writer lease is active — any stem, any drainer — not only a
same-stem owner. Interpret it as follows:

- Exit `0` with the exact v1 receipt means the initial guard passed. Keep its
  full `leaseId,token,sessionId` privately and continue below.
- Exit `2` carrying `[FINALIZATION_MARKER_ACTIVE]` means the acquire helper
  detected a marker and attempted exact withdrawal of its just-created lease;
  restore/consume remains the authority that proves absence. Repeat the Step-4
  `record-finalization-superseded --handle` command. On
  `superseded-recorded`, continue with Step 7; otherwise restore through Step 6.
- Any other guarded refusal yields no usable lease receipt. Restore through
  Step 6; that helper independently retains the private claim if any writer
  release is not mechanically proven.
- Missing, malformed, or ambiguous output is not a lease. Retain the private
  claim and stop rather than guessing that the helper released anything.

After a successful acquire, immediately rerun the exact guarded verification
from disk. This is the mandatory second finalization/mutex, deterministic
creation/edit recovery, and conflicting-writer guard — not an instruction to
inspect their files:

```bash
node orchestrator/tasks/writer-lease.mjs verify --guard-finalization \
  --lease-id "<leaseId>" --token "<token>" \
  --session-id "<sessionId>" --stem "<request stem>"
```

If this verify reports `[FINALIZATION_MARKER_ACTIVE]`, the lease remains held:
repeat the Step-4 supersession command. On `superseded-recorded`, follow Step 7,
which releases the reservation and this lease before consume. On refusal,
release the exact lease and restore through Step 6. Any other verify refusal is
also release-then-restore; an ambiguous release keeps the claim retained.

Finally run the remaining stem-scoped publication guard:

```bash
node orchestrator/tasks/edit-marker.mjs guard --stem "<request stem>"
```

A temporary, foreign, or unreadable edit owner means exact lease release,
Step-6 restore, and end of pass. Only after both commands are green may the
selected skill reuse and reverify this caller-owned standby lease. It must not
acquire or release a second `task-session` lease.

`task-lock.mjs acquire` receives that exact `sessionId`, `leaseId`, and private
`token` through `--session-id`, `--writer-lease-id`, and
`--writer-lease-token`, plus `--owner-kind standby` and
`--owner-id standby:<leaseId>`. It exact-verifies the active capability before
and after no-clobber lock publication and never persists or returns the token.
Transition helpers receive the same exact lease id/token;
`finalize-task.mjs` receives `--writer-session-id <sessionId>`.

The combined guard rejects ANY other foreign ACTIVE board-task
(`task-session`) writer — any stem, any drainer, frozen serial safety — plus
every global deterministic creation/edit publication lease.
Exit 2 means another writer or publication recovery refused the lease.
The complete returned receipt (`version`, `leaseId`, `token`, `kind`,
`stem`, `sessionId`, `expiresAt`) remains private. The selected skill may renew
and reverify it, but MUST NOT release it. The standby pass owns the one release:
Step 6 for a retry, Step 7 for supersession, or Step 8 after execution.

If a lease operation is ambiguous, retain the private claim and stop; do not
invent ownership from elapsed time.

Only `standby-queue.mjs restore` may restore the private claim. Raw filesystem
link/rename/unlink sequences are forbidden.

### 6. Exact no-clobber restore for every retryable outcome

First release the writer lease if one was acquired. Keep or recreate the exact
reservation, then run only:

```bash
node orchestrator/site/scripts/standby-queue.mjs restore --handle "<handle>"
```

`restored` means the exact claimed inode is durably back at its original queue
name and the private operation is settled. Before requeue, the helper scans the
complete writer authority and mechanically proves the exact execution-fence
lease (or any matching pre-execution standby lease) absent; an active, stale,
unreadable, or recovery-ambiguous generation keeps the private claim in place.
`restore-collision` means a public
entry already exists — even identical bytes in a different inode are foreign.
The helper overwrites and deletes nothing; it retains the private claim and WAL
for explicit recovery. Any disappearance, replacement, ancestor swap, or
durability ambiguity is also retained fail-closed.

### 7. Fresh execution fence and durable supersession

**Execution fence.** This is the final read-only authority before prompt
handoff.

With the writer lease still owned, inspect only through the lock owner:

```bash
node orchestrator/tasks/task-lock.mjs inspect --stem "<request stem>"
```

Absence is proven only by exit `1` with the exact v1 JSON envelope
`ok:false, code:"LOCK_NOT_FOUND", retryable:false`. Exit `0` means a lock
generation exists; any other exit or malformed envelope is unproved. In either
non-absent case, release the lease, restore through Step 6, and disclose no
prompt. After exact absence, run the canonical state/action authority:

```bash
node orchestrator/tasks/validate-task-state.mjs \
  --stem "<request stem>" --action "<request action>" \
  --check-index --json --caller standby
```

Execution is allowed only when exit is `0` and the envelope says all of:

- `ok:true`;
- action equals the claimed action;
- observed state equals `expectedState`;
- source revision equals `sourceRevision`;
- `indexStatus:"fresh"`.

An unavailable, malformed, contradictory, or transient fence result means:
release the writer lease, restore through Step 6, and stop without prompt
disclosure.

A well-formed stale result is terminal, but the caller does not choose or
serialize its reason. With the exact standby lease still active, run:

```bash
node orchestrator/site/scripts/standby-queue.mjs record-superseded \
  --handle "<handle>" \
  --lease-id "<leaseId>" --lease-token "<token>" \
  --session-id "<sessionId>"
```

The helper independently exact-verifies the lease, proves task-lock absence,
runs the canonical validator from a fresh snapshot, and derives the first
mechanically supported reason: `state-changed`, `source-revision-changed`, or
`task-integrity-invalid`. A healthy request cannot be tombstoned. It accepts no
caller-authored reason, verdict, stdin, paths, or prompt fields.

For the earlier mechanically proven same-stem finalization branch, use only:

```bash
node orchestrator/site/scripts/standby-queue.mjs \
  record-finalization-superseded --handle "<handle>"
```

That command succeeds only while the fresh canonical runtime snapshot contains
exactly one active same-stem finalization marker. The terminal id and admitted
lineage are immutable. If the exact durable tombstone already exists, replay is
idempotent even after the live authority disappears; any lineage mismatch keeps
the private claim intact.

Only after `superseded-recorded`:

1. release the exact reservation with its complete receipt;
2. release the writer lease if held;
3. consume through:

   ```bash
   node orchestrator/site/scripts/standby-queue.mjs consume \
     --handle "<handle>" --kind superseded
   ```

The consume helper re-reads the exact tombstone, proves the reservation absent,
and proves the same-stem standby writer authority fully released. A tombstone
mismatch, writer scan issue, active/stale matching generation, or ambiguous
reservation keeps the claim intact.

Release an exact reservation only through:

```bash
node orchestrator/site/scripts/request-reservation.mjs release \
  --request-id "<requestId>" --stem "<stem>" \
  --fingerprint "<fingerprint>" --token "<token>" \
  --created-at "<createdAt>"
```

If release is ambiguous, run `ensure-reservation --handle <handle>` again,
release the writer lease, restore through Step 6, and stop.

A standby crash keeps private claim + reservation as paired evidence; neither is automatically stale.
A crash after reservation withdrawal is ambiguous and
is never auto-requeued.

### 8. Prompt handoff and executed consume

Immediately after the final green fence, before releasing the reservation,
ask the transaction helper to independently repeat that fence and publish the
non-secret execution intent bound to the exact writer-lease generation:

```bash
node orchestrator/site/scripts/standby-queue.mjs prepare-execution \
  --handle "<handle>" \
  --lease-id "<leaseId>" --lease-token "<token>" \
  --session-id "<sessionId>"
```

The helper does not trust the prose-level validation above. It mechanically
requires the exact active bounded `task-session` lease with key
`standby:<action>`, rejects conflicting same-stem/global publication leases,
proves the canonical task-lock path absent, and reruns the canonical action
validator. It requires fresh INDEX, exact expected column and exact source
revision, then stores that snapshot hash and lease identity in the durable
execution receipt. No lease token is stored.

The lock absence is not a check-then-act promise. While this lease is active,
`task-lock.mjs acquire` scans standby writer authority both before and after
lock publication. Every foreign acquire is rejected (or its exact just-created
generation is rolled back); only the selected skill can acquire with this
receipt's exact `sessionId`, `leaseId`, private `token`, `owner-kind standby`,
and `owner-id standby:<leaseId>`. This also protects lockless `drop` and
`reopen` transitions from a competing prep/run lock between the final fence and
prompt execution.

Only `execution-prepared` permits the next step. Release the exact reservation
with the command above. Then request the prompt exactly once using the same
lease receipt:

```bash
node orchestrator/site/scripts/standby-queue.mjs read-prompt \
  --handle "<handle>" \
  --lease-id "<leaseId>" --lease-token "<token>" \
  --session-id "<sessionId>"
```

After successful reservation release, this is the sole prompt disclosure
boundary. It repeats the exact lease, lock, state, revision and INDEX checks
after reservation withdrawal and durably binds this final snapshot to the
disclosure receipt before reading the prompt. A stale/absent task, changed
source, malformed/stale INDEX, lost/foreign lease, conflicting writer, or any
lock evidence therefore produces no disclosure receipt and no prompt.

This command is the only command allowed to output `prompt`. It first writes
and fsyncs the disclosure receipt, then emits the exact prompt bytes. Treat its
stdout as if the user pasted it into this session and execute it verbatim to
completion, including its sub-agents. Do not trim, paraphrase, improve, log, or
ask questions about it. Keep the complete private writer receipt in the selected
skill's run context so its canonical lock/transition helpers can exact-verify
the same authority; never splice the token into task Markdown or logs.

Disclosure is intentionally one-shot. A crash after its receipt but before
stdout is ambiguous and loses automatic execution rather than risking a
duplicate; later calls return `prompt-already-disclosed` without prompt bytes.
Root recovery must decide what ran.

After the run finishes, release the writer lease exactly once:

```bash
node orchestrator/tasks/writer-lease.mjs release \
  --lease-id "<leaseId>" --token "<token>"
```

Only after confirmed release consume the exact claim:

```bash
node orchestrator/site/scripts/standby-queue.mjs consume \
  --handle "<handle>" --kind executed
```

The durable consume receipt is published before detaching the claim. The helper
first proves the exact writer generation released. For `prep`, `answers`,
`drop`, and `reopen`, it also requires the canonical task-lock path and every
release-recovery generation absent. For `run`, lock absence is an accepted
terminal at-most-once settlement; that absence does not by itself assert
successful completion. The other accepted shape is one canonical retained
orchestrator lock whose standby owner id and session match the disclosure fence
exactly (the BLOCKED continuation case); a foreign/replaced generation is
retained fail-closed.
It then verifies exact inode/hash lineage, fsyncs the detach, deletes only that
authorized private inode, and settles the WAL. A foreign/replaced/missing
claim, active reservation, absent disclosure receipt, unsettled lock, or
durability ambiguity is retained. There is no age-based cleanup.

End this pass after one request. `/loop` starts the next pass at Step 1, which
refreshes runner exclusion and heartbeat before another claim. Requests are
therefore strictly oldest-first and never executed in parallel by this standby
session.

## Cancellation and crash semantics

Cancellation, the CLI runner, and multiple standby drainers race at one
no-clobber ownership boundary. Exactly one can remove the canonical public
name. A transient extra hardlink invalidates standby authority and is retained;
it is never treated as an executable copy.

Every restore or consume mutation is authorized by an immutable receipt made
durable first. Recovery completes only that recorded direction. It never uses
mtime, process liveness, PID reuse, or elapsed age. Same-byte foreign inodes,
unknown operation entries, unsupported private claims, source/claim disappearance,
replacement, and symlink/special targets remain explicit evidence.

## Context hygiene

The queue, claims, receipts, reservations, locks, and task files are the source
of truth; this chat is not. Keep only the current opaque handles/receipts in the
parent context and run task work in fresh sub-agents. Finish the current task
before restarting a long-lived standby session. Do not use `/clear` mid-loop.

## Never

- Never read or mutate queue/claim/receipt paths outside the named helpers.
- Never reveal a prompt through claim, status, errors, tombstones, logs, or
  recovery reports.
- Never restore after disclosure or consume before its exact durable receipt.
- Never repair, delete, requeue, or steal evidence because it is old.
- Never run newest-first, batch, or parallelize standby execution.
- Never commit, push, or open a PR unless the claimed prompt explicitly asks.
