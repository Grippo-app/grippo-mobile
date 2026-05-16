# Invalidate-Sub — Sub-Agents & Tasks System Audit

Recurring auto-audit that keeps `requirements/sub-agents/` and `requirements/tasks/` aligned with the chapters (`requirements/00-overview/` … `14-cookbook/`) and the live code. Each pass picks the row with the **lowest audit count** from the log below (ties → table order), drills deep, applies high-confidence fixes, increments the count.

**Scope** — `requirements/sub-agents/{builders,validators,helpers}/*.md`, `requirements/sub-agents/lint.sh`, `requirements/tasks/README.md`, the `requirements/tasks/done/` layout.

**Not scope** — chapter-level doc drift → `invalidate.md`. Code-side issues (a class an agent expects doesn't exist; a recipe step that no longer works) → flag in the report, never patch here. Templatization findings (concrete reference-repo names where the agent/task spec should use placeholders) → owned by `invalidate-templatize.md`; flag and route, never patch here.

**Mode** — Template mode is in force. `requirements/` is a project-agnostic template; see `00-overview/05-template-conventions.md` for the substitution table. Slot placeholders (`<Product>Api`, `com.<org>.<product>`, `<product-domain>.com`) and canonical example types (`Note`, `Tag`, `AmountFormatState`) are intentional — NOT drift against the reference repo's literal names.

---

## The prompt

Feed the block below to a fresh agent at the repo root.

````
You are auditing the task-execution toolkit under `requirements/sub-agents/` and `requirements/tasks/`. Each item drifts from (a) the chapters it cites, (b) the live code it operates on, (c) the other items it coordinates with. **One row per pass.**

## Step 1 — pick the focus row

Read the Audit log at the bottom of `requirements/invalidate-sub.md`. Pick the row with the **lowest count**. Ties → table order (top wins). If the user names a row, honor that.

The row's path dictates the audit path:

- `sub-agents/builders/<name>.md` → Path A (agent audit, builder profile).
- `sub-agents/validators/<name>.md` → Path A (agent audit, validator profile).
- `sub-agents/helpers/<name>.md` → Path A (agent audit, helper profile).
- `sub-agents/lint.sh` → Path B (lint script audit).
- `tasks/README.md` → Path C (task contract audit).

## Step 2 — enumerate scope (explicit, no sampling)

Write out three lists before reading anything:

**A. The target file** — full Read of `<path>`. Inventory: frontmatter, every cited chapter, every grep pattern, every rule, every cross-reference to another agent/file.

**B. Every `requirements/<chapter>/<file>.md` cited.** Open each.

**C. Every source artifact the row acts on.**
- Builders: the cookbook recipe + the layer of code it writes into + at least one real example of that layer in the repo.
- Validators: every grep pattern + a sample of matching and non-matching files.
- Helpers: the other agents invoked, or the chapter index (for `requirements-lookup`).
- `lint.sh`: every script-internal check (frontmatter, dead links, README inventory).
- `tasks/README.md`: `helpers/orchestrator.md`, `helpers/task-intake.md`, current `tasks/` contents, current `tasks/done/` contents.

Use Read + Bash (`find`/`grep`/`ls`). **No Explore subagent** for verification — it samples. Spawn an Agent only for breadth grep, and require verbatim code blocks with paths and line numbers in the return.

## Step 3 — deep verification

### Path A — agent audit (.md file)

1. **Frontmatter.**
   - `name` matches the file stem.
   - `description` accurately reflects what the agent now does.
   - `tools` minimal-but-sufficient. Builders need write tools; validators MUST NOT have `Edit`/`Write` (auto-fix belongs to the builder for the violated rule). Helpers vary by role.
   - `model` matches the workload. Default sonnet; opus only for judgment-heavy work.

2. **Cited chapters still cover the cited topic.** For each `requirements/<chapter>/<file>.md` in the agent's "Authoritative reading":
   - File exists at the cited path.
   - Topic the agent claims it covers is still present.
   - Cited section heading still exists.
   - Chapter reorganized (topic split / merged / moved) → stale reference → finding.

3. **Cited rules still match the live code.**
   - **Builder**: walk one real example end-to-end through the agent's "Steps you MUST perform" against the current shape of the layer. A step that assumes stale code shape → finding.
   - **Validator**: run each grep pattern against the repo. Does it catch real violations? Does it produce false positives the agent doesn't filter? Both directions are findings.
   - **Helper**:
     - `task-intake` — classification table maps to **existing** builders; prerequisite list accurate; rejected-task examples still match real failure modes.
     - `orchestrator` — loop order matches the real validator/builder set; escalation triggers point to current chapters; bootstrap-check (Step 0) matches `launch.md` outputs.
     - `context-finder` — "Common queries" recipes return useful results against the current repo.
     - `requirements-lookup` — keyword → chapter table resolves to existing files at the cited line ranges.
     - `codex-review-loop` / `internal-reviewer` — Codex plugin command + routing table current; output shape matches what the orchestrator consumes.

4. **Cross-agent references.** For each "route to <agent>" or "spawn <agent>":
   - Target agent exists at the cited path.
   - Target's `description` actually covers the work being routed.
   - Handoff shape (what the orchestrator passes; what the builder reports back) is consistent on both sides.

5. **Anti-patterns the agent enforces.** Cross-reference each forbidden item against `requirements/13-anti-patterns/01-forbidden-patterns.md`:
   - Chapter dropped a rule → agent's enforcement is stale.
   - Chapter added a rule → agent may be missing enforcement.

6. **Output format consistency** (validators + reviewers). All validators and both reviewers share a findings format. Drift in fields or severity scale → finding (orchestrator and `codex-review-loop` consume by shape).

7. **Builder ↔ cookbook parity** (builders only). Each builder covers exactly one cookbook recipe.
   - `requirements/14-cookbook/*` gained a recipe with no corresponding builder → gap (flag; do not auto-create).
   - Recipe removed but builder still exists → obsolete (flag; do not auto-delete — removal touches orchestrator routing).

### Path B — `sub-agents/lint.sh` audit

1. Every check the script runs (frontmatter shape, dead links, README inventory match) still resolves on the current set of agent files.
2. Inventory match — every `*.md` under `builders/`, `validators/`, `helpers/` appears in `sub-agents/README.md` "Agent inventory" tables; conversely, no inventory entry points at a missing file.
3. Every dead-link target still exists, or the script's allowlist is current.
4. Exit codes / error messages match how CI or the orchestrator consumes them.

### Path C — `tasks/README.md` audit

1. **Required-section list** matches what `task-intake` rejects on. Today: `## Goal`, `## Inputs`, `## Acceptance`, `## Out of scope` are required; `## Depends on` optional. Verify against `sub-agents/helpers/task-intake.md`.
2. **File-naming scheme** (`TASK_<N>_<title>.md`) matches what `orchestrator` and `task-intake` parse.
3. **Lifecycle claim** — orchestrator moves the completed file to `tasks/done/` as the final step of a successful run. Verify against `sub-agents/helpers/orchestrator.md` Step 6.
4. **Dependency resolution** — README claims `task-intake` verifies prereqs are in `done/`. Verify against `task-intake.md`.
5. **`tasks/done/` housekeeping** — `ls tasks/done/` for malformed names; no in-flight tasks parked there.
6. **`tasks/` housekeeping** — every in-flight `TASK_*.md` has the four required sections (sample-check; full validation is `task-intake`'s job).
7. **Cross-reference to `invalidate.md`** at the end of the README ("If the task surfaced architecture drift, run `invalidate.md` to update the requirements.") — chapter-audit still lives there.

## Step 4 — structured findings

Classify each: STALE_CHAPTER_REF | STALE_GREP | STALE_RULE | MISSING_RULE | CROSS_AGENT_DRIFT | FRONTMATTER_DRIFT | OUTPUT_FORMAT_DRIFT | OBSOLETE_AGENT | MISSING_AGENT | TASK_CONTRACT_DRIFT | LINT_DRIFT.

Format:

```
### Finding N: <title>

**Target:** requirements/<path> (line NN–MM)
**Category:** <one>
**Source evidence:** <what changed in requirements/* or the live code that invalidates the claim>
**Confidence:** High | Medium | Low

**Target says:** > <verbatim>
**Reality:** > <verbatim from the chapter or live code>
**Mismatch:** <description>
**Proposed correction:** <exact replacement text>
**Reasoning:** <agent/contract drift vs intentional>
```

Zero findings on a fresh row is suspicious — re-verify. Plausible on a high-count row — state what was checked.

## Step 5 — apply edits

- **High confidence** → `Edit` the target file. Paragraph-level. Preserve frontmatter format and normative voice (MUST / MUST NOT / do NOT). Re-Read briefly to confirm clean landing.
- **Medium / Low** → flag in the report. Do not edit.

## Step 6 — update the Audit log

In `requirements/invalidate-sub.md`, increment the row's count and set Last audited to today (YYYY-MM-DD). Single `Edit`, single row.

## Step 7 — close out

Print:

1. Row audited (new count).
2. Files edited (one-line summary each).
3. Findings flagged for human review.
4. Next row per the updated log.

## Constraints

- One row per pass. Every cited chapter, every grep pattern, every cross-reference. No skim, no sample.
- No Explore subagent for verification. Agent for breadth grep only; verbatim returns required.
- No new agents unless Path A item 7 surfaces an unambiguous gap AND the user authorizes it.
- No removing agents during this pass. Obsolete → flag; removal touches orchestrator routing.
- No widening `tools`. Principle is least-tool: builders write, validators MUST NOT.
- No changing `model` without a clear reason; sonnet is default.
- No full-file rewrites — targeted paragraph-level edits only.
- Preserve normative voice (MUST / MUST NOT / do NOT).
- Don't audit `requirements/<chapter>/*` here — that's `invalidate.md`. If a chapter has drifted from code, **flag** that the chapter audit is owed; do not patch.
- **Template mode** — `requirements/` is a project-agnostic template. Authority: `00-overview/05-template-conventions.md`.

  - Slot placeholders (`<Product>`, `<product>`, `<org>`, `<product-domain>`, `com.<org>.<product>`, `<Product>Api`) are NOT drift against reference-repo literals.
  - Canonical example types (`Note`, `Tag`, `Item` and their fan-out) are NOT drift when live code has `Training`/`Exercise` instead.
  - Generic numeric format-state (`AmountFormatState`) is NOT drift against product-specific `WeightFormatState`/`HeightFormatState`/etc.
  - Reserved names (Base*, AppTokens, UiText, DialogConfig, etc. — see `04-glossary.md` "Reserved names" / `05-template-conventions.md` §7) must match live code verbatim.

  When an agent file cites a chapter's code block (Path A item 2/3): treat placeholders and canonical types as authoritative; verify surrounding signature/structure, not the slot identifier itself.

  If a sub-agent file or task spec contains a concrete reference-repo name (e.g. `GrippoApi`, `TrainingFeature`, `WeightFormatState`, `ProfileBodyState`, `com.grippo.*`, "Workout history" worked example) — that is a `PRODUCT_LEAKAGE` finding owned by `invalidate-templatize.md`. Flag in the report; do NOT patch here.
````

---

## Audit log

Lowest count wins. Ties → table order. After each pass, the agent increments count and sets Last audited.

| Item | Count | Last audited |
|---|---|---|
| `tasks/README.md` | 1 | 2026-05-16 |
| `sub-agents/lint.sh` | 1 | 2026-05-16 |
| `sub-agents/builders/cross-feature-nav-builder.md` | 1 | 2026-05-16 |
| `sub-agents/builders/data-feature-builder.md` | 0 | — |
| `sub-agents/builders/data-service-scaffold-builder.md` | 0 | — |
| `sub-agents/builders/dialog-builder.md` | 0 | — |
| `sub-agents/builders/endpoint-builder.md` | 0 | — |
| `sub-agents/builders/feature-module-scaffold-builder.md` | 0 | — |
| `sub-agents/builders/mapper-builder.md` | 0 | — |
| `sub-agents/builders/resource-builder.md` | 0 | — |
| `sub-agents/builders/room-migration-builder.md` | 0 | — |
| `sub-agents/builders/screen-builder.md` | 0 | — |
| `sub-agents/validators/anti-pattern-scanner.md` | 0 | — |
| `sub-agents/validators/architecture-validator.md` | 0 | — |
| `sub-agents/validators/build-validator.md` | 0 | — |
| `sub-agents/validators/compose-stability-validator.md` | 0 | — |
| `sub-agents/validators/data-layer-validator.md` | 0 | — |
| `sub-agents/validators/di-validator.md` | 0 | — |
| `sub-agents/validators/mvi-contract-validator.md` | 0 | — |
| `sub-agents/validators/naming-convention-validator.md` | 0 | — |
| `sub-agents/helpers/codex-review-loop.md` | 0 | — |
| `sub-agents/helpers/context-finder.md` | 0 | — |
| `sub-agents/helpers/internal-reviewer.md` | 0 | — |
| `sub-agents/helpers/orchestrator.md` | 0 | — |
| `sub-agents/helpers/requirements-lookup.md` | 0 | — |
| `sub-agents/helpers/task-intake.md` | 0 | — |

Round complete when all rows share the same count. Next pass picks the lowest again (top-to-bottom on a tie).

---

## Coordination with `invalidate.md`

`invalidate.md` owns `requirements/<chapter>/`. `invalidate-sub.md` owns `requirements/sub-agents/` and `requirements/tasks/`. Run independently; never concurrently — this pass reads chapter files to verify references, the chapter pass reads sub-agent files for nothing.

Run the chapter pass first when the live code has changed significantly, or when a Path A audit flagged "the chapter doesn't say what the agent claims". Run this pass after the chapter pass settles, or when a task execution surfaced agent confusion (a builder did the wrong thing, a validator missed a violation, the orchestrator routed incorrectly).
