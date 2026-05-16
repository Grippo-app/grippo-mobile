# Invalidate — Requirements Chapter Audit

Recurring auto-audit that keeps `requirements/<chapter>/` aligned with the live code. Each pass picks the chapter with the **lowest audit count** from the log below (ties → table order), drills deep, applies high-confidence doc fixes, increments the count.

**Scope** — `requirements/00-overview/` through `requirements/14-cookbook/`.

**Not scope** — `requirements/sub-agents/` and `requirements/tasks/` → owned by `invalidate-sub.md`. Code-side issues (a class an example references that no longer exists; a rule the code violates) → flag in the report, never patch here. Templatization findings (concrete reference-repo names where the chapter should use placeholders) → owned by `invalidate-templatize.md`; flag and route, never patch here.

**Mode** — Template mode is in force. `requirements/` is a project-agnostic template; see `00-overview/05-template-conventions.md` for the substitution table. Slot placeholders (`<Product>Api`, `com.<org>.<product>`, `<product-domain>.com`) and canonical example types (`Note`, `Tag`, `AmountFormatState`) are intentional — NOT drift against the reference repo's literal names.

---

## The prompt

Feed the block below to a fresh agent at the repo root.

````
You are auditing a `requirements/` folder that describes a Kotlin Multiplatform (KMP) mobile architecture. Goal: find and fix doc drift against the live code in the same repo. **One chapter per pass.**

## Step 1 — pick the focus chapter

Read the Audit log at the bottom of `requirements/invalidate.md`. Pick the chapter with the **lowest count**. Ties → table order (top wins). If the user names a chapter, honor that.

## Step 2 — enumerate scope (explicit, no sampling)

Write out two lists before reading anything:

**A. Every .md in the chapter** — `ls requirements/<chapter>/*.md`.

**B. Every source file the chapter references.** Chapter → sources:

- `00-overview` → top-level `README.md`, `launch.md`, `requirements/00-overview/03-project-config.md`, the glossary's named symbols.
- `01-tech-stack` → `gradle/libs.versions.toml`, `gradle.properties`, `settings.gradle.kts`.
- `02-module-structure` → `settings.gradle.kts`, every `build.gradle.kts` named in the chapter.
- `03-architecture-patterns` → `:shared/RootComponent.kt`, representative `*Component.kt`/`*ViewModel.kt`/`*Screen.kt`, `:ui-dialog-features:dialog-api/DialogConfig.kt`.
- `04-base-classes` → every file under `ui-core/foundation/src/{commonMain,androidMain,iosMain}/...`.
- `05-design-system` → every file under `design-system/{core,components,resources,preview}/src/commonMain/...`.
- `06-data-layer` → `BackendClient.kt`, `TokenProvider.kt`, `<Product>Api.kt`, `ClientLogger.kt`, every `dto/`, `Database.kt`, every `*Entity.kt`/`*Dao.kt`/`*Pack.kt`, every `Migration*.kt`, `DatabaseBuilder.{android,ios}.kt`, `StringListConverter.kt`, DataStore files.
- `07-mappers` → each of the 7 `:data-mappers:*` modules' `build.gradle.kts` + 2–3 representative mapper files per direction.
- `08-dependency-injection` → `:shared/Koin.kt`, every `*Module.kt` (grep `@Module` + `@ComponentScan`).
- `09-conventions` → repo-wide grep for anti-pattern markers (`viewModelScope.launch`, `mutableListOf`, `mutableStateListOf`, `Color(0xFF`, `\.dp\b` outside design-system, `stringResource\(R\.`, `painterResource\(R\.`, `runBlocking`, `GlobalScope`, `lateinit var`); read sample hits.
- `10-toolkit` → every file under `toolkit/*/src/{commonMain,androidMain,iosMain}/...`.
- `11-state-and-formatters` → every file under `ui-core/state/.../commonMain/.../formatters/...` + `UiText.kt`.
- `12-gradle-build` → every file under `build-logic/convention/src/main/kotlin/`, `gradle/libs.versions.toml`, `gradle.properties`, `settings.gradle.kts`, app shell `build.gradle.kts`, `:shared/build.gradle.kts`.
- `13-anti-patterns` → repo-wide grep for each forbidden item; absence is the expected state.
- `14-cookbook` → for each recipe, the artifacts it touches; verify each exists with the documented shape.

Use Read + Bash (`find`/`grep`/`ls`). **No Explore subagent** for verification — it samples. Spawn an Agent only for breadth grep, and require verbatim code blocks with file paths and line numbers in the return.

## Step 3 — 7-point deep verification per .md file

For EACH file in the chapter, do all seven steps. If a step has nothing to check, state that — do not silently skip.

1. **Full read.** Top to bottom. Note headers, every code block, every rule (MUST / forbidden / anti-pattern), every cross-reference, every named symbol.
2. **Verbatim code blocks** — line-by-line against the source. Class/interface/object name, visibility, generics, ctor params (names, types, defaults, `@InjectedParam`), every method (name, params, return, modifiers `public`/`internal`/`protected`/`suspend`/`inline`/`@Composable`/`@ReadOnlyComposable`), every annotation (`@Single(binds=...)`, `@Module(includes=...)`, `@Serializable`, `@SerialName(...)`, `@Entity(...)`, `@Immutable`, `@Transient`, ...), every default, every field. Method bodies and comments if quoted. **A single-character mismatch counts.**
3. **Prescriptive rules** — grep evidence per rule. Followed everywhere → ✓. Violated → finding (decide: doc fix vs code drift). Partial → AMBIGUOUS.
4. **Named symbols** — every class/method/constant/token/module path: verify it exists at the documented path with the documented signature. Tokens (`AppTokens.colors.text.primary`) — open `AppColor.kt`, verify nested path + type. Module paths — `include(":...")` in `settings.gradle.kts`, dir exists, namespace matches.
5. **Cross-references** — for every "see `<chapter>/<file>.md`", open it and verify it still covers the cited topic.
6. **Cookbook steps** (only for `14-cookbook`) — walk each step against the live code: paths exist, APIs match, convention plugin behavior matches, gradle commands match the current task graph.
7. **Illustrative (non-verbatim) examples** — must compile under current architecture: imports resolve, annotations apply, APIs present, no anti-pattern violation.

## Step 4 — structured findings

Classify each: MISSING | OUTDATED | INACCURATE | AMBIGUOUS | REDUNDANT | UNCLEAR.

Format:

```
### Finding N: <title>

**Requirements file:** requirements/<path>.md (line NN–MM)
**Category:** <one>
**Source evidence:** <module>:<path>:<line> — verbatim quote
**Confidence:** High | Medium | Low

**Doc says:** > <verbatim>
**Code says:** > <verbatim>
**Mismatch:** <description>
**Proposed correction:** <exact replacement text>
**Reasoning:** <doc drift vs code drift vs intentional>
```

Zero findings is suspicious — re-verify before reporting. Some chapters yield few (e.g. `13-anti-patterns` on a clean repo) — state what was checked.

## Step 5 — apply edits

- **High confidence** → `Edit` the requirements file. Paragraph-level. Never full rewrites. Re-Read briefly to confirm clean landing.
- **Medium / Low** → flag in the report. Do not edit.

## Step 6 — update the Audit log

In `requirements/invalidate.md`, increment the chapter's count and set Last audited to today (YYYY-MM-DD). Single `Edit`, single row.

## Step 7 — close out

Print:

1. Chapter audited (new count).
2. Files edited (one-line summary each).
3. Findings flagged for human review.
4. Next chapter per the updated log.

## Constraints

- One chapter per pass. Every file. Every code block. Every rule. Every symbol. No skim, no sample.
- No Explore subagent for verification. Agent for breadth grep only; verbatim returns required.
- No new chapters or files unless an unambiguous gap demands one.
- No full-file rewrites — paragraph-level edits only.
- Preserve normative voice (MUST, do NOT, numbered lists).
- No new prescriptive rules unless the live code already enforces them. Requirements describe what exists, not aspirations.
- Cite line numbers for source, file paths for docs.
- Don't delete content without strong reason — apparent redundancy may be intentional cross-referencing.
- **Template mode** — `requirements/` is a project-agnostic template. Authority: `00-overview/05-template-conventions.md`.

  - Slot placeholders (`<Product>`, `<product>`, `<org>`, `<product-domain>`, `com.<org>.<product>`, `<Product>Api`) are NOT drift against reference-repo literals.
  - Canonical example types (`Note`, `Tag`, `Item` and their fan-out: `NoteEntity`, `NoteRepository`, `NoteFeature`, `NotePack`, `NotesRouter`, etc.) are NOT drift when the live code has `Training`/`Exercise` instead — they teach the pattern, not the product.
  - Generic numeric format-state (`AmountFormatState`) is NOT drift against product-specific `WeightFormatState`/`HeightFormatState`/etc. in live code.
  - Reserved names (Base*, AppTokens, UiText, DialogConfig, etc. — see `04-glossary.md` "Reserved names" / `05-template-conventions.md` §7) must match live code verbatim. The template substitution does NOT touch them.

  In Step 3.2 (verbatim code-block verification): a doc using a placeholder against live code using the concrete name is intentional substitution, NOT a single-character mismatch. Verify the surrounding signature, generics, modifiers, and structure against live code; ignore the slot/example-type identifier itself.

  If a chapter contains a concrete reference-repo name (e.g. `GrippoApi`, `TrainingResponse`, `WeightFormatState`, `ProfileBodyState`, `com.grippo.*`) — that is a `PRODUCT_LEAKAGE` finding owned by `invalidate-templatize.md`. Flag in the report; do NOT patch here.

- This pass does NOT audit `requirements/sub-agents/` or `requirements/tasks/`. Drift there → owned by `invalidate-sub.md`.
````

---

## Audit log

Lowest count wins. Ties → table order. After each pass, the agent increments count and sets Last audited.

| Chapter | Count | Last audited |
|---|---|---|
| 00-overview | 3 | 2026-05-15 |
| 01-tech-stack | 3 | 2026-05-15 |
| 02-module-structure | 4 | 2026-05-15 |
| 03-architecture-patterns | 3 | 2026-05-15 |
| 04-base-classes | 3 | 2026-05-15 |
| 05-design-system | 3 | 2026-05-15 |
| 06-data-layer | 3 | 2026-05-15 |
| 07-mappers | 3 | 2026-05-15 |
| 08-dependency-injection | 3 | 2026-05-15 |
| 09-conventions | 3 | 2026-05-16 |
| 10-toolkit | 3 | 2026-05-16 |
| 11-state-and-formatters | 3 | 2026-05-16 |
| 12-gradle-build | 3 | 2026-05-16 |
| 13-anti-patterns | 2 | 2026-05-15 |
| 14-cookbook | 2 | 2026-05-15 |

Round complete when all rows share the same count. Next pass picks the lowest again (top-to-bottom on a tie).
