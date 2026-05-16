# Invalidate-Templatize — Requirements Genericization Pass

Recurring auto-audit that strips product-specific leakage from `requirements/` so the folder works as a project-agnostic template. Each pass picks the row with the **lowest audit count** from the log below (ties → table order), scans every `.md` in that row's scope, and substitutes concrete reference-repo names with the placeholders defined in `00-overview/05-template-conventions.md`.

**Scope** — every `.md` under `requirements/` EXCEPT:

- `requirements/00-overview/03-project-config.md` — the per-project edit point; its frontmatter is intentionally Grippo-valued.
- `requirements/00-overview/05-template-conventions.md` — the conventions file itself; it documents what to replace and quotes the reference values legitimately.
- `requirements/invalidate.md`, `requirements/invalidate-sub.md`, `requirements/invalidate-templatize.md` — meta-files.
- `requirements/README.md` — already templatized via `SETUP_TEMPLATIZE.md`; do not re-edit unless leakage appears.

**Not scope** — chapter-vs-code drift (`invalidate.md`'s job), sub-agent contract drift (`invalidate-sub.md`'s job), live code under `:shared`/`:data-services`/etc.

**Prerequisite** — `SETUP_TEMPLATIZE.md` must have been run once. If `00-overview/05-template-conventions.md` does not exist, STOP and run setup first.

---

## The prompt

Feed the block below to a fresh agent at the repo root.

````
You are templatizing the `requirements/` folder against the substitution table in `requirements/00-overview/05-template-conventions.md`. Goal: replace concrete reference-repo names with placeholders so the requirements work for any project, not just Grippo. **One row per pass.**

## Step 0 — verify prerequisite

Confirm `requirements/00-overview/05-template-conventions.md` exists. If missing, STOP and report that `SETUP_TEMPLATIZE.md` must be run first.

## Step 1 — pick the focus row

Read the Audit log at the bottom of `requirements/invalidate-templatize.md`. Pick the row with the **lowest count**. Ties → table order (top wins). If the user names a row, honor that.

## Step 2 — enumerate scope (explicit, no sampling)

The row name maps to a directory or file set:

- Chapter rows (`00-overview` through `14-cookbook`) → `ls requirements/<row>/*.md`, EXCLUDING `03-project-config.md` (for `00-overview`) and `05-template-conventions.md` (for `00-overview`).
- `sub-agents` row → `find requirements/sub-agents -name '*.md'` (all builders, validators, helpers, README).
- `tasks` row → `find requirements/tasks -name '*.md'`.

Read `requirements/00-overview/05-template-conventions.md` fully — it is the source of truth for substitutions in this pass. Re-read every time; do not rely on memory across passes.

Use Read + Bash (`find`/`grep`/`rg`). No Explore subagent. Spawn an Agent only for breadth grep, with verbatim return.

## Step 3 — per-file leakage scan

For EACH .md file in scope:

### 3.1 Read fully

Top to bottom. Note headers, every code block, every named symbol, every cross-reference.

### 3.2 Grep for leakage signatures

Use the patterns from `05-template-conventions.md` §8 against the file. Two passes:

```
rg -n -i 'grippo|training|exercise|workout|muscle|equipment|weight|height|profile.?body|workout.?history|excluded.?muscle|excluded.?equipment|exercise.?example|iteration|repetition|intensity|density|volume|duration|multiplier|percentage' <file>
```

```
rg -n -E 'GrippoApi|Training\w+|Exercise\w+|Iteration\w+|Muscle\w+|Equipment\w+|Weight\w*FormatState|Height\w*FormatState|Volume\w*FormatState|Density\w*FormatState|Intensity\w*FormatState|Repetition\w*FormatState|Multiplier\w*FormatState|Duration\w*FormatState|Percentage\w*FormatState|ProfileBody\w*|WorkoutHistory\w*|WeightHistory\w*|MuscleLoad\w*|TrainingStreak\w*|GoalProgress\w*|RatingPicker|StartTraining|FinishWorkout|ChangeWeight|stubTraining|stubWeight\w+|com\.grippo|grippo-app\.com|:data-features:trainings|:data-features:muscle|:data-features:weight-history|:data-features:goal|:data-features:equipment|:data-features:excluded|:data-features:exercise|:ui-screen-features:training|:ui-dialog-features:weight-picker' <file>
```

Skip hits in:
- Quoted exception text that documents the rule (e.g. `00-overview/04-glossary.md`'s "Product-specific names" line).
- Code blocks that explicitly demonstrate "before/after" replacement (rare; the templatize loop generally produces only "after").

### 3.3 Classify each hit

For every remaining hit, classify per `05-template-conventions.md`:

| Class | Source pattern | Substitution |
|---|---|---|
| Slot leakage | `GrippoApi`, `com.grippo`, `grippo-app.com`, bare `Grippo` in class prefixes | `<Product>Api`, `com.<org>.<product>`, `<product-domain>.com`, `<Product>` (§1) |
| Domain-type leakage | `TrainingRepository`, `ExerciseEntity`, `MusclePack`, `WeightHistory*`, `Goal*` (when illustrating a pattern) | `NoteRepository`, `NoteEntity`, `NotePack`, `Note*`, `Tag*` (§2) |
| Numeric FormatState | `WeightFormatState`, `HeightFormatState`, `DurationFormatState`, etc. | `AmountFormatState` (§4.2) |
| Format infrastructure list | "must exist" list naming product FormatStates | Remove product entries; keep only §4.1 (§4.3) |
| Module list | Concrete feature/entity enumerations (12-item areas table, 16-entity Database list) | `<feature>` / `<Entity>Entity` placeholders per §5 |
| Component-name leakage | `WeightHistoryChart`, `MuscleLoadHeatmap` in design-system examples | `<Entity>Chart`, `<Entity>Heatmap` per §6 |
| Worked-example leakage | `ProfileWorkoutHistory*`, "Workout history" prose | **FLAG, do not patch** (cross-file rewrite — §3 & §9 of conventions) |
| File-rename hint | `03-grippo-api-and-dtos.md` filename refs | **FLAG, do not patch** (§9 of conventions) |
| Resource-key leakage | `Res.string.weight_*`, `notification_weight_*`, `Deeplink.WeightHistory` | `Res.string.<key>`, `Deeplink.<Name>` placeholders |
| Stub function | `stubTraining()`, `stubWeightHistoryList()` | `stubNote()`, `stubNotes()` |

### 3.4 Verify replacement preserves the pattern

After substitution, re-read the surrounding paragraph and code block. The teaching value of the example MUST survive. Specifically:

- Kotlin code blocks: imports still resolve under the substituted names; signatures still compile-looking; `@Annotation(binds = [<X>::class])` still references a plausible type.
- Cross-references to other chapters: if the chapter cited a section like "see `06-data-layer/03-grippo-api-and-dtos.md`", update to `06-data-layer/03-product-api-and-dtos.md`. If the target file hasn't been renamed yet, leave the citation and add a finding instead.
- Tables of contents: rename column values consistently within a table.

## Step 4 — findings format

Single category: `PRODUCT_LEAKAGE`. Format:

```
### Finding N: <one-line summary>

**File:** requirements/<path>.md (line NN–MM)
**Class:** Slot | Domain-type | Numeric-FormatState | FormatState-list | Module-list | Component-name | Worked-example | File-rename | Resource-key | Stub
**Confidence:** High | Medium | Low
**Action:** PATCHED | FLAGGED

**Before:** > <verbatim>
**After:** > <verbatim substituted>  (omit for FLAGGED — explain why instead)
**Reasoning:** <which §of conventions justifies the substitution>
```

Zero findings on a chapter row is suspicious for the first 1–2 passes (most chapters have at least one leak). Plausible later; state grep result.

## Step 5 — apply edits

- **High confidence + paragraph-level** → `Edit` the file. One `Edit` per leak (or one `replace_all` per identifier if the same token appears many times in the same file). Do NOT re-Read after each Edit unless the tool errored.
- **Cross-file rewrite needed** (worked example, multi-file consistency) → FLAGGED only. Don't patch.
- **File rename** → FLAGGED only.
- **Editorial judgment** (ambiguous whether a name is product or pattern) → FLAGGED, ask in the report.

Constraints on edits:

- Paragraph-level only. No full-file rewrites.
- Preserve frontmatter, code-block fences, normative voice (MUST / do NOT).
- Do not change indentation or list structure.
- Do not delete content beyond the leaking token — if a sentence becomes empty after substitution, restructure the paragraph minimally; if it requires real rewriting, FLAG instead.
- Imports inside Kotlin code blocks: if substitution would orphan an import (e.g. `import com.grippo.training.Training`), update the import line too.

## Step 6 — update the Audit log

In `requirements/invalidate-templatize.md`, increment the row's count and set Last audited to today (YYYY-MM-DD). Single `Edit`, single row.

Also update the "Leakage remaining" column: after the pass, run

```
rg -c -i -E '(grippo|training|exercise|workout|muscle|weightformatstate|heightformatstate|profilebody|workouthistory)' requirements/<row>/*.md 2>/dev/null | awk -F: '{s+=$2} END {print s+0}'
```

and write the integer into that column. Zero = the row is converged.

## Step 7 — close out

Print:

1. Row audited (new count, leakage remaining).
2. Files edited (one-line summary each).
3. Findings FLAGGED for human review (worked-example, file-rename, editorial).
4. Next row per the updated log.

## Constraints

- One row per pass. Every file in the row. No skim, no sample.
- Source of truth = `00-overview/05-template-conventions.md`. NOT live code. NOT memory.
- Paragraph-level edits only. Cross-file rewrites → flag.
- No file renames in this loop — flag for the final cleanup prompt.
- No new chapters, no moving files between chapters, no restructuring.
- Preserve frontmatter, fences, normative voice.
- This pass does NOT verify against live code (`invalidate.md`'s job). If a substitution accidentally makes the chapter contradict the architecture, that's a problem `invalidate.md` will surface on the next regular audit cycle — don't try to catch it here.
- **Reserved names are reserved.** Do not substitute anything from `05-template-conventions.md` §7 (Base*, OperationManager, AppTokens, UiText, DialogConfig, etc.). If the leakage grep matches one of these, it's a false positive — skip.
- **Convergence signal**: when every row's "Leakage remaining" column is 0 AND no FLAGGED findings remain unresolved, the templatize work is done. At that point, run the final cleanup prompt (worked example + file rename) manually, then this loop can be retired.
````

---

## Audit log

Lowest count wins. Ties → table order. After each pass, the agent increments count, sets Last audited, and updates Leakage remaining.

| Row | Count | Last audited | Leakage remaining |
|---|---|---|---|
| 00-overview | 2 | 2026-05-16 | 0 |
| 01-tech-stack | 2 | 2026-05-16 | 0 |
| 02-module-structure | 2 | 2026-05-16 | 0 |
| 03-architecture-patterns | 2 | 2026-05-16 | 0 |
| 04-base-classes | 2 | 2026-05-16 | 0 |
| 05-design-system | 2 | 2026-05-16 | 0 |
| 06-data-layer | 2 | 2026-05-16 | 0 |
| 07-mappers | 1 | 2026-05-16 | 0 |
| 08-dependency-injection | 1 | 2026-05-16 | 0 |
| 09-conventions | 2 | 2026-05-16 | 0 |
| 10-toolkit | 1 | 2026-05-16 | 0 |
| 11-state-and-formatters | 1 | 2026-05-16 | 0 |
| 12-gradle-build | 1 | 2026-05-16 | 0 |
| 13-anti-patterns | 1 | 2026-05-16 | 0 |
| 14-cookbook | 3 | 2026-05-16 | 0 |
| sub-agents | 2 | 2026-05-16 | 0 |
| tasks | 2 | 2026-05-16 | 0 |

**Converged 2026-05-16.** All rows at Leakage remaining = 0. Final cleanup applied: "Workout history" worked example rewritten to "Note archive" across the four §3 files (plus `07-add-resource.md`, `tasks/README.md`, `sub-agents/README.md`); `06-data-layer/03-grippo-api-and-dtos.md` renamed to `03-product-api-and-dtos.md` with all references updated; `RatingPicker` rewritten to `TagPicker` in `14-cookbook/02-add-dialog.md`. Residual hits in `00-overview/03-project-config.md`, `00-overview/05-template-conventions.md`, `requirements/README.md`, and `00-overview/04-glossary.md` line 59 are documented substitution-table exceptions per §8 of conventions.

---

## Coordination with other invalidates

- `invalidate.md` and `invalidate-sub.md` are PAUSED while this loop runs. Resume them only after every row here has converged.
- When this loop reports a substitution that makes a chapter *structurally* wrong (rather than just stylistically generic), that's evidence the chapter's architectural content was also stale — flag it for the next chapter audit (`invalidate.md`) to handle after templatize finishes.
- The final cleanup prompt (separate, manual one-shot) handles: the cross-file "Note archive" worked example rewrite (§3 of conventions) and the `06-data-layer/03-grippo-api-and-dtos.md` → `03-product-api-and-dtos.md` rename (§9 of conventions). Do NOT include those in this loop.
