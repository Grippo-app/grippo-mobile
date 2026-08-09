# Ship — Step 6: chat summary + `## Outcome` appendix (6a–6d)

Self-contained orchestrator finalization. When every gate is green, do all of
this **in the same turn**: post the chat summary → 6a write an Outcome draft →
6b–6d hand the complete publication to `finalize-task.mjs`. The finalizer owns
the atomic Outcome install, component/token binding phases, sanctioned move, derived
artifacts, verification, and lock release. The happy path ends only when it
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

## Step 6a — Write the outcome appendix

Write the `## Outcome` trailer as a draft at
`orchestrator/.cache/tasks/finalizations/<stem>.draft.md`; do **not** mutate the
todo file yourself. `finalize-task --outcome-file` validates the combined task,
persists an internal recovery snapshot, and atomically installs the trailer as
its first phase. **Six headings are always required, in this exact order; `###
Execution log` is parser-optional after `### Files touched`, but REQUIRED for a
Figma-enabled UI task before finalization.** `ship-done` injects only the machine
digest line and will not rewrite the appendix structure or line endings. If a required subsection
has nothing, write exactly one bullet `- none` — except `### Runtime verify`,
which always records its exact `Gate:` and `Result:` pair. Never skip a required
heading or leave a required subsection empty.

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

**Self-check the draft parses, BEFORE Step 6b (`finalize-task`).** Confirm all six
required `### ` headings exactly — `Build gates`, `Runtime verify`, `Acceptance
trace`, `Caveats`, `Follow-ups`, `Files touched` (PascalCase; add the seventh
`### Execution log` whenever the Figma UI gate applies) — plus a `**Status**` line and a `**Reviewer**` line with the
exact key casing. A missing/mis-cased heading/key silently flips the badge to
`malformed`. `finalize-task` and its nested `ship-done` interlock both enforce
this mechanically (exit 1 with the exact malformed reason) — but self-check
first: the interlock catching it costs a re-run.

**Done is immutable** — do not edit the appendix after it lands in `done/`. To
fix a wrong outcome, use the explicit move-back procedure (see
[`run-loop.md`](run-loop.md) "Move-back").

## Steps 6b–6d — Run the recoverable finalizer

**Do not call `ship-done`, `regen-index`, `regen-arch`,
`mv`, or `rm` separately, and never write `component-mappings.json` by hand.** Invoke the single transaction with the draft from
Step 6a:

```bash
node orchestrator/tasks/finalize-task.mjs "$STEM" \
  --outcome-file "orchestrator/.cache/tasks/finalizations/${STEM}.draft.md"
```

The finalizer persists a write-ahead marker under
`orchestrator/.cache/tasks/finalizations/` plus an immutable, content-addressed
Outcome snapshot and executes these idempotent phases:

1. atomically install and revalidate the Outcome;
2. when Figma is enabled, run the `components` and `tokens` binding phases: a
   design-origin component task's frozen binding evidence is validated and its
   binding-authorized mapping is published into
   `orchestrator/figma/component-mappings.json` after a staged validation
   compare; a generic task only passes the touch-scoped extraction regression
   gate (it never auto-maps);
3. call the transaction-authorized `ship-done.mjs`, whose final driver re-pins
   census after a consulted mapping/inventory projection changes, stages receipts/digest,
   publishes an immutable transaction-owned task in `done/` without
   overwriting an existing file, then commits the staged receipts;
4. invoke the finalizer-owned canonical INDEX publisher and structurally check
   its exact `INDEX.json` receipt;
5. regenerate and check the architecture map (cleanly non-applicable before
   bootstrap);
6. verify the task exists only in `done/`, Outcome/INDEX/arch/mapping-registry
   state are fresh, and a UI task's ship receipts bind to the current task;
7. atomically detach the task lock only when its identity and bytes still match
   the lock captured at transaction start, retaining a durable proof across the
   unlink crash window;
8. mark complete, reverify the exact done bytes, and delete private publication,
   receipt, lock, snapshot, and marker proofs last (the marker is final).

Each mutating phase records intent before its effect. A crash, terminal close,
or server restart therefore leaves the lock and marker in place. Resume with the
same command without `--outcome-file`, or use **Resume finalization** on the
Board; it continues from physical postconditions and never starts the AI task
again. Do not clear a finalization lock manually — the server rejects that while
a marker exists (and blocks competing task mutations during the pre-marker
global-mutex window too).

**Exit 0** means the complete transaction verified and cleaned up. **Exit 2**
means `ship-done` hit a Figma/task-shape gate: keep the marker+lock, route the
finding to Step 4, repair the same todo task, then invoke the finalizer again.
It accepts a newly valid pre-ship task body, refreshes the durable intent, and
replays the components phase and ship onward. **Exit 1** is an exact phase/precondition failure;
fix that cause and rerun the same finalizer. Never bypass it with a hand move.

`done/` remains the single source of truth for what shipped. If the task ends in
escalation with no completion claim, do not create the Outcome draft and do not
start finalization; leave it in `todo/` with its pipeline lock.
