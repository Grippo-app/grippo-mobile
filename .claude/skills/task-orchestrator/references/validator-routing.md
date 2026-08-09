# Validator routing + gates — Steps 4, 4.5, 4.6, 4.6b, 5, 5.5

Self-contained orchestrator validator wave, the assemble / verify /
screenshot gates, and the reviewer + security gates. Read after
[`run-loop.md`](run-loop.md) Step 3.5.

**Every numeric cap below is frozen** — match it exactly against
[`orchestrator/contracts/orchestrator-loop.md`](../../../contracts/orchestrator-loop.md);
never re-derive loosely. The validator-invocation envelope (always-on three +
fail-closed conditionals) is pinned in
[`validation-run.md`](../../../contracts/validation-run.md); the reviewer
output shape in
[`reviewer-output.md`](../../../contracts/reviewer-output.md).

## Step 4 — Run validators (PARALLEL)

Once all builders in the plan have reported done, run **every applicable**
validator in a single multi-Agent message.

**Pass `TASK_STEM` to every spawned validator and reviewer.** They run in
separate sessions and cannot see the orchestrator's shell var, yet several read
the pre-task baseline files on their own. Prepend the literal token
`TASK_STEM=<stem>.` to each prompt (e.g. `TASK_STEM=TASK_3_add_filter.`) so each
spec reconstructs `/tmp/orchestrator_pre_task_sha_${TASK_STEM}.txt`; absent the
token they fall back to `HEAD~1`/`HEAD`. Applies to `scope-leak-validator`,
`mvi-contract-validator`, `build-validator`, `anti-pattern-scanner`,
`acceptance-tracer`, `architecture-validator`, and the Step 5 / 5.5 reviewers.

For `build-validator`, also prepend `BUILD_MODE=compile.` — it parses the token
from the prompt text (the parent does not export it as a shell env var):

```
Agent(subagent_type: "build-validator", prompt: "BUILD_MODE=compile. TASK_STEM=<stem>. Run Step 1 (compile path) and Step 2.5 (deprecation extraction) per your spec.")
```

**Always include the three always-on validators** regardless of what task-intake
returned (they run in the same parallel group as the conditionals):

- `build-validator` (mode=compile) — fast Kotlin compile + KSP on changed files,
  no R8/dex/lint/packaging or `xcodebuild` (that is Step 4.5).
- `scope-leak-validator` — enforces the `Inputs`/`Acceptance`/`Out of scope`
  boundary the user wrote.
- `acceptance-tracer` — traces each `## Acceptance` bullet to its match in the
  diff. Pass it the Step-2a plan verbatim so it uses the plan's `## Acceptance
  mapping` as its fast path.

For `figma-spec-validator` (conditional — only when Step 1b ran), pass the stem,
the shared `FIGMA_PIPELINE_RUN_ID`, and the plan (its screens↔owner-builder
mapping). Its procedure and output contract live in the `validation-gates`
skill, `references/spec-fidelity-gate.md` — it MUST run the machine baseline
(extract-compose-model, then `compare-screen-spec.mjs <stem>
--impl-model <implementation-model> --gate` with the relevant `--impl-file` /
`--screen-map` inputs) and author `orchestrator/.cache/figma/reports/spec-<stem>.json`
**via `write-spec-report.mjs <stem> --screen … --issue …` (never a hand-assembled
envelope — the CLI computes overall/counts/run-id/hashes and schema-validates
before writing)**; the final evidence bundle hard-requires
both that report and `spec-compare-<stem>.json`, so a skipped baseline deadlocks
Step 6b. Its
**Blocker/Major** findings route like any validator finding; its **Minor**
findings are advisory (suggestion-only) — do NOT route them; copy each
as one `### Caveats` bullet in Step 6a. A `COMPONENT_COLOR_DELEGATED` WARN is
likewise advisory and is NEVER routed to the screen builder: it says the token
binding lives inside a reused design-system component, so the finding belongs
to (and dedups with) that component's own gates — routing it at the screen
would violate the one-caller rule. Canvas/DrawScope widgets need no special
routing: their hoisted tokens are reconciled by the gate's evidence alias
resolution (a correct one PASSes like any declarative widget), so there is no
canvas-specific WARN — a real token defect is a normal Blocker.

### Footprint scoping (only when Step 3.5 found pre-existing uncommitted work)

When the footprint in `/tmp/orchestrator_task_footprint_${TASK_STEM}.txt` is a
strict subset of the whole-tree `git status`, three validators reason about *the
diff* and would raise false positives on files this task never touched —
`scope-leak-validator`, `anti-pattern-scanner`, `acceptance-tracer`. Brief each
with the explicit current-task file list and instruct it to scope to those paths
only.

`build-validator` is a **split case**: its compile and assemble gates (Step 1 /
Step 4.5) read **final** file state and are unaffected by the footprint, but its
Step 2.5 deprecation extraction is **diff-scoped** (it diffs against
`PRE_TASK_SHA`) — so pass the footprint to both build-validator invocations with
the intersect instruction for the Step 2.5 filter only:

```
Agent(subagent_type: "build-validator", prompt: "BUILD_MODE=compile. TASK_STEM=<stem>. Run Step 1 (compile path) and Step 2.5 (deprecation extraction) per your spec. Current-task footprint (intersect your Step 2.5 diff-filter with these paths before routing deprecation warnings; the compile gate is unaffected): <verbatim paths from /tmp/orchestrator_task_footprint_${TASK_STEM}.txt>.")
```

The read-final-state validators (`architecture-validator`,
`mvi-contract-validator`, `naming-convention-validator`, `di-validator`,
`compose-stability-validator`, `data-layer-validator`) are unaffected — do not
scope them. When the footprint equals the whole-tree diff, pass nothing extra and
run all validators normally.

### Dedup, adjudication, and routing

**Dedup before routing.** Validators run in parallel and may flag the same
`(file, rule_id)` from different angles. Build a `Map<(file_path, rule_id),
Finding>` keeping the highest-severity entry per key — same `(file, rule_id)`
from two validators counts as one. Group the deduped set by `routed_to` builder
and send each builder one consolidated message — never N round trips.

**Adjudicate conflicts against the skill rules, don't blind-route.** Dedup
collapses two validators flagging the *same* `(file, rule)`; it does nothing for
two validators reaching *opposite* verdicts, or a validator that *approves*
something a skill reference forbids. When that happens, open the cited
skill reference and decide against it — the rule outranks any validator that justifies
itself by pointing at existing (possibly drifted) code as "the established
pattern". Record the adjudication (which verdict won + the settling rule) as a
`### Caveats` bullet in Step 6a.

**Necessary footprint expansion is allowed; record it.** Honoring a finding
sometimes requires touching a file beyond the named `## Inputs` — most often the
direct caller of a signature the fix changed. When the change is a mechanical
consequence inside a module the task already authorized, authorize the
responsible builder to make it (you still do not write it yourself). The
`scope-leak-validator` flag on that caller is then adjudicated as allowed. Do
**not** expand beyond mechanical necessity, and **never** into an unauthorized
module — that remains a scope leak and routes back as a revert. Record every added
file in `### Files touched` and the reason in a `### Caveats` bullet.

Validators that emit ERROR/WARNING/INFO (`backend-contract-drift`, `figma-drift`,
`figma-component-coverage`) map ERROR→high / WARNING→medium / INFO→informational;
the two suggestion-only validators (`figma-drift`, `backend-contract-drift`)
never enter the routing pool regardless of severity, but a
`figma-component-coverage` ERROR DOES route — to `design-system-component-builder`.

If any validator returns 0 findings AND `build-validator` (mode=compile) is green
AND no other validator has high-severity findings → proceed to **Step 4.5**.
Otherwise group findings by builder, send one consolidated follow-up per builder,
let them fix, re-run validators (goto Step 4).

### Caps (frozen — see `orchestrator-loop.md`)

- **Rotation cap — cap by identity, not by count.** After each cycle compute
  `unique_findings_set = set((file, rule_id))`. If this set does **not shrink
  across 2 consecutive cycles**, escalate — a builder is fixing one issue and
  breaking another. Three iterations of a *shrinking* set is fine; two iterations
  of a *stable* set is escalation.
- **Counter scope.** The rotation counter is scoped to a **single Step 4 entry
  session**. When re-entering Step 4 from a downstream FAIL (4.5 / 4.6 / 5 /
  5.5), **reset** `unique_findings_set` history — each fresh entry starts the
  rotation counter at zero.
- **Outer per-task re-entry cap.** Maintain a second, outer counter distinct from
  the per-session one: initialise `step4_reentries = 0` at the first Step 3 entry
  and increment it **each time any downstream gate triggers `goto Step 4`** (the
  first Step 3 → Step 4 does not count). It is **never reset mid-task**. When
  `step4_reentries` reaches **7**, escalate: `BLOCKED[general]: Step 4 re-entered
  <N> times — the change cannot converge through the full pipeline…`. Leave the
  per-session rotation detection unchanged — it catches a builder oscillating
  *within* one session; this outer cap catches the orthogonal failure where each
  circuit is clean-then-broken-downstream.

## Step 4.4 — Deterministic test certification

Runs exactly once per Step-4 entry, after the validator wave converged in
`mode=compile` and BEFORE the assemble gate. This step is a deterministic
security boundary, not a Claude validator: the orchestrator invokes the
canonical Node CLIs and never lets task text, planner prose or builder output
form a shell command.

1. **Observed impact.** Recompute the actual content footprint through the
   canonical byte-content snapshot (`orchestrator/tasks/content-snapshot.cjs`)
   and materialize `test-impact.observed.json` with
   `node orchestrator/tasks/resolve-test-impact.mjs` (planner proposal +
   footprint facts). The observed artifact may only keep or widen the sealed
   planned impact (`checkWidening`); a widening returns the task to the owner
   builders BEFORE any certification run, and narrowing is impossible by
   contract.
2. **Certification runs.** For each required tier and lane, the deterministic
   orchestrator writes the strict request artifact and invokes exactly
   `node orchestrator/tasks/run-test-certification-request.mjs --request <relative-json>`.
   That caller verifies the exact task-lock tuple/hash before driving the
   receipt producers in `run-test-certification.mjs`; it executes ONLY
   allowlisted Gradle task paths taken from the published capability
   inventory / root aggregates — direct new/changed identities first, then
   owner-module suites, then the affected consumer closure, then required
   platform lanes; the full suite when the observed impact demands it. Direct
   tier never accepts `FROM-CACHE`/`UP-TO-DATE`; `--continue` is diagnostic
   only and never flips a failed leaf.
   Typed N/A gates carry only policy gate ids; executable/tool/argv come from
   the producer's closed mapping and never from the request.
3. **Receipts → sealed summary.** Every command/gate yields immutable
   started/terminal receipts under
   `orchestrator/.cache/tasks/test-certification/<stem>/<run-id>/`;
   `evaluateCommandReceipt` turns zero-discovered, `NO-SOURCE`-with-required-
   tests, all-skipped, timeout and fail→pass-retry into typed violations.
   `aggregate-test-certification.mjs` enumerates all receipts, proves their
   started→terminal links and sealed report/output hashes, rechecks impact and
   source snapshots, and derives every summary field itself; no API accepts a
   caller-supplied verdict or summary. The
   sealed `summary.json` (verdict `PASS|SKIPPED|BLOCKED|FAIL`) is the ONLY
   authority the tracer, finalizer and Site consume — a builder report is a
   claim, never evidence.
4. **Checkpoint + journal.** Emit journal phase `tests`; the phase checkpoint
   stores terminal receipt ids only after the summary is sealed (a `started`
   receipt is never reusable). Infrastructure failures (missing emulator/
   simulator, toolchain, disk) are `BLOCKED[tests-infrastructure]` — the task
   stays in `todo/`, never a skip and never a PASS; test failures route back
   into Step 4 through the same outer re-entry counter (cap **7**). The single
   diagnostic retry happens inside this invocation and can never change a
   verdict (flaky = BLOCKED).
5. **Acceptance hand-off.** `acceptance-tracer` maps every `test:` anchor to
   discovered identities + receipt hashes from the sealed summary: fresh PASS
   → `verified`; test exists but does not prove the bullet → `partial`;
   contradicting assertion → `conflicting`; failed/stale required lane keeps
   blocking. Manual bullets never close from a test PASS.
6. **Finalization binding.** The Outcome must carry exactly one `` `tests` ``
   build-gate row. Before publishing its marker or immutable Outcome snapshot,
   `finalize-task.mjs` discovers `summary.json` from the exact active
   orchestrator lock run, reconstructs every context/started/terminal receipt,
   recalculates `test-task-input` from the pre-Outcome bytes, and reverifies
   the current source snapshot. The marker freezes task/source/policy/summary,
   run/session, and raw lock hashes as one all-or-none binding; every recovery
   verify repeats it. `` `tests` — skipped `` is accepted only with the exact
   typed ``test-not-applicable: <policy-enum>`` reason. A missing row, standalone
   self-hashed summary, missing receipt, stale task/source, foreign run/session,
   or FAIL/BLOCKED verdict leaves the task in `todo/`.

## Step 4.5 — Full assemble gate

Step 4 converged in `mode=compile`; compile-only does not exercise R8 shrinking,
dex packaging, lint, resource processing, the iOS XCFramework link, or
`xcodebuild :iosApp`. Run `build-validator` once more in `mode=assemble`:

```
Agent(subagent_type: "build-validator", prompt: "BUILD_MODE=assemble. TASK_STEM=<stem>. Run Step 1 (full XCFramework + assembleDebug), Step 2 (ancillary module assembles), Step 2.5 (deprecation extraction), Step 3 (xcodebuild :iosApp when iosEnabled=true), per your spec.")
```

(Apply the same footprint/intersect instruction as Step 4 when pre-existing work
exists — only the Step 2.5 filter is affected; the assemble gate reads final
state.)

If PASS → proceed to Step 4.6. If FAIL → the failure is in territory
the compile gate does not cover (R8 keep-rule conflict, missing resource,
manifest merge, native link/XCFramework export, lint blocker). (Per-module
compiles on KMP library modules use the project-config `moduleCompileTask`
suffix — `compileAndroidMain` by default; the `:androidApp`-style
`compileDebugKotlin` variants do not exist on them.) Route to the
responsible builder using the same `(file, rule_id)` dedup as Step 4 — the file
in the assemble error owns the failure; a project-wide config issue is tagged
`build-system` and surfaced to the user (no builder owns it). After the fix, goto
Step 4 — the loop must re-converge in compile-mode before retrying the assemble
gate.

**Cap: 2 *consecutive* assemble-mode FAILs per task.** The counter tracks only
the FAIL→retry circuit *from Step 4.5 itself* — not retries originating
downstream and passing back through. A PASS in between **resets the counter**.

- 4.5 FAILs → builder fix → Step 4 re-converges → 4.5 fires again; if the second
  invocation also FAILs (especially same root cause), escalate. That is the cap.
- 4.5 PASSes → counter resets to 0. A downstream gate FAIL routing through 4.5 a
  *third* time later is fine — the FAIL→FAIL consecutive condition was broken by
  the intervening PASS.
- A second FAIL on the immediately-following invocation with a *different* root
  cause still escalates — two consecutive FAILs is the signal that the change
  cannot reach assemble-green in this task.

Why not run `assemble` inside Step 4 every cycle? `assembleDebug` + `xcodebuild`
is the slowest gate (1–5 min). Promoting it out of the loop means the typical
2–3 rounds incur only the per-module compile (~30s–1min; `moduleCompileTask`
suffix from project-config, default `compileAndroidMain`) and the full assemble
fires once at convergence.

## Step 4.6 — Runtime verify (optional gate — Anthropic `verify` skill)

Read `verifyEnabled` (default `auto` when absent) and detect `Skill`-tool
availability by inspecting your own tool budget at runtime. **Auto by default**:
it runs when the top-level session that adopted this playbook has the `Skill`
tool enabled; otherwise it is recorded as deferred. Only `verifyEnabled:false`
disables it. Editing any frontmatter does nothing — the parent adopts the role,
its own tool budget is what counts.

Routing matrix:

| verifyEnabled | `Skill` available | Action |
|---|---|---|
| auto *(or absent)* | yes | invoke `Skill(skill: "verify", args: "<verify intent>")` |
| auto *(or absent)* | no  | skip; emit a `### Manual verify hint` block in the summary; record gate `deferred` in the appendix |
| true | yes | invoke as above |
| true | **no** | **HALT** — escalate: *"verifyEnabled=true but Skill tool unavailable. Enable a runtime that exposes Skill, or set verifyEnabled to `auto`/`false`."* Do not silently fall back. |
| false | *(skip detection)* | skip; record `skipped (verifyEnabled=false)` |

**Verify intent** — one sentence describing the user-observable change, derived
from `## Goal` + the first `### Manual` acceptance bullet. If the task has zero
`### Manual` bullets AND no runtime-observable behaviour in `## Goal` (pure
infra/codegen — e.g. a mapper change), skip the invocation and record `skipped
(no runtime-observable change)` even when `auto` + Skill available.

On **FAIL**, treat like a validator finding: map to a responsible builder via the
plan's `## Acceptance mapping` → `### Automated` `Owner builder` column (a failing
manual behaviour maps through the `### Automated` structural bullet that delivers
the screen/route it exercises; if none corresponds — e.g. crash at startup —
route to **all** builders that ran). Goto Step 4. On **PASS**, proceed to Step 4.6b
when it applies; proceed to Step 5 only after screenshot fidelity has passed or
self-skipped as non-UI.

**Turn discipline** — `Skill` executes inside your own session; the skill's
output instructions bind the **report it emits**, not your turn. Capture the
report as the gate's return value, then keep executing this playbook in the same
turn. **Case-mapping:** the skill returns uppercase `PASS/FAIL/SKIP`; write
lowercase `pass/fail/n/a` into the appendix `Result:` bullet and uppercase
`PASS/FAIL/N/A` into the chat summary.

**Cap: at most 3 verify invocations per task.**

## Step 4.6b — Screenshot fidelity (Roborazzi + Figma oracle, conditional)

After Step 4.6 (or after Step 4.5 when 4.6 was skipped). No Skill-tool
dependency — runs Roborazzi locally via Gradle and calls
`compare-screenshots.mjs`.

**Step 4.6b·0 — capture-config gate (BEFORE the Roborazzi render).** The
`@Config(qualifiers = "w<W>dp-h<H>dp")` is DERIVED from the oracle `frameSizeDp`
(fidelity-gate §3), so it is verified mechanically, not left to the builder's memory —
this is what keeps an already-written test from silently rendering at the ~320×470 host
default (the stale-vendored-copy failure). Run
`node orchestrator/figma/scripts/check-capture-config.mjs <stem> --gate` first. It reports
`CAPTURE_CONFIG_MISSING` / `CAPTURE_CONFIG_DRIFT` before an expensive Roborazzi render — the
locale segment included (the design language is derived from `designLocale` config or spec
texts × string resources; a wrong/absent locale segment is the same DRIFT/MISSING class); route
those findings to the owner builder to update `ScreenshotTest.kt` / `@Config`. The orchestrator
does not run `--fix` here: validators are read-only in the run loop, and capture test edits must
stay inside builder ownership. `CAPTURE_LOCALE_UNDERIVABLE` (votable design text, no decisive
locale, no declaration) is NOT a builder code fix — surface it to the user: the remedy is an
owner config line (`designLocale:` in project-config) or richer design text layers. If it
reports `CAPTURE_TEST_ABSENT` (a `## Design` bullet no
`@Test` captures), that is also the builder's gap — route it back (the screenshot gate will block
it as `MISSING_CAPTURE` anyway). Only after this gate is green do you record the Roborazzi captures.
 **Mandatory** whenever `figmaEnabled: true` and the task
carries a non-`none` `## Design` bullet — a pulled oracle (any theme) + its Roborazzi
capture are REQUIRED inputs, **not** run-conditions. **No separate enable flag.** It
self-skips ONLY for a non-UI task (no `figmaEnabled` / no non-`none` `## Design`
bullet — but a `figmaEnabled` task that cites a Figma node URL or embeds a
`designComponentId:`+`figmaNodeId:`/`frozenStructuralHash:` component snapshot yet omits
`## Design` is NOT non-UI: the `ship-done`/`verify-done` UI-by-evidence backstop
hard-blocks those before `done/`. A screen/dialog FILE edit with no node cited only
draws an advisory NOTE there — that filename-only class is an accepted residual, not a
netted case). When a `## Design` screen IS declared but its oracle
or capture is missing, that is a **BLOCKER** (`MISSING_ORACLE` / `MISSING_CAPTURE`), never a silent skip —
the comparison must run. Invoke `compare-screenshots.mjs` with **`--gate`**.

```
Agent(subagent_type: "figma-screenshot-validator",
      prompt: "Task: <task content>. Stem: <stem>. Implementation plan: <plan path>. Affected modules: <module list from Step 4.5>. Census/screen map: <census report path or none>. FIGMA_PIPELINE_RUN_ID=<same id from Step 1b>. Run the screenshot-fidelity gate in --gate mode (see the validation-gates skill, references/screenshot-fidelity-gate.md). Preferred: one driver command — `node orchestrator/figma/scripts/run-figma-gates.mjs <stem> --stage screenshot --modules <:module,:module>` (it runs check-capture-config --gate with the --fix self-heal, exports SCREENSHOT_CAPTURE_STARTED_AT itself, records via recordRoborazziAndroidHostTest, derives ROBORAZZI_OUTPUT_DIRS, emits the nodeId capture manifest, then compare-screenshots --gate). Debug path (identical semantics): `check-capture-config.mjs <stem> --gate`, then Roborazzi for the affected modules, then `compare-screenshots.mjs <stem> --gate`.")
```

| Result | Action |
|---|---|
| PASS | Proceed to Step 5. |
| Minor | Proceed to Step 5; copy findings to `### Caveats`. |
| Major | Follow `screenshotPixelGate`: under the default `strict`, a Major blocks and routes to the owner builder; under `advisory`, proceed to Step 5 and copy findings to `### Caveats`; under `off`, pixel-similarity findings are suppressed but completeness/anti-forgery blockers still apply. |
| Blocker (incl. `MISSING_ORACLE` / `MISSING_CAPTURE`) | Route to owner builder (same `(file, screen)` dedup as Step 4). Goto Step 4. The comparison MUST run before done. |
| SKIPPED — non-UI (no `figmaEnabled` / no `## Design` screen) | Proceed to Step 5 (nothing to compare). Record in `### Spec fidelity`. |
| SKIPPED — a `## Design` screen is declared but its capture/oracle is missing | **Treat as Blocker**, not a pass — a declared screen cannot ship uncompared. Route as above, goto Step 4. |
| BLOCKED (Roborazzi not wired) | Surface: *"add `id("screenshot.test.convention")` + ScreenshotTest.kt"* — but the builder scaffolds this by construction (implement-figma Step 5), so it should not occur. Goto Step 4. |

**Routing a Major/Blocker back to the builder:** pass the report's **`fixBrief`**
(also printed under `Fix first:` in the gate output) verbatim as the HEAD of the fix
prompt — it is the ranked "start here" digest (capture wiring → deciding zone →
whole-frame band → colour axis), so the builder fixes the verdict-deciding element
first instead of re-deriving it from the full report.

Record the verdict in the chat summary's `### Spec fidelity` block (one bullet
per screen). **Do NOT add a new `### Screenshot fidelity` heading to the outcome
appendix** — the parser expects exactly the six `###` headings (+ optional
`### Execution log`). Pixel-similarity routing follows `screenshotPixelGate`:
the default `strict` blocks Major, `advisory` records Major in `### Caveats`, and
`off` suppresses pixel-similarity findings while completeness/anti-forgery
blockers still route back through the fix path.

**Cap: at most 3 invocations per task.**

Before Step 5, when Step 1b ran, run `node
orchestrator/figma/scripts/run-figma-gates.mjs <stem> --stage final` (preferred —
it first re-runs `component-census` under the pinned run id when a CONSULTED
mapping/inventory projection changed, then the final bundle; the raw
equivalent is `node orchestrator/figma/scripts/evidence-bundle.mjs <stem>
--stage final --fresh`) and
**require exit 0** — final mode fails closed when any of screen-cache, check-spec,
capture-config, census, spec, spec-compare, or screenshot is missing/`SKIPPED`/stale (all BLOCKER at
`final`). A non-zero exit ⇒ the screenshot comparison did not run/pass ⇒ goto Step 4,
do not advance. The refreshed `evidence-<stem>.json` supersedes the pre-build bundle
and is the SAME bundle the Step-6b `ship-done.mjs` interlock re-runs before the
todo→done move (see the outcome-appendix reference) — they are one fail-closed
contract, not two. `ship-done` also writes the code-emitted `- Figma meta:` digest and
the headless `verify-done.mjs` auditor catches any task that reached `done/` without it.

When the final bundle blocks, route by the blocker kind. The first, second and fourth
rows are BUNDLE-emitted kinds — read `issues[].issueKind` in `evidence-<stem>.json`;
the `CAPTURE_*` row's witness lives in the SCREENSHOT report — when the bundle is
BLOCKER with no matching bundle issue, read `issues[].issueKind` /
`results[].status` in `screenshot-<stem>.json`:

| Final-bundle blocker | Action |
|---|---|
| `THRESHOLDS_WEAKENED` / `THRESHOLDS_UNRECORDED` / `METRIC_MISMATCH` | A screenshot report does not prove the required judge configuration — unset threshold overrides and re-run the compare + bundle. |
| `LOCALE_ENV_OVERRIDE` | The compare ran under the fixture-only `FIGMA_DESIGN_LOCALE` / `FIGMA_SUPPORTED_LOCALES` / `FIGMA_STRING_RESOURCE_ROOTS` env override(s), which can redirect or disarm the capture-locale witness — unset them and re-run the compare + bundle (same class as `THRESHOLDS_WEAKENED`; they may not certify a task). |
| `DESIGN_CHANGED_SINCE_CHECK` | The task's `## Design` changed after the screen-cache gate — re-run the `figma:screens` pull, then re-run the gates under the same run id. |
| `CAPTURE_IS_ORACLE_COPY` / `CAPTURE_PATH_UNCONTAINED` | The capture is not a real app render (copied oracle / uncontained path) — fix the ScreenshotTest/manifest; never point outputs at the cache. |
| `CAPTURE_LOCALE_MISMATCH` | The capture rendered a different LANGUAGE than the design (the manifest `localeTag` vs the derived/declared design locale) — run `check-capture-config --fix` (locale-aware, patches the `@Config` locale segment) and re-record, then re-run the compare + bundle. NOT a builder code fix beyond the `@Config` repair, NEVER a threshold change, and never a re-pull of a "convenient" oracle. |
| `CAPTURE_LOCALE_UNDERIVABLE` | The design language could not be derived (votable spec text, no decisive locale match) and `designLocale` is not declared — an OWNER config action, not a builder fix: add `designLocale: <locale>` to `orchestrator/project-config.md` (one of `supportedLocales`) or enrich the design's text layers, then re-run `check-capture-config --gate`. |
| `ASPECT_MISMATCH` whose message names `IOS_CHROME_SUSPECTED` | The un-normalized oracle likely embeds iOS device chrome (the "9:41" status bar / home indicator, a 40–90dp one-sided offset) — re-pull the screens (the `figma:screens` session runs `normalize-oracle` mandatorily, stripping the chrome at the pull boundary), or rename the Figma layer if the strict predicate missed it. NEVER a builder code fix, never a capture crop, never a threshold change. `CHROME_CROP_*` / `SPEC_PNG_ASPECT_MISMATCH` blockers at the screen-cache gate carry the same remedy: re-pull. |
| `DESIGN_SOURCE_HASH_MISSING` | Re-run `check-screen-cache`. |
| `REPORT_STALE_RUN` | One report was written under a different run id (the blocker message names both ids and the pin file) — re-run ONLY that report under the pinned id, then the final bundle. NOT a re-pull. `run-figma-gates.mjs` prevents this by construction. |
| `REPORT_INPUT_HASH_MISMATCH` | A report's input changed after it was written. On `census` this is the mapping-registry ordering (a CONSULTED mapping/inventory projection changed): re-run `component-census.mjs <stem>` under the pinned id, then the final bundle — `run-figma-gates.mjs --stage final` does exactly this automatically. On other reports: re-run that one gate, then the bundle. |
| `PIXEL_REVIEW_REQUIRED` / overall `REVIEW_REQUIRED` | A canvas/glass-classed component row awaits the OWNER's pixel verdict because the metric is blind on that class. NOT a builder fix and NOT an agent decision: leave the task in todo and surface it; the owner reviews in the site's evidence tab (three verdict buttons) and the receipt re-runs the bundle. NEVER write `orchestrator/tasks/evidence/pixel-review/` receipts yourself — an agent-authored receipt is a forged review. |
| `PIXEL_REVIEW_FAILED` | The owner REJECTED a reviewed row — the issue message carries their note. Route it to the owner builder like any visual finding, fix, and re-run the pipeline (the new render voids the old receipt by hash). |
| `CLASS_ROUTING_STALE` | A row was routed to review under a renderClass the LIVE mapping registry no longer carries (the owner tightened it back to strict). Re-run `compare-screenshots` under the pinned run id, then the final bundle. |

**False-positive doctrine.** A machine false positive is fixed at the
DETECTOR (the way the locale and device-chrome traces were — derive the invariant
or normalize it away), or routed to the owner's review receipt when the metric is
provably blind (renderClass routing) — it is **never adjudicated around**: no agent
edits a gate script mid-task, hand-authors a report, or waives a finding. There is
no waiver mechanism; if standing exceptions ever become necessary, that is an
owner-authorized design change, not a run-loop improvisation.

**Policy authority.** `done/` tasks are immutable receipts, never gate
precedent. Do not mine `done/` files to decide whether a finding
is blockable or a shape is acceptable: the committed gate scripts, the committed
`screenshot-thresholds.json` (its `version` is the gate-policy version, stamped
into every report as `gatePolicyVersion`) are the only authorities.

If a prior partial/manual run left a **half-state** (a prebuild bundle beside a
fresh screenshot report, mixed-run reports, or a stale bundle — the site shows it as
`SUPERSEDED`/`MIXED_RUNS`/`STALE`/`NOT_RUN`, never authoritative), run `node
orchestrator/figma/scripts/evidence-clean.mjs <stem> --bundle-only` to delete only the
stale bundle + digest while preserving the seven gate reports, then re-run the final bundle
from those reports. Use plain `evidence-clean <stem>` only when intentionally discarding
all gate reports. Never reuse a prebuild bundle as final.

## Step 5 — External review (Codex or internal-reviewer)

Reviewer-agnostic: either Codex (cross-provider) or `internal-reviewer`
(Claude-backed local fallback). Pick **one** before every review attempt and
lock that choice for the attempt. Read `codexEnabled`, then run the shared,
non-billable detector:

```bash
node orchestrator/tasks/reviewer-status.cjs
```

Consume only its JSON contract (`availability`, `installed`, `checkedAt`,
`reasonCode`, `detectorVersion`, `source`). Do not replace it with directory
greps, `command -v`, or a trial review: a found installation is not necessarily
available, and readiness checks must never spend review work.

On entry to Step 5, emit a structured queue point before detection. A crash or
paused session between the gates and reviewer selection then remains visible as
pending without transcript scraping; the later `phase-start` supersedes it:

```bash
python3 orchestrator/tasks/log-event.py "$TASK_STEM" gate \
  --phase review --status info --detail "review queued" || true
```

| codexEnabled | Detector availability | Action |
|---|---|---|
| missing / invalid | *(skip detection)* | **HALT** — record/report `config-invalid`; repair the canonical project config in Site Setup. |
| auto | available | invoke the official Codex plugin review |
| auto | unavailable / unknown | invoke `internal-reviewer` |
| true | available | invoke the official Codex plugin review |
| true | **unavailable / unknown** | **HALT** — record `require-codex-blocked` and escalate. Do not silently fall back. |
| false | *(skip detection)* | invoke `internal-reviewer` |

Before invoking, increment the task-local positive decimal `REVIEW_ATTEMPT` and
emit the structured selection. The values are fixed:

| Decision | `reviewer` | `selectionReason` | optional `reasonCode` |
|---|---|---|---|
| Automatic + available | `codex` | `codex-available` | — |
| Automatic + unavailable/unknown | `internal-reviewer` | `codex-unavailable` | `fallback-used` |
| Require Codex | `codex` | `forced-codex` | — |
| Internal review only | `internal-reviewer` | `forced-internal` | — |

```bash
python3 orchestrator/tasks/log-event.py "$TASK_STEM" phase-start \
  --phase review --status info \
  --meta reviewer="$REVIEWER" \
  --meta reviewAttempt="$REVIEW_ATTEMPT" \
  --meta selectionReason="$SELECTION_REASON" \
  ${REVIEW_REASON_CODE:+--meta reasonCode="$REVIEW_REASON_CODE"} || true
```

For `Require Codex` with unavailable/unknown status, record the selection above,
then close it without invoking a reviewer:

```bash
python3 orchestrator/tasks/log-event.py "$TASK_STEM" stop \
  --phase review --status blocked \
  --meta reviewer=codex \
  --meta reviewAttempt="$REVIEW_ATTEMPT" \
  --meta reasonCode=require-codex-blocked || true
```

Invoke the selected path exactly once:

```text
# reviewer=codex
Skill(skill: "codex:review", args: "--wait --scope working-tree")

# reviewer=internal-reviewer
Agent(subagent_type: "internal-reviewer",
      prompt: "TASK_STEM=<stem>. Internal gates passed. Task: <task content>. Implementation plan (pass it WHOLE — its `## Behavioral edge-cases` section is the reviewer's per-task logic worklist and its `## Test contract` section is the test-quality worklist; do not trim): <plan verbatim>. Builders that ran: <list>. Review the current diff using only your own read/analysis tools and return classified findings — do NOT spawn builders or any sub-agent. Test-quality gate (each is a finding, not advice): expected values derived from acceptance/contract rather than copied from the implementation; the test would fail on the old bug; the subject is exercised through its public/observable contract and is never mocked itself; a relevant negative/boundary case exists; no existing assertion was weakened and no test deleted while its contract survives; async behavior is scheduler-driven (no sleeps/retry-until-pass); tests are hermetic and independent; the platform lane matches the behavior; any screenshot baseline change is deliberate; anti-patterns (assertTrue(true), production-formula echoes, shared mutable fixtures, real network/DB, over-mocked mappers, one giant end-to-end) are findings.")
```

When Step 3.5 found pre-existing work, append the footprint path list and
instruct `internal-reviewer` to confine its review to those paths. The official
Codex command reviews the working tree; discard any Codex finding outside the
task footprint during normalization. When the footprint equals the whole-tree
diff, no footprint filter is needed.

Both paths yield the **same normalized output shape**; the orchestrator branches only on
the verdict:

- **Clean** → proceed to Step 5.5, then Steps 6–6d. The `DONE` token is the
  verdict, not the end of your run.
- **Critical / Major** → goto Step 4 (re-run validators after fixes).
- **Minor / Style / Info** → batch into a single non-looping fix, OR copy each as
  a `### Caveats` bullet WITHOUT re-looping. Don't loop per nit.
- **Stylistic-philosophy disagreement** (reviewer prefers X, project prefers Y)
  → **hold for the user**; never auto-route.
- A finding requiring architectural change → escalate.

The re-review loop is *this role's*, not the plugin command's or the internal
reviewer's — track the iteration count yourself across re-invocations. Record
the reviewer identity in the Step 6 summary. Treat a Codex response that cannot
be normalized into classified findings or an explicit clean verdict as
`reviewer-invocation-failed`; never infer a clean result from empty/malformed
output.

Close every attempt with the same `reviewer` and `reviewAttempt`. Use the exact
terminal forms below; a retry is a new numbered attempt and therefore a new
`phase-start`:

```bash
# Clean verdict
python3 orchestrator/tasks/log-event.py "$TASK_STEM" phase-end \
  --phase review --status ok \
  --meta reviewer="$REVIEWER" \
  --meta reviewAttempt="$REVIEW_ATTEMPT" || true

# Findings / completed failed verdict
python3 orchestrator/tasks/log-event.py "$TASK_STEM" phase-end \
  --phase review --status fail \
  --meta reviewer="$REVIEWER" \
  --meta reviewAttempt="$REVIEW_ATTEMPT" \
  --meta reasonCode=review-failed || true

# Reviewer invocation failure
python3 orchestrator/tasks/log-event.py "$TASK_STEM" stop \
  --phase review --status fail \
  --meta reviewer="$REVIEWER" \
  --meta reviewAttempt="$REVIEW_ATTEMPT" \
  --meta reasonCode=reviewer-invocation-failed || true
```

**Review fallback policy.** `auto` selects `internal-reviewer` only when the
shared detector reports Codex unavailable/unknown **before** the attempt starts.
After `codex` is recorded in `phase-start`, any invocation failure is a failed
Codex attempt: do not silently switch reviewers inside that attempt. `true`
forces Codex and halts when it is not available. `false` selects
`internal-reviewer` directly. The reviewer is always a **separate agent session**
— you are never the reviewer (self-review yields zero independence). If both are
unavailable: `BLOCKED[scaffold]: no reviewer agent available…`. Do NOT proceed
without a review pass.

## Step 5.5 — Auto security-review (conditional — Anthropic `security-review` skill)

Runs after Step 5 returns clean, **only when the diff touches** auth/token/
credential paths. No project-config flag — implicit and conservatively triggered.

**No severity threshold here.** Unlike Step 5, security findings block regardless
of severity — a `Minor`/`Info`-labelled credential or token regression still
routes to a builder; never batch one to `### Caveats` or hold it.

Trigger detection:

```bash
PRE_TASK_SHA=$(cat /tmp/orchestrator_pre_task_sha_${TASK_STEM}.txt 2>/dev/null); [ -z "$PRE_TASK_SHA" ] && PRE_TASK_SHA=HEAD
SECURITY_TOUCHED=$(git diff --name-only "$PRE_TASK_SHA" -- ':!*.md' ':!*.json' ':!*.lock' 2>/dev/null | \
  grep -iE 'token|auth|credential|secret|password|cipher|crypt|jwt|oauth|keystore' | head -5)
```

If empty → record `skipped (no matching files)` and skip. Otherwise detect
`Skill`-tool availability (same off-by-default lever as Step 4.6):

| Skill available | Action |
|---|---|
| yes | invoke `Skill(skill: "security-review", args: "<security intent>")` |
| no | record `skipped (Skill tool unavailable; manually run security-review on the diff)`. **No HALT** — advisory by default. |

**Security intent** — one sentence naming the touched surface + project context
(the validation-gates skill, references/when-to-stop-and-ask.md § Data layer;
the data-layer skill, references/auth-session.md). The Anthropic skill carries its own
credential/token/crypto deny-rules baseline; the project does not duplicate them.

Route findings via the **same dedup as Step 4** → group by `routed_to` → one
message per builder → goto Step 4. **Cap: 2 security-review iterations per task.**
Beyond 2, escalate (*"the change as designed cannot satisfy the deny rules; split
the task or drop the credential-touching part."*).

**Turn discipline — same as Step 4.6.** The skill's closing instruction binds the
**skill's report**, not your turn. On clean (or skipped), continue **in the same
turn** through Steps 6 → 6d. Record the gate in the summary under `### Security
review`. If any security finding was resolved, add ONE `### Caveats` bullet:
`Security finding: <one-line> — fixed in <builder>.` Do NOT add a new appendix
heading — the six-heading parser contract is fixed.
