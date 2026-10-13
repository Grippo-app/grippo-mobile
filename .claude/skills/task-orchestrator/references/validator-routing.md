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
separate sessions and cannot see the orchestrator's shell var, and several of
them reason about the diff. Prepend the literal token `TASK_STEM=<stem>.` to
each prompt (e.g. `TASK_STEM=TASK_3_add_filter.`) so each spec can address the
task's control-plane state. Every diff-sensitive spec diffs against `HEAD` —
the sealed base commit of this execution root — never a `/tmp` baseline.
Applies to `scope-leak-validator`,
`mvi-contract-validator`, `build-validator`, `anti-pattern-scanner`,
`acceptance-tracer`, `architecture-validator`, and the Step 5 / 5.5 reviewers.

When compilation is permitted, for `build-validator` prepend `BUILD_MODE=compile.` — it parses the token
from the prompt text (the parent does not export it as a shell env var):

```
Agent(subagent_type: "build-validator", prompt: "BUILD_MODE=compile. TASK_STEM=<stem>. Run Step 1 (compile path) and Step 2.5 (deprecation extraction) per your spec.")
```

**Always include the three always-on validators** regardless of what task-intake
returned (they run in the same parallel group as the conditionals):

- `build-validator` — source/configuration assessment on every task. Run fast
  Kotlin compile + KSP in mode=compile only when compilation is permitted;
  otherwise record it as unrun with the user's restriction. No
  R8/dex/lint/packaging or `xcodebuild` here (that is Step 4.5 when permitted).
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
before writing)**. These reports record the configured source/spec comparison;
retain both when the gate ran. They do not require an automated screenshot bundle
or establish rendered fidelity. Its
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

### Footprint scoping

In an execution root the footprint is the whole diff against `HEAD`: the
checkout starts at the exact target base commit, so nothing in it predates the
task. Brief the diff-sensitive validators (`scope-leak-validator`,
`anti-pattern-scanner`, `acceptance-tracer`) with `git status --porcelain`
output when you want them scoped explicitly; there is no subset to compute and
no baseline file to read.

`build-validator` is a **split case**: its compile and assemble gates (Step 1 /
Step 4.5) read **final** file state, while its Step 2.5 deprecation extraction
is **diff-scoped** against `HEAD` — the sealed base — so neither invocation
needs a footprint argument:

```
Agent(subagent_type: "build-validator", prompt: "BUILD_MODE=compile. TASK_STEM=<stem>. Run Step 1 (compile path) and Step 2.5 (deprecation extraction) per your spec; diff against HEAD, which is this run's sealed base commit.")
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

When applicable validator assessments have no blocking findings AND permitted
compilation passes (or excluded compilation is explicitly recorded as unrun
with the user's reason) → proceed to **Step 4.5**.
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

## Step 4.5 — Full assemble gate

Step 4 converged. Permitted compile-only checks do not exercise R8 shrinking,
dex packaging, lint, resource processing, the iOS XCFramework link, or
`xcodebuild :iosApp`. If builds are within the user's authorized verification
scope, run `build-validator` once more in `mode=assemble`:

```
Agent(subagent_type: "build-validator", prompt: "BUILD_MODE=assemble. TASK_STEM=<stem>. Run Step 1 (full XCFramework + assembleDebug), Step 2 (ancillary module assembles), Step 2.5 (deprecation extraction), Step 3 (xcodebuild :iosApp when iosEnabled=true), per your spec.")
```

(No footprint argument is needed: Step 2.5 diffs against `HEAD`, this run's sealed base commit, and the assemble gate reads final state.)

When the user excludes builds, record the affected build checks as unrun with
that reason and continue permitted review. Do not label them PASS or invoke a
build to satisfy this playbook.

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
tool enabled and the requested runtime checks are authorized; otherwise it is
recorded as deferred or skipped with the actual reason. `verifyEnabled:false`
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
when it applies, then Step 5 after recording observed visual findings or unverified
states. A source/spec match cannot supply a visual PASS.

**Turn discipline** — `Skill` executes inside your own session; the skill's
output instructions bind the **report it emits**, not your turn. Capture the
report as the gate's return value, then keep executing this playbook in the same
turn. **Case-mapping:** the skill returns uppercase `PASS/FAIL/SKIP`; write
lowercase `pass/fail/n/a` into the appendix `Result:` bullet and uppercase
`PASS/FAIL/N/A` into the chat summary.

**Cap: at most 3 verify invocations per task.**

## Step 4.6b — Visual review (conditional)

For a changed Figma-backed surface, follow the validation-gates
[screenshot fidelity reference](../../validation-gates/references/visual-review.md).
Use its actual preview or an authorized app screenshot and compare the relevant
states with the design oracle. Check content, theme, dimensions, spacing and
interaction affordances. Do not add screenshot automation or dependencies.
Record captures and actual observations; unavailable runtime visual checks stay
explicitly unverified. A source/spec match alone does not prove rendered fidelity.

**Cap: at most 3 invocations per task.** This bounds review/fix cycles; it does
not require capture tooling or turn an unavailable rendered state into a defect.

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
      prompt: "TASK_STEM=<stem>. Internal gates passed. Task: <task content>. Implementation plan (pass it WHOLE — its `## Behavioral edge-cases` section is the reviewer's per-task logic worklist; do not trim): <plan verbatim>. Builders that ran: <list>. Review the current diff using only your own read/analysis tools and return classified findings — do NOT spawn builders or any sub-agent. Review requirements, business behavior, state ownership/restoration, lifecycle/cancellation, DI, privacy and unnecessary complexity. Distinguish source evidence from observed runtime behavior; do not add test infrastructure.")
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
# HEAD is this execution root's sealed base commit: every difference from it
# belongs to this task.
SECURITY_TOUCHED=$(git diff --name-only HEAD -- ':!*.md' ':!*.json' ':!*.lock' 2>/dev/null | \
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
