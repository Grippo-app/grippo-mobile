# Prep flow — backlog → pending → todo, questions, convergence

Self-contained reference for the `task-prep` lifecycle: the two input modes, pre-flight gates,
the in-progress lock + journal, gap analysis, the promote-vs-ask decision, the questions sidecar,
Mode B convergence, canonical INDEX publication, and the output blocks. Builder classification lives in [`intake-classification.md`](intake-classification.md);
acceptance shape in [`acceptance-anchors.md`](acceptance-anchors.md); the Figma split in
[`figma-split.md`](figma-split.md); BLOCKED/ESCALATE conditions in
[`blockers-and-dependencies.md`](blockers-and-dependencies.md).

`task-prep` is the gatekeeper between a free-text user idea and a structured task the orchestrator can
execute. It produces **questions** when scope is unclear and a **promoted todo** when it is clear. It
NEVER writes product code.

## Hard limits

- NEVER write Kotlin / JS / product code.
- NEVER invoke product builders or product validators. The canonical task-state validator/helper is mandatory lifecycle infrastructure, not a product validation phase.
- NEVER guess an irreversible or unsafe ambiguity. In the normal direct flow,
  surface it as a question; under the Board contract below, exclude it from
  scope and report a typed actionable blocker/follow-up instead.

## Board Prepare no-questions contract

The exact prompt marker `BOARD PREP POLICY: NO QUESTIONS.` activates this
contract for the server-owned Board **Prepare** action only. It is neither a
project/global setting nor a change to direct task-prep calls.

- `needs_action`, `transition-task-state.mjs ask`, and publication of
  `pending/*.questions.md` are forbidden.
- Repo-decidable gaps become explicit `Assumed —` Inputs. Reversible ambiguity
  uses the safest conservative default.
- Irreversible, destructive, authorization, breaking-contract, or missing-owner
  ambiguity stays out of the runnable scope and becomes a typed actionable
  blocker/follow-up, never a request for user input.
- When a safe runnable scope remains, continue through Step 5 and promote it to
  `todo`.

---

## Authoritative reading (verify each concrete path with `[ -f <path> ]`; a miss → BLOCKED)

1. `orchestrator/skills/_index/install-manifest.json` — the skills roster you classify against (`capabilities.json` next to it maps each operation to its owning skill). This is the skills-only builder/validator catalog.
2. `orchestrator/skills/{ui-feature,data-layer,design-system,mappers}/references/index.md` — follow the relevant index row to recognise which recipe a backlog item maps to.
3. `orchestrator/skills/validation-gates/references/` — recognise escalation triggers (`when-to-stop-and-ask.md` in particular).
4. `orchestrator/project-config.md` — project context (product name, locales, codex flag, framework name) cited back to the user when relevant. Also read `figmaEnabled` here — it gates the `## Design`-section rule (Steps 2/5) and the census split (Step 5.5): `rg -m1 '^figmaEnabled:' orchestrator/project-config.md | awk '{print $2}'`.
5. The change-kind taxonomy — reuse [`intake-classification.md`](intake-classification.md) §1 verbatim. Do **not** re-derive it.
6. `orchestrator/tasks/README.md` — the lifecycle + the todo-file template written when promoting.

Also pre-flight the two helper references the orchestrator's Step 2 lookups route through —
`[ -f orchestrator/skills/task-orchestrator/references/context-finder.md ]`
and `[ -f orchestrator/skills/task-orchestrator/references/requirements-lookup.md ]` — and fold any missing path into
the same `BLOCKED: required reading missing — <list>` return. This guards file presence only; the
actual `Agent(...)` spawn resolves by agent registration, not by path.

If any required reading is missing, stop and report `BLOCKED: required reading missing — <list>`.

---

## Input modes

Invoked in one of two modes. The caller's prompt states which.

### Mode A — fresh backlog file

The caller hands `orchestrator/tasks/backlog/TASK_<N>_<title>.md`. Body is free-text; the only
required content is a first line `# TASK <N> — <title>`. Decide whether the task is **ready** (promote)
or **ambiguous** (ask).

- Missing the `# TASK <N> — <title>` first line → return immediately: **`BLOCKED: backlog file missing required first line '# TASK <N> — <title>'. Repair the heading so N exactly matches the existing filename stem, or drop and recreate the idea through “+ New backlog item”; then re-run task-prep.`** Never compute `max + 1` here: deterministic backlog creation is the sole allocator, and task-prep may not invent or change identity.
- Contains a `## Outcome` section (the orchestrator's done-trailer) → **`BLOCKED: outcome appendix present in backlog/ — file was carried back from done/ without an authorized lifecycle rewrite. Submit corrected backlog bytes through the canonical in-column edit transaction, then re-run task-prep; do not strip the trailer or move files by hand.`**

### Mode B — answered questions sidecar

The caller hands the answered sidecar in one of two forms:

- **Path form** — a reference to `orchestrator/tasks/pending/TASK_<N>_<title>.questions.md` whose `### Answer` blocks have been filled by hand on disk.
- **Inline form** — the answered markdown is embedded in the prompt (the site board UI sends this). Keep those bytes in memory until Step 0 has both authorized the writer and acquired/verified the exact task-prep lock generation; **no durable sidecar write may precede those proofs**. Mode B is explicitly two transactions. First obtain a canonical pending `sourceRevision`, pipe the submitted exact UTF-8 bytes to `transition-task-state.mjs persist-answers --input -`, and close stdin as described in the canonical boundary below. This operation admits only changes inside the existing `### Answer` bodies: frontmatter, question ids/order/text/types/options, `round`, `createdAt`, `updatedAt`, `gapCount`, and `prevGapCount` remain byte-identical. Never overwrite `pending/<stem>.questions.md` directly and never stage inline answers in a proposal file. **The board POST is the ONLY carrier of these freshly-typed answers**: require the helper's matching `ok: true`, `operation: "persist-answers"` receipt, then RE-READ the durable sidecar and prove byte-for-byte equality (or equality of SHA-256 over the exact bytes) with the submitted payload; `round:` and `### Answer` counts are additional structural checks, never substitutes for exact equality. On a helper failure or mismatch, the helper preserves/restores the previous valid sidecar; return `BLOCKED: answer sidecar write failed — re-submit`. Only after exact persistence is proven may task-prep classify answers: run canonical validation again, retain the fresh post-persistence `sourceRevision`, analyze the durable re-read bytes, and use only that fresh revision for the second ask-again/promote transaction. Never reuse the pre-persistence revision. An interruption or ESCALATE after the first receipt therefore leaves the user's answer round durable.

You wrote the original empty-answer sidecar in a prior Mode A run; the user filled in answers between
`### Answer` and the next `## Q` (or EOF). The original backlog file is still in `backlog/`.
Re-evaluate with the new context. Either promote, or write a new round of questions (replacing the
existing sidecar).

Before parsing answers, run the outcome-appendix check on **both** the sidecar AND the backlog file: if
either contains a `## Outcome` heading (matched line-anchored as `^## Outcome$`), return
**`BLOCKED: outcome appendix present in <backlog/|pending/> — file was carried back from done/ without an authorized lifecycle rewrite. Reconcile it through the canonical in-column edit/recreate flow, then re-run task-prep; do not strip the trailer by hand.`**

---

## Step 0 — authorize and own one exact in-progress lock generation

Before requesting lock admission, inspect
`orchestrator/.cache/tasks/finalizations/`. If it contains any non-mutex
`*.json` entry (including a corrupt/unsafe name), return
`BLOCKED: durable task finalization needs recovery — resume it before task-prep`.
Task-prep's transition helper publishes shared `INDEX.json`, so even a marker for another stem is
a global exclusion. Treat an unreadable marker directory as blocked. The site
runner/standby worker enforce the same rule; this is the direct-invocation net.

If `ORCHESTRATOR_WRITER_SESSION_ID` is inherited, its presence is not proof of
ownership. Before the first task/lock/INDEX mutation (also on every resumed
site turn), run `node orchestrator/tasks/writer-lease.mjs verify-session
--guard-finalization --session-id "$ORCHESTRATOR_WRITER_SESSION_ID" --stem
"<STEM>"` and require exit 0. Only a non-expiring, child-attached site lease is
eligible; a TTL-bounded direct lease cannot be inherited authority because it
could expire immediately after the check. Exit 2 means the lease is missing,
stale, unverified, unattached, bounded, wrong-stem, or publication won the
handshake: make no mutation and
return `BLOCKED: writer session is no longer authorized; restart or resume
through the site`. This check is read-only; never accept a session ID solely
because its environment value has a valid shape.

Without inherited `ORCHESTRATOR_WRITER_SESSION_ID`, distinguish the caller's
authority before acquiring anything:

- A standby invocation passes its complete guarded bounded writer-lease
  receipt (`version`, `leaseId`, private `token`, `sessionId`, `kind`, `stem`,
  `expiresAt`). Verify that exact active generation on every turn:

  ```bash
  node orchestrator/tasks/writer-lease.mjs verify --guard-finalization \
    --lease-id "<receipt.leaseId>" --token "<receipt.token>" \
    --session-id "<receipt.sessionId>" --stem "<STEM>"
  ```

  Require exit 0 and an exact matching receipt for one active, unexpired,
  verified bounded `task-session`; retain its id/token/session as transition
  authority. Do not acquire a second lease: it would correctly self-conflict
  with the standby generation. The lease remains caller-owned. Task-prep may
  renew that exact generation before each numbered phase and after any
  30-minute operation using `writer-lease.mjs renew --lease-id <id> --token
  <token> --ttl-ms 3600000`, then must immediately repeat the guarded exact
  verification above. An expired, unverified, changed, or refused generation is
  a fail-closed stop and is never reacquired or revived by task-prep. Task-prep
  must never release it; the standby claim lifecycle does that after the
  complete prompt returns.
- A truly direct invocation with neither inherited site authority nor a
  caller-supplied lease receipt acquires `node
  orchestrator/tasks/writer-lease.mjs acquire --guard-finalization --kind
  task-session --stem "<STEM>" --key "direct:prep" --owner-pid "$PPID"`. The
  `--owner-pid "$PPID"` anchor is mandatory: it binds the lease to the
  long-lived session process (the executing shell's parent), not the per-call
  shell that dies when the command returns — renewal/verification require a
  still-live owner identity. Run the command at the top level of the tool
  call — never inside `bash -c`, a heredoc, or any nested shell, where
  `$PPID` resolves to the dying per-call shell. A lease leaked by an
  interrupted turn stays active while this session process lives and holds
  every drainer — release it on every failure path. Retain the returned
  `leaseId`/`token`/`sessionId` privately and ownership-safely release this
  prep-owned lease on every task-prep return (promotion, new question round,
  validation failure, or no-op). Exit 2 means finalization owns publication —
  or another live board-task writer for the SAME stem won the handshake; make
  no mutation.

A successfully verified site or standby generation must not acquire a
duplicate. Release a prep-owned direct lease with `node
orchestrator/tasks/writer-lease.mjs release --lease-id <leaseId> --token
<token>` and require its zero exit. Never release a site- or caller-owned
generation from task-prep.

After that site/standby verification or direct lease acquisition, but
still **before** acquiring/verifying the task-prep lock or proposing any task
mutation, run:

```bash
node orchestrator/tasks/edit-marker.mjs guard --stem "<STEM>"
```

This is the mandatory second half of the backlog-edit/task-prep handshake. Exit
2 means a crash-recoverable edit marker (or unsafe marker state) owns shared
task publication: release any direct lease, make no mutation, and return
`BLOCKED: deterministic backlog edit needs recovery — retry task-prep after the
site completes it`. Never move this check before the writer lease: an edit that
wins first publishes its marker and blocks this guard; task-prep that wins first
is visible to the edit helper's writer scan, which aborts before replacing the
task. A resumed site turn repeats both lease verification and this edit guard.

### Canonical task-state boundary (mandatory for every sidecar/todo mutation)

Task-prep may compose Markdown, but it does not own durable column writes,
deletes, moves, or `INDEX.json` publication. Those effects belong exclusively
to `orchestrator/tasks/transition-task-state.mjs`:

1. Run `node orchestrator/tasks/validate-task-state.mjs --stem "<STEM>"
   --expect <backlog|pending> --check-index --json --caller task-prep` after
   writer authorization and immediately before preparing a mutation. Require
   `ok: true` and retain the exact `sourceRevision`. Never reuse a revision
   after any successful helper call or after re-reading changed task bytes.
2. Compose the complete proposed questions/todo bytes in memory. Pass those
   exact UTF-8 bytes on the helper's stdin with `--input -`, then close stdin.
   Do not stage task-prep output in `/tmp`, the repository, or a shell-created
   proposal file. The helper consumes the complete bounded stream and validates
   UTF-8 before acquiring a transition guard or mutating runtime/task state.
3. For an inherited site session, pass no lease flags (the helper verifies
   `ORCHESTRATOR_WRITER_SESSION_ID`). For either a caller-supplied standby lease
   or a prep-owned direct guarded lease, pass the exact private `--lease-id
   <id> --lease-token <token>` verified/acquired in Step 0. Never log or place
   the token in a task/journal file.
4. ASK and re-ASK use
   `node orchestrator/tasks/transition-task-state.mjs ask --stem "<STEM>"
   --input - --source-revision <exact-revision> <authority-flags>`, with the
   complete proposal delivered on stdin and EOF closed.
   The helper chooses `backlog→pending` or `pending→pending` from fresh state.
   Initial questions must use `round: 1` with no `prevGapCount`; re-ASK must
   preserve `createdAt`, increment `round` by exactly one, and copy the old
   `gapCount` into `prevGapCount`. Inline answer persistence instead uses
   `persist-answers` with the same flags and may change only Answer bodies in
   the existing round; markup inside an answer must not change which question
   or Answer headings are CommonMark-visible.
5. PROMOTE uses
   `node orchestrator/tasks/transition-task-state.mjs promote --stem "<STEM>"
   --input - --source-revision <exact-revision> <authority-flags>`, with the
   complete todo delivered on stdin and EOF closed.
   The helper atomically publishes todo, detaches backlog/pending sources,
   validates the postcondition, publishes `INDEX.json`, and rolls back exact
   source bytes if any postcondition fails.
6. Treat only a parsed v1 receipt with `ok: true`, matching `stem`, operation,
   and destination state as success. Exit `4` / `TRANSITION_SOURCE_CHANGED`
   means refresh state and restart the decision from current bytes; do not
   retry prepared output against a new revision. Any other failure is BLOCKED
   with the helper's bounded finding codes. Never report success or release
   the lock while claiming a transition succeeded without this receipt.

`--input -` is the only task-prep transport. An empty, oversized,
NUL-containing, malformed-UTF-8, or still-open stdin stream cannot acquire the
transition guard. Command and required source-revision shape are admitted
before stdin is read, and options outside a command's explicit allow-list are
rejected before any runtime state.

The helper performs fresh pre-validation, source-revision CAS, filesystem
post-validation, fail-closed INDEX publication, and final post-validation.
Running `regen-index.py`, `rm`, `mv`, or a direct Write against a durable
task-column path in addition to the helper is a contract violation.

After `## Authoritative reading` and the Input-mode validity checks pass (so
malformed input never flashes an in-progress badge), establish lock ownership
only through `orchestrator/tasks/task-lock.mjs`. The helper is the sole writer
and remover of `orchestrator/.cache/tasks/locks/<STEM>.json`; task-prep never
creates, replaces, adopts, or deletes lock bytes itself. `<STEM>` is the
backlog/pending filename without `.md` / `.questions.md`, for example
`TASK_3_chart_redesign`.

There are exactly two admission branches:

1. **Caller supplied a complete lock receipt for this run.** Require a parsed
   v1 receipt with `ok: true`, the exact `stem`, `stage: "task-prep"`, `runId`,
   `sessionId`, `lockHash`, `startedAt`, and complete `owner`. Verify that exact
   generation before doing any work and again on every resumed turn:

   ```bash
   node orchestrator/tasks/task-lock.mjs verify \
     --stem "<STEM>" --stage task-prep \
     --run-id "<receipt.runId>" --session-id "<receipt.sessionId>" \
     --expected-hash "<receipt.lockHash>"
   ```

   Require exit 0 and a matching v1 receipt. A shaped environment value, an
   `inspect` result, a same-stem file, age, or knowledge of only part of the
   tuple is not ownership. Verification failure means make no task mutation
   and return `BLOCKED: task-prep lock ownership changed — resume or recover
   the owning run`; never reacquire over it.
2. **No lock receipt exists for this run.** Acquire only after the writer lease
   and edit-marker guard are green:

   ```bash
   node orchestrator/tasks/task-lock.mjs acquire \
     --stem "<STEM>" --stage task-prep \
     --owner-kind "<site|direct>" --owner-id "<bounded-owner-id>"

   # Standby only — pass the exact caller-owned capability without logging it:
   node orchestrator/tasks/task-lock.mjs acquire \
     --stem "<STEM>" --stage task-prep \
     --session-id "<standby receipt.sessionId>" \
     --writer-lease-id "<standby receipt.leaseId>" \
     --writer-lease-token "<standby receipt.token>" \
     --owner-kind standby --owner-id "standby:<standby receipt.leaseId>"
   ```

   A site turn keeps `ORCHESTRATOR_WRITER_SESSION_ID` in the environment, so
   the helper binds the lock to that verified session. A standby turn passes
   the exact caller-owned writer receipt's `sessionId`, `leaseId`, and private
   `token` and uses `--owner-kind standby`; the helper scans that generation
   before and after lock publication and never persists or returns the token.
   A truly direct turn receives a generated session identity. None
   acquires a second writer lease. Require exit 0 and retain the complete parsed
   receipt privately for the entire run. In particular, retain the exact
   `stem`, `stage`, `runId`, `sessionId`, `lockHash`, `startedAt`, and `owner`;
   never reconstruct them from the lock path or log them into task Markdown.
   `LOCK_ALREADY_OWNED`, an unsafe lock path, a stale INDEX, or a failed action
   admission is BLOCKED and leaves the incumbent generation untouched.

Acquire also performs fresh canonical task-state + action admission before and
after no-clobber publication. Its `sourceRevision` describes admission only;
do not reuse it for ASK/PROMOTE. Obtain the mutation revision with the
canonical validator immediately before the transition, as specified above.

**Release** is a hard rule for every branch that successfully acquired or
verified a receipt: use the exact receipt tuple at Step 7.5 on the happy path,
or before a later `BLOCKED` / `ESCALATE` return. If execution stops before lock
admission, there is no owned receipt and no release attempt. A missing or
mismatched generation is not a successful no-op and must never be fixed with
raw deletion.

### Journal discipline

Append structured events to `orchestrator/.cache/tasks/journal/<STEM>.jsonl` via
`orchestrator/tasks/log-event.py`, so Task Details → Activity shows what prep did. Best-effort: append
`|| true`; the script never fails the run. Run from the repo root. The enum vocabulary is defined in
`log-event.py`'s docstring (the source of truth). Emit:

- **`phase-start`** right after the Step 0 acquire/verify receipt is proven: `python3 orchestrator/tasks/log-event.py "$STEM" phase-start --phase prep --status info || true`
- **a round note** whenever you write a questions sidecar (Step 4, and each Step 6 re-eval that re-asks): `python3 orchestrator/tasks/log-event.py "$STEM" note --phase prep --detail "round <N>: <M> questions" --meta round=<N> || true`
- **`task-split`** when Step 5.5 spawns component tasks: `python3 orchestrator/tasks/log-event.py "$STEM" task-split --phase prep --meta children=<comma-separated child stems> || true`
- **`phase-end`** on promotion to `todo/` (Step 5): `python3 orchestrator/tasks/log-event.py "$STEM" phase-end --phase prep --status ok --detail "promoted to todo" || true`
- **`stop`** on a Step 6.4 ESCALATE: `python3 orchestrator/tasks/log-event.py "$STEM" stop --phase prep --status escalate --detail "<one-line reason>" || true`

These are the intra-`backlog`/`pending` detail layer; the column transitions (backlog → pending → todo)
are recorded server-side from `INDEX.json`, so do not log those.

---

## Step 1 — classify the change

Reuse the change-kind taxonomy from [`intake-classification.md`](intake-classification.md) §1 verbatim.
Read that table, map the backlog text to one or more rows, and record:

- The list of builder kinds the task implies.
- For each kind, whether the prerequisite scaffold exists (mirror [`intake-classification.md`](intake-classification.md) §2 — `feature-module-scaffold-builder` before `screen-builder`, `data-service-scaffold-builder` before `endpoint-builder`, etc.).
- Any rows that clearly do **not** apply (so you know what to call out as out-of-scope in the promoted task).

If the backlog text matches no row in the taxonomy, that's a strong signal the task is
under-specified — go to Step 3 and ask questions.

---

## Step 2 — gap analysis (Mode A; re-run in Mode B per Step 6.2)

Spawn helpers in parallel to verify the backlog's references resolve against the live codebase:

```
Agent(subagent_type: "context-finder", prompt: "<consolidated questions about screens/features/routes mentioned in the backlog>")
Agent(subagent_type: "requirements-lookup", prompt: "<short keyword for any architecture concept the backlog mentions but does not name fully>")
```

Build a list of:

- **Missing inputs** — the backlog says "the existing X" but X does not exist on disk.
- **Ambiguous scope** — phrases like "and related screens", "also update the …", "modernise the layout" that have no enumerable boundary.
- **Multi-feature implications** — the backlog mentions ≥ 2 feature modules; confirm which is the primary owner and whether cross-feature wiring is intended.
- **Resource-only changes** — strings/drawables in non-English locales the backlog forgot to enumerate.
- **Migration triggers** — schema/entity edits that need explicit user authorization per `../../validation-gates/references/when-to-stop-and-ask.md`.
- **Missing UI-node designs** — only when `figmaEnabled: true` AND the Step-1 classification includes `screen|dialog|component|overlay` UI kinds: every UI node the task creates or visually modifies should have either a `- <NodeName> [kind] — <figma node URL>` bullet or an explicit audited opt-out `- <NodeName> — none (<why no mock>)`. A missing bullet, an empty value after the em-dash, or a no-mock claim without a reason is a real gap: ASK for the URL or the explicit reason. Never infer "no mock exists" from silence and never synthesize a `none` bullet; `none` disables the census and spec/screenshot gates for that node, so only source text or an owner answer may authorize it. A clearly non-visual edit may omit `## Design` entirely. See `screensPrompt` in `orchestrator/site/scripts/figma-actions.js` (the screen-cache/census authoring contract) and [`figma-split.md`](figma-split.md).
- **Contract-missing endpoints** — the backlog names (or implies) a backend endpoint. When `npm run --silent contract:paths` in `orchestrator/api-contract/` reports a valid snapshot, fold "does the backend expose <path/capability>? what shape?" into the context-finder questions — it reads the resolved inventory first and prefixes `CONTRACT_MISS:` on a miss. An endpoint absent from a *present* snapshot is a gap for Step 3 (options: stub ahead of the backend, refresh via Backend Test + Refresh / typed `contract:probe` then `contract:refresh-*`, or defer) — do NOT ask the user for a Swagger URL the snapshot already answers. No snapshot → contract questions go to the user only when the task actually needs an endpoint.

The gap list feeds Step 3.

---

## Step 3 — decision tree (Mode A)

First split every gap from Step 2 into **decidable** vs **undecidable**:

- **Decidable** — the answer is recoverable from the repo, a project convention, or an existing precedent (context-finder / requirements-lookup resolved it, there is one obvious project default, or the acceptance shape follows mechanically from the taxonomy). The user cannot supply information you do not already have; asking only makes them author what you can draft.
- **Undecidable** — genuine product intent or an irreversible choice only the user holds: which screen / flow / copy; whether to touch a schema-migration, destructive-data, or authorization surface; which of ≥ 2 feature modules owns the change; or a named input/artifact that does not exist on disk.

```
Mode A → after classification + gap analysis:
  ├── No undecidable gaps (decidable gaps may remain).
  │     → DRAFT each decidable gap's grounded default, record it as an
  │       `Assumed —` Input, PROMOTE (Step 5).
  └── One or more undecidable gaps.
        ├── Board Prepare no-questions contract active.
        │     → Use the safest reversible default; exclude unsafe/irreversible
        │       portions as typed blockers or follow-ups; PROMOTE whenever a
        │       safe runnable scope remains.
        └── Normal direct invocation.
              → ASK (Step 4) the undecidable ones only; draft the rest.
```

**Bias toward drafting a confirmable default, not toward asking.** Prep has repo access, so closing a
decidable gap is prep's job, not the user's: draft the grounded default, record it as an explicit
`Assumed — <fact>: <default> (basis: …)` bullet under `## Inputs` (visible and correctable via a Mode-A
re-edit), and promote. Reserve the questions round for genuinely undecidable gaps — never make the user
author what the repo already answers, and never ask a blank question when you can propose a concrete
default. This is NOT "guess and hope": an undecidable gap (product intent; irreversible
migration/destructive/authorization surface; ambiguous owner; missing artifact) is still a hard ASK
in the normal direct flow. Under `BOARD PREP POLICY: NO QUESTIONS.`, apply the
action-scoped safe-default/blocker rule above instead.

---

## Step 4 — write the questions sidecar

This entire step is forbidden when `BOARD PREP POLICY: NO QUESTIONS.` is
active. Never invoke `ask` or create/replace a pending sidecar for that Board
action; return to Step 3's safe-default/blocker branch.

Durable destination: `orchestrator/tasks/pending/<stem>.questions.md`. Compose
the following complete sidecar in memory; if a prior
sidecar exists, this is a `pending→pending` replacement owned by the helper,
not a direct overwrite. File shape:

```markdown
---
forTask: TASK_<N>_<title>
createdAt: <ISO8601 — first round's timestamp; never changes>
updatedAt: <ISO8601 — this round's write>
round: <N>
gapCount: <total gaps found this round (logical T_now), even when only 7 questions are rendered — the "Max 7 questions per round" cap is a render/UX limit only, NOT this count>
prevGapCount: <gapCount from the prior round; omit on first round>
---

## Q1 — <question text>

<descriptive body — for choice/multiselect, one bullet per option with a one-line trade-off, then a `**Recommended**:` line.>

**Type**: text | choice | multiselect
**Options**: a, b, c

### Answer


## Q2 — <question text>
**Type**: text

### Answer

```

`gapCount` / `prevGapCount` are bookkeeping for Step 6's convergence cap (6.5). The site form only
reads `forTask`, `round`, and the `## Q` body — the bookkeeping fields are inert from the UI's
perspective.

### Output language

By default, write every question in English. If the caller's prompt specifies an output language (the
site appends this when its UI is set to a non-English locale), write all **human-facing prose** in that
language — the `## Q<N> —` question text, the descriptive body, the option titles/descriptions, and the
`**Recommended**:` reason. Keep all **structural tokens verbatim in English**, because the site parses
them literally and never displays them: the frontmatter keys, the `## Q<N> —` heading prefix,
`**Type**:` and its value (`text` / `choice` / `multiselect`), `**Options**:` and its letter values,
the `**Recommended**:` label, `### Answer`, and the `(a)` / `(b)` / `(c)` option-letter markers. The
output language applies ONLY to this sidecar — a promoted `todo/` file is always fully English, since
the orchestrator and validators match English anchor strings.

### Rules

- **Every question proposes a concrete default — never blank.** The user confirms or edits a grounded draft; they never start from nothing. For `choice` / `multiselect` this is the required `**Recommended**:` line. For `text`, add a `**Proposed**:` line in the question body carrying your grounded draft answer (it renders as prose above the answer box); the user accepts it or edits. If you genuinely cannot propose any default, the gap is undecidable — that is the only case for a bare question, and it must name the single fact you need.
- **Prefer `choice` over `text` whenever the options are enumerable from the repo.** A confirmable list with a recommended default is faster than free text and lets the user one-click the default.
- **Max 7 questions per round — a UX cap, not a total cap.** Ask the 7 most important this round; the rest queue for next round. Step 6's convergence cap is the only hard stop.
- **Group related questions.** Two clarifications about the same screen ride in one question with sub-bullets, not two questions.
- **Use `choice` / `multiselect` when options are bounded.** Free-text answers are slower for the user and harder to parse.
- **`**Options**:` is required only for `choice` and `multiselect`.** Omit the line entirely for `text` — never write `**Options**:` with no value.
- **Trade-off framing — required for `choice` and `multiselect`.** Write one bullet per option above `**Type**:`, then a final `**Recommended**:` line. The site renders each bullet inline next to its radio/checkbox. Frame trade-offs in terms of **user value / build cost / maintenance cost / time-to-ship** — picking one or two per bullet, never all four. If you genuinely cannot recommend, write `**Recommended**: defer — needs <one-line on what context you'd need>`.

  Canonical option-bullet shape (the parser keys off the `(letter)` prefix and the `**Title**` block — keep that order, don't bold the letter):

  ```markdown
  ## Q1 — How should the archive screen open from ProfileOverview?

  - (a) **Inline expansion** — lightest build cost; constrains future filtering controls.
  - (b) **Push as sub-route** — preserves back-stack history; one extra route on `ProfileRouter`.
  - (c) **Modal bottom sheet** — feels heaviest; matches existing pickers; cheapest if you reuse `DialogConfig.*`.

  **Recommended**: (b) — project default for "list with future filter/search", history-back works for free.

  **Type**: choice
  **Options**: a, b, c

  ### Answer

  ```

  Optional contextual prose may sit between the `## Q<N> —` heading and the option bullets; it renders above the radios. The option bullets are absorbed into the radio rows — don't restate them as prose.

- **`text`-typed questions never carry option trade-offs.** No `(letter)` bullets, no `**Recommended**:` line. A single `**Proposed**:` draft answer in the body is expected (see the proposed-default rule above) — it is a starting point to confirm, not an enumerated choice. If you would write per-option trade-offs, it's actually a `choice` and you didn't enumerate the options yet.
- **Each question must be answerable in < 2 sentences.** If it needs a paragraph, split it.
- **No yes/no questions in disguise.** "Should we …?" → "Choose: (a) add it now, (b) defer to a follow-up task." The site form parses by `## Q\d+ —` heading; the answer is the text between `### Answer` and the next `## Q` (or EOF).
- **`createdAt`** uses ISO8601 from `date -u +"%Y-%m-%dT%H:%M:%SZ"`.
- **`round`** is the round counter, incremented each time `task-prep` publishes a new sidecar for this stem. **No prior sidecar on disk** (Mode A first pass) → `round: 1`. **Prior sidecar exists** (Mode B replacement) → read the old `round` and write `<old> + 1`; if the old sidecar lacks the field, treat it as `1` (new round = `2`).
- **Carry forward question identity when possible.** If round N had `Q3 — How will navigation work?` and the answer was partial/off-target, round N+1 re-uses `Q3` with refined wording instead of renumbering to a fresh `Q1`. The user sees a familiar question deepening, not a parade of new ones.
- **Do NOT delete the backlog file.** The user may want to consult it while answering. Only the canonical `promote` helper detaches it after todo post-validation.

Publish the temporary sidecar with the canonical `ask` operation, using the
fresh revision and authority contract above. Require a matching success
receipt and re-read the durable sidecar before emitting the matching Step-8
block — Mode-A "questions written" on the first round, Mode-B "more questions"
on any re-eval landing here. `ask` owns INDEX publication; do not run a second
publisher.

---

## Step 4.7 — test-foundation doctor (before the final todo proposal)

Task-prep is the single lifecycle owner of the primary foundation check: run it
after the intake answers are sufficient but BEFORE building the final todo
proposal and before any product planner. Step 0 of the run loop repeats the
doctor only as defense-in-depth — it never creates tasks or repairs the
foundation.

```bash
node orchestrator/tasks/task-test-foundation.mjs doctor --product-root <productRoot>
```

Exactly one typed state comes back; act on it, never on a partial reading:

| State | Action |
|---|---|
| `READY` | Continue to Step 5 promotion; nothing else to do. |
| `ABSENT_CAN_INSTALL` | Run the bootstrap-coordinator protocol below, bind the child stem into the parent's `## Depends on`, then continue to Step 5. |
| `PARTIAL_CORRUPT` | `BLOCKED:` with the exact missing/duplicate/drift entries from the inventory. Never "top up" a partial foundation by hand. |
| `CONFLICTING_STACK` | `BLOCKED:` name the conflicting markers; a separate owner-authorized clean-cut task picks one stack. Never auto-delete user tests/dependencies. |
| `UNSUPPORTED_VERSION` | `BLOCKED:` name the off-pin versions; never guess coordinates/APIs for an unpinned stack. |
| `TOOLCHAIN_UNAVAILABLE` | `BLOCKED:` name the exact JDK/SDK/simulator prerequisite. |

**Bootstrap coordinator protocol (`ABSENT_CAN_INSTALL` only).** The absent
foundation is a GLOBAL singleton intent, not a per-task errand:

1. Compute `foundationIntentHash` from the current machine-policy hash and the
   doctor's `doctorInventoryHash` (`task-test-foundation-contract.cjs`), then
   `claim` the durable no-clobber marker
   (`.cache/tasks/test-foundation/<intent-hex64>.json`). A rival claim returns
   typed `FOUNDATION_IN_PROGRESS` with the exact owner — reuse its promoted
   child as the dependency or report BLOCKED while its phase is live; never
   delete or adopt a marker without owner-death proof and marker/receipt
   reconciliation.
2. Scan the consistent task-state snapshot for an active child with the exact
   factory Source (`task-source-contract.cjs testFoundationPrerequisite(firstParentStem,
   foundationIntentHash)`). Exactly one match → reuse it as the dependency;
   zero → create; several matches, or any active bootstrap with a different
   intent → `BLOCKED`, never a second installer.
3. Create through the single canonical allocator (`create-backlog.py`) with the
   factory Source, `originStem` = first parent stem, and the stable global
   `dedupKey` `task-prep.test-foundation.<intent-hex64>` — crash recovery and
   rescans MUST return the same child, never a new number. The child body's
   `## Inputs` carries the exact canonical bullets `Test policy hash`,
   `Doctor inventory hash` and `Foundation intent hash` so a lost gitignored
   cache is recoverable from task bytes.
4. Under the same first-parent writer/session authority, run the existing
   delegated child flow: the child gets `## Origin` `- split from
   <first-parent-stem>`, a prep-owned lock and promotion `backlog→todo`
   (`transition-task-state.mjs` delegated promotion covers exactly this Source
   type with the same parent-ref/origin/lock/session/no-clobber checks as
   `task-split`). Advance the marker `claimed → child-created →
   child-promoted`; the process mutex is held until durable `child-promoted`.
5. The prerequisite child changes ONLY canonical build/test infrastructure,
   runs its allowlisted `bootstrap-foundation-fixture` structural gate, and can
   never write itself a `test-not-applicable`. Its Source type is the single
   doctor exception (no recursive prerequisite). If a crash left the child in
   `backlog` before delegated promotion, the child goes through its own
   canonical task-prep under a child-owned session, then the parent prep is
   re-run from its unchanged backlog bytes — a fresh prep of a backlog task,
   never a forbidden todo re-prep.
6. The parent's final todo proposal lists the exact child stem under
   `## Depends on` BEFORE its own promotion. Promotion with the unresolved
   dependency is fine; Run admission stays fail-closed until the child is
   accepted in `done/`.

## Step 5 — promote to todo

The backlog item is ready. Rewrite the free-text into the standard structured shape in memory. Freeze
the proposed `## Design` section before Step 5.5, and pipe the final parent to `promote --input -` only after every promotion-time augmentation has been
folded in. The full shape (Goal / Inputs / Design? / Acceptance / Out of
scope / Depends on?) and the promotion rules — every `### Automated` bullet's automation anchor, the
build gate, the five defensive Out-of-scope bullets, design-bullet handling, spec-gate generation —
live in [`acceptance-anchors.md`](acceptance-anchors.md). The Figma census split (Step 5.5 / 5.5a) runs
inside this promotion path before the parent file is written, so generated component tasks, parent
`## Depends on` entries, preview-state bullets, and screenshot-gate bullets are part of the verified
todo write — see [`figma-split.md`](figma-split.md).

**Precondition and collision check.** Run the canonical validator with
`--expect backlog` (Mode A) or `--expect pending` (Mode B), `--check-index`, and
retain its exact revision. It detects todo/done copies, partial pending pairs,
number/stem collisions, malformed Source provenance, and stale INDEX as explicit
findings. Do not reduce this to `[ -f ]` checks and never advise deleting a
possibly authoritative duplicate by hand.

Publish the complete temporary todo, including the backlog task's exact
canonical `## Source` block immediately after the H1, with the canonical
`promote` operation. Do not add Source to the pending sidecar and do not rewrite
its Kind/Type/Ref/Fingerprint while reshaping the remaining task sections.
For Source Type `api-work-package`, also copy the complete canonical
`## API Work Package` section byte-for-byte immediately after Source; its
package id and source aliases are immutable provenance and may not be
summarized, reordered, or regenerated.
Require its v1 `ok: true`, `operation: "promote"`, `state: "todo"` receipt,
then re-read `todo/<stem>.md`. On any failed postcondition the helper removes
its owned todo candidate and restores the exact backlog plus pending sidecar;
return `BLOCKED: promote transaction failed — source state preserved` with the
bounded finding codes. Task-prep itself never writes todo and never deletes a
source artifact.

Promotion is invalid if any of `## Goal`, `## Inputs`, `## Acceptance`, `## Out of scope` cannot be
filled. In the normal direct flow, go back to Step 4 and ask. Under
`BOARD PREP POLICY: NO QUESTIONS.`, first synthesize the safest conservative
runnable shape from evidence; if that remains unsafe or impossible, return a
typed actionable blocker/follow-up without publishing pending questions.
Preserve the task number `<N>` (do not renumber). Copy any `## Depends on`
entries verbatim. An unresolved ordinary dependency does not block promotion;
canonical Run admission remains closed until it is accepted in `done/` (see
[`blockers-and-dependencies.md`](blockers-and-dependencies.md)).

The successful helper receipt proves `INDEX.json` is fresh; return the Mode-A
or Mode-B output block (Step 8) without a second regeneration.

---

## Step 6 — Mode B re-evaluation (convergence-bounded)

You opened `orchestrator/tasks/pending/<stem>.questions.md` (if the caller sent inline answered
markdown, publish it first through `persist-answers --input -` and re-read the helper-owned durable sidecar — see
Mode B — then proceed). Keep asking until the gap count
reaches zero, escalating only when the count stops shrinking.

### 6.1 — Harvest and classify each answer

Read the current `round` and `T_prev` from the required `round` and `gapCount` frontmatter fields.
If either field is absent or malformed, stop and regenerate the sidecar through the canonical helper;
never infer control state from question headings.

For each `## Q\d+ —` heading, extract the answer block (lines between `### Answer` and the next `## Q`
or EOF), trim whitespace, and classify:

| Class | Trigger | What to do |
|---|---|---|
| **clear** | Non-empty, unambiguous, answers the question asked. | Resolved. Use the answer when promoting. Drop the question from the next round. |
| **partial** | Non-empty, answers only part / leaves a sub-bullet open. | Re-ask **only the unanswered sub-bullets** next round, with the partial answer quoted back (*"You said 'Profile screen' — and the entry point?"*). |
| **off-target** | Non-empty, but answered something different from what was asked. | Re-ask with refined wording that quotes the user's answer back and pivots to the original phrasing (*"Got the layout. The original question was about navigation — does the layout imply (a) inline list / (b) sub-route / (c) modal sheet?"*). |
| **empty** | Whitespace-only or missing answer block. | Re-ask next round verbatim. |
| **SKIP** | The literal token `SKIP`. | Permanently waive. Drop the question; do not re-ask. The promoted todo's Inputs/Acceptance must work without it — if Step 2 gap analysis later decides that's impossible, escalate. |

**Bias toward `clear` over `partial`.** If you're 80% sure the answer is sufficient, treat it as
`clear` and proceed; the user can still correct via a Mode-A re-edit of the promoted todo. The cost of
one over-asked question is higher than one slightly-over-confident promotion.

After classifying, the **carry-forward set** = `partial` + `off-target` + `empty` (count = `G`).
Resolved answers (`clear` + `SKIP`) drop out — `clear` feeds Step 6.2's gap-analysis source; `SKIP`
contributes nothing but closes its gap.

### 6.2 — Re-classify and re-run gap analysis

Using `backlog + clear answers` as the combined source (SKIP answers contribute no content but their
gaps count as closed for the carry-forward set), re-run Step 1 (taxonomy classify) and Step 2 (gap
analysis). Fresh answers may **expose new gaps** the original backlog didn't show (e.g. user picked
"bottom sheet" → now you need whether it's blocking or dismissable). If Step 2 surfaces a gap the user
already SKIPped, do NOT re-ask — escalate instead (SKIP is provisional only against non-essential gaps;
an essential SKIP is a structural conflict).

Let `N` = count of newly-surfaced gaps not already in the carry-forward set.
**Total carry-forward this round**: `T_now = G + N`.

### 6.3 — Decide

`T_prev` is always available on Mode B (Mode A wrote `gapCount`). `T_prev_prev` (the round before last)
is only available from round 3 onward — Mode A's first write omits `prevGapCount`.

| Condition | Action |
|---|---|
| `T_now == 0` | **PROMOTE** (Step 5). Use clear answers. When answers contradict a named artifact in the backlog text (backlog says "Notes screen" but the answer says "Tasks screen"), the **answers win** — they are the user's latest word. Surface the conflict in the promotion summary (Step 8). |
| `T_now > 0` AND `T_now < T_prev` (strict shrinkage) | **ASK again** (Step 4). Compose and helper-publish a replacement sidecar. New set: refined partials + refined off-targets + still-empty (same `Q` numbers) + new gaps (fresh `Q` numbers after the highest carried number). Cap at 7. Increment `round`. |
| `T_now > 0` AND `T_now >= T_prev` AND `prevGapCount` is absent (first Mode B run — one free stuck round) | **ASK again** (same overwrite rules). The first stuck round may just mean an answer surfaced as many new gaps as it closed; give one more attempt. |
| `T_now > 0` AND `T_now >= T_prev` AND `prevGapCount` is present AND last round was stuck (`T_prev >= T_prev_prev`) | **ESCALATE** per 6.4. |
| `T_now > 0` AND `T_now >= T_prev` AND `prevGapCount` is present AND last round shrunk (`T_prev < T_prev_prev`) | **ASK again** — only this round is stuck, not last. Counts can stall once without indicating an unresolvable task. |

Equal counts are never "progress" in the strict sense — they're handled via the freebie-round and
consecutive-stuck rules above.

### 6.4 — Escalation rules (convergence cap)

Two triggers, evaluated in order — see [`blockers-and-dependencies.md`](blockers-and-dependencies.md)
for the full convergence-stall ESCALATE text and the round-5+ soft warning.

### 6.5 — Frontmatter accounting

When composing the helper-owned sidecar replacement for the next round, persist these so 6.4's convergence math is recoverable:

```yaml
---
forTask: TASK_<N>_<title>
createdAt: <ISO8601 — original creation>
updatedAt: <ISO8601 — this round's write>
round: <new round number>
gapCount: <T_now>
prevGapCount: <T_prev>     # what the previous sidecar reported as gapCount
---
```

A first Mode A run writes `round: 1`, `gapCount: <T_now logical total, not the count of Qs rendered>`,
and omits `prevGapCount`. A Mode B re-eval reads `gapCount` as `T_prev` and `prevGapCount` as
`T_prev_prev`, then writes a fresh `gapCount` and shifts the old `gapCount` into `prevGapCount`.

---

## Step 7 — verify canonical publication receipt

Every ASK/PROMOTE helper invocation derives and atomically publishes
`orchestrator/tasks/INDEX.json` only after the filesystem postcondition passes,
then validates exact equivalence. Task-prep parses and verifies that helper
receipt instead of running `regen-index.py` itself. Column rules enforced by
the shared validator/publisher are:

- `backlog`: `backlog/<stem>.md` exists AND `pending/<stem>.questions.md` does NOT exist.
- `pending`: `pending/<stem>.questions.md` exists.
- `todo`: `todo/<stem>.md` exists.
- `done`: `done/<stem>.md` exists.

A task is in **exactly one** logical column at a time; pending is the backlog
body plus matching questions sidecar. If validation reports a collision or
partial pending pair, stop with its exact paths and safe recovery text. Do not
publish over it and do not guess which copy to remove.

---

## Step 7.5 — release the in-progress lock

After the transition helper's final postcondition and INDEX receipt are green,
release only the exact lock generation proven in Step 0:

```bash
node orchestrator/tasks/task-lock.mjs release \
  --stem "<STEM>" \
  --run-id "<receipt.runId>" --session-id "<receipt.sessionId>" \
  --expected-hash "<receipt.lockHash>" \
  --expected-state "<transition.state>" \
  --source-revision "<transition.sourceRevision>"
```

Require exit 0 and a v1 receipt with `ok: true`, `released: true`, and the exact
`stem` / `runId` / `sessionId` / `lockHash` plus the exact final `state` /
`sourceRevision` and a valid `snapshotHash`. The helper runs fresh state + INDEX
validation before prepare, immediately before detach, and after detach while
the active release generation still blocks every new owner. It commits release
only when all three verdicts match the final transition receipt; a replacement
generation or changed task/INDEX is preserved for exact recovery.

If you are about to emit `BLOCKED` / `ESCALATE` after Step 0, attempt the same
exact release before returning, but first obtain a fresh green canonical
state + INDEX receipt and use its `observedState` and `sourceRevision` as the
two final-release arguments. Never reuse the acquisition revision after task
bytes may have changed. `LOCK_NOT_FOUND`, `LOCK_IDENTITY_MISMATCH`,
`LOCK_CHANGED`, or an invalid receipt means release was not proven: do not
delete or overwrite anything, preserve the original receipt for recovery, and
return `BLOCKED: exact task-prep lock release could not be proven`. If the
helper reports `LOCK_RELEASE_RECOVERY_REQUIRED` or an interrupted release left
the receipt-owned `.release-*` generation, run:

```bash
node orchestrator/tasks/task-lock.mjs recover-release \
  --stem "<STEM>" \
  --run-id "<receipt.runId>" --session-id "<receipt.sessionId>" \
  --expected-hash "<receipt.lockHash>" \
  --expected-state "<fresh-final.state>" \
  --source-revision "<fresh-final.sourceRevision>"
```

Require a matching v1 recovery receipt. The command is idempotent and removes
only the exact retained inode after the supplied current state/revision and
fresh INDEX are green; a foreign canonical lock, stale final receipt, or
ambiguous/malformed quarantine is preserved. If state changed, run the normal
next task-prep iteration and retry recovery with its new final receipt rather
than editing the quarantine. Never delete `.release-*` manually. A completed
task transition remains completed; never roll task state backward merely to
hide a runtime-lock recovery condition.

---

## Step 8 — output format

Return a single Markdown block matching what happened.

### Mode A — promoted

```markdown
# task-prep — promoted

**Task:** TASK_<N>_<title>
**Decision:** ready — promoted from backlog to todo.
**Reason:** <one line — "taxonomy hit single recipe, all inputs resolve, acceptance is enumerable">
**Files touched:**
- `orchestrator/tasks/todo/<stem>.md` (created)
- `orchestrator/tasks/backlog/<stem>.md` (removed)
- `orchestrator/tasks/INDEX.json` (published by transition helper)

Next step: ask the parent Claude session to *"run task TASK_<N>_<title>.md"*.
```

### Mode A — questions written

```markdown
# task-prep — questions

**Task:** TASK_<N>_<title>
**Decision:** ambiguous — wrote <N> questions to pending/.
**Open gaps:**
- <one line each>

**Files touched:**
- `orchestrator/tasks/pending/<stem>.questions.md` (created)
- `orchestrator/tasks/INDEX.json` (published by transition helper)

Next step: open the site board → answer the questions → re-run `task-prep` on the same stem.
```

### Mode B — promoted after answers

```markdown
# task-prep — promoted after answers

**Task:** TASK_<N>_<title>
**Rounds:** <N>
**Decision:** ready — promoted from backlog to todo.
**Files touched:**
- `orchestrator/tasks/todo/<stem>.md` (created)
- `orchestrator/tasks/backlog/<stem>.md` (removed)
- `orchestrator/tasks/pending/<stem>.questions.md` (removed)
- `orchestrator/tasks/INDEX.json` (published by transition helper)

Next step: ask the parent Claude session to *"run task TASK_<N>_<title>.md"*.
```

### Mode B — more questions

```markdown
# task-prep — more questions

**Task:** TASK_<N>_<title>
**Round:** <N+1>
**Gap count:** <T_now> (was <T_prev>; <↓ shrinking | = stuck | ↑ growing>)

**Classification this round:**
- Clear (resolved, dropped): <count> — `Q<n>`, `Q<n>`
- SKIP (waived, dropped): <count> — `Q<n>`
- Partial (re-asked with refinement): <count> — `Q<n>`
- Off-target (re-asked with refined wording): <count> — `Q<n>`
- Empty (re-asked verbatim): <count> — `Q<n>`
- New gaps (exposed by clear answers): <count> — `Q<n>`

**Files touched:**
- `orchestrator/tasks/pending/<stem>.questions.md` (overwritten)
- `orchestrator/tasks/INDEX.json` (published by transition helper)

Next step: open the site board → answer the new questions → re-run `task-prep`.
```

Drop a sub-bullet category entirely (don't write `Clear (resolved, dropped): 0`) when its count is
zero — keep the report tight.

The ESCALATE output blocks live in [`blockers-and-dependencies.md`](blockers-and-dependencies.md).

---

## What prep MUST NOT do

- Do **not** directly write, move, delete, or regenerate files under `orchestrator/tasks/{backlog,pending,todo,done}` or `INDEX.json`. Compose Markdown in memory and pipe it only through `transition-task-state.mjs ... --input -`; do not author a temporary proposal file. The deterministic creator/transition helper owns every durable task-state effect. The canonical lock helper alone owns lock bytes; task-prep retains its exact receipt and authors only best-effort journal events. Delegated Figma exceptions remain limited to the transient census/cache reports; task-prep never mutates the component mapping registry (Mapping Review and finalization own it).
- Do **not** create, replace, adopt, clear, or garbage-collect a task lock with shell/filesystem operations. No `mkdir` + JSON write, `mv`, `rm`, age-based cleanup, or same-stem overwrite is an ownership proof; use only `task-lock.mjs acquire|verify|release|recover-release` with the full receipt tuple.
- Do **not** write Kotlin, Swift, JavaScript, or any product code. Your authored payloads are Markdown task/question proposals consumed by the helper.
- Do **not** invoke product builders or product validators. The canonical task-state validator/helper is the required publication boundary, not a product validation phase.
- Do **not** bypass an *undecidable* ambiguity — genuine product intent, an irreversible migration/destructive/authorization surface, an ambiguous feature owner, or a named artifact missing on disk → write a question; never invent product behavior and promote. A *decidable* gap (the repo, a project convention, or a clear precedent answers it) is different: draft the grounded default, record it as an `Assumed —` bullet under `## Inputs`, and promote — do not manufacture a question the user cannot answer better than you can.
- Do **not** ask more than 7 questions per round. Defer the rest.
- Do **not** renumber a task. `<N>` is assigned when the backlog file is first created and never changes through pending/todo/done.
- Do **not** modify `orchestrator/tasks/done/`. Completed tasks are an audit trail.
- Do **not** implement write-then-delete yourself. `transition-task-state.mjs promote` is the sole owner of target publication, source detachment, rollback, INDEX publication, and recovery.
- Do **not** remove a real unresolved dependency to force readiness. Preserve it
  through promotion; Run admission owns the accepted-`done/` gate. Self-
  dependencies and cycles remain blockers.
