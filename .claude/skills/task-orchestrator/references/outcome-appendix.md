# Ship — Step 6: chat summary + `## Outcome` appendix (6a–6d)

When every gate is green, do all of this **in the same turn**: prove the current
sealed test-summary → compose and strict-validate the Outcome bytes → create the
completed `ship` checkpoint for the exact execution generation → publish the
generation-bound Outcome draft → post the chat summary and end the turn. The
draft's exact final name is the durable handoff, so it is exposed only after the
three preceding evidence fences pass. A run NEVER publishes: it cannot reach
the control root at all, and
`finalize-task.mjs` is driven only by the integration transaction the owner
starts with Integrate. That transaction owns the atomic Outcome install,
component/token binding phases, sanctioned move, derived artifacts,
verification, and lock release. The happy path ends only when it
reports success and removes its recovery marker.

The frozen `## Outcome` → `### Acceptance trace` shape (six required `###`
headings, three trace verdicts) is pinned in
[`orchestrator/contracts/acceptance-trace.md`](../../../contracts/acceptance-trace.md);
match it exactly — drift silently flips the board badge to `malformed`.

## Step 6 — Chat summary

Post a single status block to the user (separate from the appendix — this is
conversational, the appendix is a structured trailer on the task file):

```markdown
## Task TASK_<N>_<title> — done

### Files changed
- <path> — <one-line>

### Pre-flight
- `inputs-resolver`: all <N> Inputs claims resolved.
- `implementation-planner`: contract covered <N> files (<C> create / <M> modify), <N> acceptance bullets (structural: <s> / build-gated: <b> / manual: <m>).

### Builders that ran
- `<builder>` — <one-line outcome>

### Validator iterations
- N rounds, all green.

### Acceptance trace
- Automated verified: <count> / <count>.
- Manual bullets recorded for on-device verification: <list of bullet numbers>.

### External review
- Reviewer: `codex` | `internal-reviewer`
- Iterations: N — final verdict clean.

### Security review
- Gate: ran (<N> findings, all resolved) | ran (<N> findings, escalated) | skipped (no matching files) | skipped (Skill tool unavailable)
- Touched files: <comma-separated, or "—" when skipped>

### Runtime verify
- Gate: ran | deferred (manual hint emitted) | skipped (verifyEnabled=false) | skipped (no runtime-observable change)
- Result: PASS / FAIL / N/A — <one-line note; include screenshot path if emitted>

### Spec fidelity
- Gate: ran (<N> screens) | skipped (figmaEnabled=false | no non-`none` ## Design bullets | not a UI task)
- <ScreenName>: PASS (<n> minors → Caveats) | FAIL — <one-line> | SKIPPED — no mock (`## Design`: `none`)   (one bullet per ## Design screen; SKIPPED is the status-neutral no-mock home — does NOT add a Caveats bullet, never flips outcome status)
- Screenshot gate: ran | skipped (figmaEnabled=false | not a UI task) — a declared `## Design` screen whose oracle/capture is missing is a Blocker (routed), NOT a skip
- Screenshot <ScreenName>: PASS (0.94) | Minor (→ Caveats) | Major (route by `screenshotPixelGate`: default `strict` blocks, `advisory` → Caveats, `off` suppresses pixel-similarity findings) | Blocker (routed)   (omit the line only for a non-UI task)

### Build gate
- iOS XCFramework (`:shared:assemble<IosFrameworkName>DebugXCFramework`) — PASS / SKIP (iosEnabled=false)
- `:androidApp:assembleDebug` — PASS

### Manual verify hint (only when gate is `deferred`)
- Run the Anthropic `verify` skill yourself, or manually exercise the app: <verbatim verify intent>

### Open assumptions (if any)
- <one-line per manual bullet, inputs-resolver signature drift, or planner-flagged assumption>
```

Omit `### Manual verify hint` entirely unless the verify gate is `deferred`.

## Step 6a — Validate and hand off the outcome appendix

The final handoff is the `## Outcome` trailer as a draft in the CONTROL cache at
`$ORCHESTRATOR_FINALIZATIONS_DIR/<stem>.$ORCHESTRATOR_WORKTREE_ID.draft.md`
(both variables are pinned in your environment; the same path inside your
execution checkout is a different, throwaway directory and the transaction will
never see it). The worktree id in the name binds the draft to THIS generation —
a draft written by an earlier run describes work this candidate does not
contain, and the transaction refuses it rather than publishing it. Do **not** mutate the
todo file yourself — it lives in the control root and you cannot reach it. The
integration transaction validates the combined task,
persists an internal recovery snapshot, and atomically installs the trailer as
its first phase. **Six headings are always required, in this exact order; `###
Execution log` is parser-optional after `### Files touched`, but REQUIRED for a
Figma-enabled UI task before finalization.** `ship-done` injects only the machine
digest line and will not rewrite the appendix structure or line endings. If a required subsection
has nothing, write exactly one bullet `- none` — except `### Runtime verify`,
which always records its exact `Gate:` and `Result:` pair. Never skip a required
heading or leave a required subsection empty. Do not create that exact final
path yet: first compose the bytes in a private temporary file outside the
candidate tree and complete the mandatory fences below.

Set `$OUTCOME_TMP` to a newly created mode-`0600`, single-link regular temporary
file outside both the execution and control trees (for example, a fresh
`mktemp` result). Never reuse a caller-supplied path, symlink, hardlink or prior
attempt's temp file. Write only the intended appendix bytes there; the exact
generation-bound final path must remain absent until fence 3 succeeds.

```markdown

---

## Outcome

**Status**: completed | completed-with-caveats | partially-completed
**Completed at**: <ISO8601 UTC>
**Reviewer**: codex | internal-reviewer
**Review iterations**: <integer>

### Build gates

- `<gradle task or equivalent>` — pass | fail | skipped (<one-line reason>)

### Runtime verify

- Gate: ran | deferred | skipped (<verifyEnabled=false | no runtime-observable change>)
- Result: pass | fail | n/a — <one-line note; include screenshot path if emitted>

### Acceptance trace

- `<verbatim acceptance bullet, first 80 chars>` — verified | manual | deferred — <one-line note>

The backticks around the quoted bullet are LOAD-BEARING, not decoration: the parsers (site +
ship-done) treat the leading backtick span as opaque, so an acceptance bullet that itself
contains ` — ` (the house style: "Board shows pill — red when expired") cannot shift the
verdict segment. Quote verbatim — never reword a bullet to dodge its em-dash. The verdict is
machine-checked at ship: anything outside `verified | manual | deferred` (canonical:
`contracts/acceptance-trace.md`, machine-readable `acceptanceVerdicts` in
`contracts/outcome-shape.json`) fails ship-done with the exact offending bullet named.

### Caveats

- <one-line caveat>

### Follow-ups

- `TASK_<N>_<title>` — backlog | pending | todo | done

### Files touched

- `<repo-relative path>` — created | modified | deleted | renamed

### Execution log

- Phases: <phases in order; annotate retried ones, e.g. `validators (3 cycles)`>
- Totals: <whole-run duration> · stops <n> · retries <n>
- Design: <screens pulled, or `none`>
- Figma: final <PASS|WARN|BLOCKER|INCOMPLETE> · <n> screenshots · run <id> · report sha256:<short> · <fresh|stale>
- Figma meta: «AUTO — written by `ship-done.mjs`; do NOT hand-type this bullet»
- Spawned: <child / follow-up task stems, or `none`>
```

### Field rules (the site parser reads these literal shapes — drift breaks rendering)

- `---` separator on its own line **immediately precedes** `## Outcome` (the site
  finds the appendix by the **last** `---` in the file). One blank line above and
  below.
- `**Status**`/`**Completed at**`/`**Reviewer**`/`**Review iterations**` each on
  its own line, `**Key**: value`. `Status` is exactly one of:
  - `completed` — every automated acceptance bullet verified or deferred; manual bullets recorded; no Caveats.
  - `completed-with-caveats` — automated bullets verified/deferred and manual bullets recorded, but Caveats non-empty.
  - `partially-completed` — ≥1 structural acceptance bullet not delivered (a
    matching `### Follow-ups` task is **mandatory**).
- `Reviewer` is `codex` / `internal-reviewer` only.
- All Outcome value sets and the required-heading list are canonical
  in `orchestrator/contracts/outcome-shape.json` (runtime-read by
  `regen-index.py` and `ship-done.mjs`; the site imports its generated
  projection) — change them there, never by editing a parser.
- Subsections use the exact documented order. Only an optional final
  `### Execution log` may follow `### Files touched`; unknown, duplicated, or
  reordered subsections are invalid.
- `### Build gates` — one bullet per gradle task you ran (the build-gate
  acceptance line gets its own bullet). Effectively never `- none`.
- **The `tests` gate row is required for every new finalization** and
  uses the existing grammar unchanged: `` - `tests` — pass `` when the sealed
  summary verdict is `PASS`, or
  `` - `tests` — skipped (test-not-applicable: <allowlisted value>) `` when it
  is a proven typed `SKIPPED`. A `fail` value never publishes — the finalizer
  refuses it, the row must byte-match the sealed summary the same transaction
  stages (mismatch = `OUTCOME_TESTS_GATE_MISMATCH`), and a `tests` row without
  a transaction-bound summary is refused as `TEST_EVIDENCE_UNBOUND`. Task-body
  lookalikes are ignored: only the structurally parsed Outcome `Build gates`
  section can supply this row. The full
  digest lives in the typed committed summary, never in Outcome prose — do NOT
  add a seventh heading for it. The interlock is unconditional for every
  forward `Status` (`completed`, `completed-with-caveats`,
  `partially-completed`): flaky is BLOCKED (no WARN class), and the one legal
  bypass is the owner-authorized descope flow (`descope-task.mjs`), which
  shrinks the acceptance obligations BEFORE finalization — a failed test can
  never become a Caveat.
- `### Runtime verify` — **exactly two bullets** in order: `Gate:` (ran/deferred/
  skipped) then `Result:` (pass/fail/n/a). The verdict is `n/a` whenever the gate
  did not run; `pass`/`fail` only when `ran`. Append the verify-skill screenshot
  path to the Result bullet when emitted. Do not write `- none` — the heading is
  exhaustively two bullets.
- `### Acceptance trace` bullets **quote the first 80 chars of the original bullet
  verbatim** (incl. inline backticks), then ` — <verdict> — <note>`. Verdict is
  exactly `verified` (an `### Automated` bullet `acceptance-tracer` matched),
  `manual` (a `### Manual` bullet), or `deferred` (gate-delegated — build-gated /
  spec-gated / screenshot-gated). The transient states `partial`/`missing`/
  `conflicting` MUST NOT appear. The trace is **exhaustive** over `### Automated`
  + `### Manual`. Figma-deferred bullets may carry the machine report in the note:
  `spec PASS report=.cache/figma/reports/spec-compare-<stem>.json hash=<sha256>`
  or `screenshot PASS report=…/screenshot-<stem>.json hash=<sha256>`.
- `### Caveats` — one short line per caveat, ≤120 chars. Canonical shape: `<what
  was done despite a constraint> because <why the alternative did not work>`.
  Long discussions → a follow-up task, not here.
- `### Follow-ups` — one bullet per follow-up (`TASK_<N>_<title>` + column);
  `- none` if none.
- `### Files touched` — every repo-relative path you changed (created/modified/
  deleted/renamed), without summaries or truncation markers: every bullet must
  match the exact path/change grammar. Includes any authorized footprint
  expansion (reason in `### Caveats`).
- `### Execution log` — the **durable digest** of the pipeline journal
  (gitignored), folded in so the run's shape survives a fresh clone. The **last**
  heading, strictly **after `### Files touched`**, and **optional + parser-
  additive for non-UI tasks, but mandatory for a Figma-enabled UI ship** — deliberately NOT in the required-heading set (the canonical
  `headings` list in `orchestrator/contracts/outcome-shape.json`, which every
  parser derives from — keep it out of that list), so a non-UI done file without
  it stays valid. ≤6 bullets. When final
  Figma evidence exists and is fresh, add the human `Figma:` bullet (short). The
  machine `- Figma meta:` bullet is **code-emitted by `ship-done.mjs`** at the move
  (Step 6b) from this run's verdict + real report hashes — **do not hand-type it**;
  `ship-done` injects it (and overwrites any hand-typed one) but never creates
  the heading or normalizes surrounding bytes. It may exceed 120 chars
  to carry full `sha256:<64>` hashes; keep all other bullets short.

Every bullet is one physical line. Free-form caveat text is bounded to 120
characters; build/runtime notes use their explicitly bounded grammar, and
execution-log bullets use the dedicated ≤6-line contract above.

### Mandatory completion fences and publication order

1. Retain the exact current Step-4.4 `test-summary:<sha256>` receipt. Its sealed
   summary MUST be for this task and `runId`, MUST report
   `snapshotVerification: current`, and its verdict MUST be `PASS` or the one
   typed, policy-allowed `SKIPPED`. Recompute it after every later tree change;
   prose or a prior generation's receipt is not evidence. The execution pin is
   owned by the checkpoint in fence 3, not by the test-summary schema.
2. Run the canonical strict parser against the intended bytes in the private
   temporary file — visual heading checks are insufficient:

   ```bash
   node --input-type=module -e '
   import { readFileSync } from "node:fs";
   import { outcomeShapeError } from "./orchestrator/figma/scripts/outcome-shape.mjs";
   const error = outcomeShapeError(readFileSync(process.argv[1], "utf8"));
   if (error) { console.error(error); process.exit(1); }
   ' "$OUTCOME_TMP"
   ```

   Any non-null parser result blocks completion. Repair and validate the bytes;
   never downgrade a strict outcome-shape error to a Caveat.
3. Set `$TEST_SUMMARY_RECEIPT` to the exact current receipt id and
   `$SHIP_ATTEMPT` to the exact current positive ship attempt. Create the
   terminal checkpoint with this complete stdin payload (no omitted or extra
   keys):

   ```bash
   node orchestrator/tasks/task-checkpoint.mjs create --stem "$TASK_STEM" <<EOF
   {"runId":"$ORCHESTRATOR_RUN_ID","phase":"ship","attempt":$SHIP_ATTEMPT,"status":"completed","outputReceiptIds":[],"priorPhaseReceiptIds":["$TEST_SUMMARY_RECEIPT"],"failureCode":null,"retryPolicy":{"kind":"restart-task","safePhase":null,"reasonCode":null}}
   EOF
   ```

   `$TEST_SUMMARY_RECEIPT` MUST be exactly one `test-summary:sha256:<64-hex>`
   id for this run. Empty stdin, an undefined variable, malformed JSON, a
   second summary, or any other receipt set must fail closed. Capture the one
   JSON response and require `ok:true`; then verify `response.checkpoint`
   repeats that `runId` and has a non-null `executionPin` whose
   `worktreeId`, `baseCommit`, `baseTree`, `executionTree`, `targetRef` and
   `targetCommit` all equal the current manager-resolved generation. Missing,
   stale or foreign fields mean no handoff; candidate sealing is required to
   fail closed without this checkpoint.
4. Only after 1–3 pass, publish the already-validated bytes as one sole regular
   file at the exact generation-bound final path. A bare `<stem>.draft.md`, a
   different worktree id, a symlink, hardlink, oversized file or unsafe path is
   not a completion signal. Re-run the same canonical parser against the exact
   final file. If that final read differs or fails, do not claim completion.
   Detach only the exact single-link generation and bytes you just published;
   if exact ownership cannot be re-proven, preserve the observed entry and
   report recovery-required instead of unlinking a symlink, replacement or
   foreign generation. Remove the owned private temp after the final read.
5. Emit the terminal ship journal event with the checkpoint id, post the chat
   summary, and end the turn without any further candidate-tree mutation.

The manager independently re-proves all of this at sealing/integration. These
run-side fences prevent the exact draft signal from becoming visible before the
evidence it promises exists.

**Done is immutable** — do not edit the appendix after it lands in `done/`. To
fix a wrong outcome, use the explicit move-back procedure (see
[`run-loop.md`](run-loop.md) "Move-back").

## Steps 6b–6d — Hand the sealed candidate over to Integrate

**You never publish.** A run works in an isolated worktree against the committed
base; the task file, `INDEX.json`, the architecture map and the mapping
registries all live in the control root, which your checkout cannot write. So
there is no finalizer call in this step, and there is no `ship-done`,
`regen-index`, `regen-arch`, `mv` or `rm` to run by hand.

What you do instead:

1. prove the current test-summary and completed exact-generation `ship`
   checkpoint, then leave the strict-valid exact Outcome draft from Step 6a in
   the control cache;
2. end the turn with the report below. Without all three, leave no exact draft
   and do not claim completion.

The site manager then seals your working tree into ONE candidate commit on the
manager-owned branch, publishes a candidate receipt, and moves the generation to
`ready-for-integration`. The Board raises **Integrate** on the task.

When the owner presses it, the integration transaction runs under a write-ahead
log and does all of the publication in one serialized sequence:

1. re-check its preconditions — the candidate base is still the target tip, the
   control root is clean apart from this task's own source, the identity is
   configured — and refuse with the exact blocking paths otherwise;
2. apply the exact candidate diff into the control root's index and worktree;
3. run the finalizer's PREPARE half: install and revalidate the Outcome, run the
   Figma component/token binding phases, publish the task into `done/` through
   the transaction-authorized `ship-done`, regenerate and check `INDEX.json` and
   the architecture map, and verify every derived artifact — all while keeping
   the task lock and the recovery marker;
4. create ONE canonical commit on the exact target base, with your repository's
   own configured git identity, carrying exactly the candidate paths plus the
   artifacts prepare produced, with every repository hook run;
5. run the finalizer's CONFIRM half: re-prove the branch, parent, commit and the
   published artifacts against that commit, then release the lock and remove the
   marker last;
6. release the worktree and delete the candidate branch.

Every phase records intent before effect, so a crash at any boundary leaves the
transaction resumable from physical state — **Resume integration** on the Board,
or `node orchestrator/tasks/task-worktree.mjs integrate --stem <STEM>`. Nothing
is ever rolled back and no user change is stashed or committed to clear a
blocker.

If a gate refuses during prepare, the transaction stops with the lock and marker
intact: route the finding back to Step 4, repair the task in a new run, and the
owner integrates again.

`done/` remains the single source of truth for what shipped. If the task ends in
escalation with no completion claim, do not create the Outcome draft and do not
start finalization; leave it in `todo/` with its pipeline lock.
