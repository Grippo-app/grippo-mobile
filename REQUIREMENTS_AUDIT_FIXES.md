# Requirements Audit — Fix Prompts

Список самодостаточных промптов, каждый из которых можно скопировать целиком в новый чат с ИИ. Промпты независимы: можно запускать в любом порядке.

Все фиксы относятся к папке `requirements/` в репо `grippo-mobile` (KMP-шаблон для нового проекта). Папки `requirements/tasks/` и `requirements/sub-agents/` промптами **не трогаются**.

Перед тем как принять правки агента — проверь diff: ниже даны ровно те правки, которые нужны.

---

## PROMPT 1 — Sync stale «cleanup-pending» language in meta-files

````
You are editing the `requirements/` folder of a Kotlin Multiplatform mobile architecture template at `/Users/maxvoitenko/Projects/Pet/grippo-mobile/`. The template has already converged: the cross-file worked example was rewritten from "Workout history" to "Note archive", and `requirements/06-data-layer/03-grippo-api-and-dtos.md` was renamed to `requirements/06-data-layer/03-product-api-and-dtos.md`. All chapter content uses placeholders (`<Product>`, `<product>`, `<org>`, `<product-domain>`, `com.<org>.<product>`).

Several meta-files still describe these cleanups as **pending**. Your job: bring them in sync with reality.

### Background context (do NOT change these)

- `requirements/00-overview/03-project-config.md` frontmatter intentionally keeps Grippo values (it is the per-project edit point).
- `requirements/00-overview/05-template-conventions.md` legitimately documents the substitution table with Grippo as the "before" example.
- `requirements/00-overview/04-glossary.md` line 59 is allowed to mention `GrippoApi` / `Training*` / etc. as illustrations.
- `requirements/README.md` legitimately keeps the Replacement-checklist table mentioning `com.grippo`, `Grippo`, `grippo-mobile`, `grippo-app.com`.

### Edits to apply

**1. `requirements/invalidate-templatize.md`** — rewrite the "Coordination with other invalidates" section (the last block of the file, starts with `## Coordination with other invalidates`). Replace the three bullets that say `invalidate.md and invalidate-sub.md are PAUSED`, the structural-wrongness note, and the "final cleanup prompt" handles-rename note with a single post-convergence statement: pause is lifted, the final cleanup (Note-archive rewrite + `03-grippo-api-and-dtos.md` rename + `RatingPicker → TagPicker`) is done. Keep the section heading. Keep the "Converged 2026-05-16" paragraph above it untouched.

**2. `requirements/invalidate-templatize.md` §3.3 table** — the table classifies leakage types. Two rows now describe completed work:
- Row `| Worked-example leakage | ... | **FLAG, do not patch** ...`
- Row `| File-rename hint | ... | **FLAG, do not patch** ...`

Update both rows to reflect that this work is now historical context for future templatize passes (e.g. change "**FLAG, do not patch**" to "**Historical — completed in convergence pass; future passes treat as substitution if encountered**"). Keep all other rows of the table untouched.

**3. `requirements/00-overview/05-template-conventions.md` §9 "Out of scope for templatize"** — two bullets describe pending work:
- `Rename `.md` files (e.g. `06-data-layer/03-grippo-api-and-dtos.md` → `03-product-api-and-dtos.md`). Flag; handle in the final cleanup prompt.`
- `Rewrite the cross-file worked example (§3). Flag; handle in the final cleanup prompt.`

Delete both bullets. The remaining bullets in §9 (Reorganize chapters / Touch project-config / Touch live code) stay.

**4. `requirements/00-overview/05-template-conventions.md` §3 "Canonical worked example"** — the section ends with: `The recipe steps are identical; only the names change. The four files that share this example MUST be rewritten in a single coordinated pass (cross-file consistency). The templatize loop flags this work but does NOT auto-patch it; finish it with one manual cleanup prompt at the end.`

Replace this final sentence with a past-tense note: `The four files that share this example were rewritten in a single coordinated pass during the templatize-convergence cleanup; chapters now use the "Note archive" form consistently.`

**5. `requirements/README.md` line 9** — the bullet reads:
> All Kotlin code in the documents is a **reference implementation**, not a literal copy-paste target. Replace `com.grippo.*` package roots, `Grippo` class prefixes, and domain types (Training, Exercise, etc.) with the new project's names.

This is stale: chapters no longer contain `com.grippo.*` literals or `Training`/`Exercise` types — only placeholders. Rewrite to: `All Kotlin code in the documents is a **reference implementation**, not a literal copy-paste target. Substitute the placeholders (`<Product>`, `<org>`, `<product>`, `<product-domain>`, `com.<org>.<product>`) with the new project's values per the Replacement checklist below and per `00-overview/05-template-conventions.md` §1.`

**6. `requirements/00-overview/04-glossary.md` line 59** — current text:
> Product-specific names (e.g. `GrippoApi`, `Training*`, `WeightFormatState`, `ProfileBody*`) **should** be replaced — see `00-overview/05-template-conventions.md` for the substitution table.

The phrasing "should be replaced" implies these names still appear in chapter bodies. They don't. Rewrite to: `The reference repo uses concrete product-specific names (e.g. `GrippoApi`, `Training*`, `WeightFormatState`, `ProfileBody*`). Chapters in this folder use placeholder / canonical-example equivalents instead; see `00-overview/05-template-conventions.md` for the substitution table.`

### Verification

After editing, run:

```
cd /Users/maxvoitenko/Projects/Pet/grippo-mobile/requirements
grep -n "PAUSED\|FLAG, do not patch\|finish it with one manual cleanup prompt\|should\s*\*\*be\*\*\s*replaced" \
  invalidate-templatize.md 00-overview/05-template-conventions.md 00-overview/04-glossary.md README.md
grep -n "03-grippo-api-and-dtos" 00-overview/05-template-conventions.md
```

The first grep should return at most the unchanged "Reasoning" rows of the §3.3 table (if any). The second grep should return zero matches.

Do not touch anything outside the six edits listed.
````

---

## PROMPT 2 — Clean up code-example hygiene leaks

````
You are editing the `requirements/` folder of a Kotlin Multiplatform mobile architecture template at `/Users/maxvoitenko/Projects/Pet/grippo-mobile/`. The template has been templatized to product-agnostic form; chapters use placeholders (`<Product>`, `<product>`, `<org>`) and canonical example types (`Note`, `Tag`, `Item`, `AmountFormatState`).

A few small example-code leaks remain. Fix them precisely as described. Do NOT refactor anything else.

### Edits

**1. `requirements/05-design-system/08-shared-components.md` lines ~282–285** — current block:

```kotlin
@Stable public sealed interface ChipStype {   // sic — this is the typo'd name in the reference repo
    @Stable public data object Default : ChipStype
    @Stable public data class Clickable(val onClick: () -> Unit) : ChipStype
}
```

Plus the trailing prose line: `Pass `stype = ChipStype.Clickable { ... }` to make a chip interactive; ...`

A template MUST NOT propagate reference-repo typos to downstream projects. Rename `ChipStype` → `ChipStyle` and `stype` → `style` throughout this file, and delete the `// sic ...` comment. Use `replace_all` on the file for each token to catch every occurrence.

**2. `requirements/05-design-system/07-preview-container.md` ~line 89** — current preview state literal:

```kotlin
amount = AmountFormatState(value = 72.0, unit = "kg"),
```

The unit `"kg"` leaks a fitness-product hint. Replace with a neutral literal: `amount = AmountFormatState(value = 72.0, unit = "unit"),`

**3. `requirements/09-conventions/02-naming.md` ~line 29** — the Use-case row of the naming table lists examples including `CreateProfileUseCase`. Replace the example list so it sticks to the canonical types (Note / User / Tag / Item). Specifically, change `CreateProfileUseCase` to `CreateUserUseCase`. The full row should keep its structure; only the one identifier swap is needed.

**4. `requirements/05-design-system/02-app-color.md` ~line 102** — current sentence:

> The structure stays mostly **two levels deep** — `AppTokens.colors.button.backgroundPrimary1`, `AppTokens.colors.text.primary`. A handful of groups (`profile.experience.beginner`, `example.category.compound`, `charts.ring.success.indicator`) go three levels for sub-domains; don't go deeper than three — flatten when in doubt.

`profile.experience.beginner` is a reference-repo path. Replace it with a neutral example like `<feature>.<group>.<state>` (e.g. `payment.tier.gold`). Keep the surrounding structure of the sentence. Acceptable replacement form: `A handful of groups (e.g. `<feature>.<group>.<state>`, `charts.ring.success.indicator`) go three levels for sub-domains; ...`

**5. `requirements/08-dependency-injection/02-koin-annotations.md` lines ~7–13** — current snippet has an inline TODO:

```kotlin
extensions.getByType<KspExtension>().apply {
    arg("KOIN_CONFIG_CHECK", "false")  // TODO wait until next version of Koin Annotations
}
```

Plus the prose: `KOIN_CONFIG_CHECK = false` disables a sanity check that doesn't yet handle some Koin Annotations 2.3.1 cases. This is a temporary workaround — re-enable when the upstream issue is fixed.`

In a template, an inline TODO referencing a specific Koin Annotations version is awkward. Two-part fix:
- Remove the `// TODO wait until next version of Koin Annotations` comment from the snippet (keep the line itself).
- Soften the prose to drop the hard-coded `2.3.1`: `KOIN_CONFIG_CHECK = false` disables a sanity check that does not yet handle every Koin Annotations case the project relies on. Re-enable once the upstream issue you hit is fixed; if it never bites you, leave it off.`

### Verification

After editing, run:

```
cd /Users/maxvoitenko/Projects/Pet/grippo-mobile/requirements
grep -n "ChipStype\|stype" 05-design-system/08-shared-components.md
grep -n '"kg"' 05-design-system/07-preview-container.md
grep -n "CreateProfileUseCase" 09-conventions/02-naming.md
grep -n "profile.experience" 05-design-system/02-app-color.md
grep -n "TODO wait until next version\|2.3.1" 08-dependency-injection/02-koin-annotations.md
```

All five greps must return zero matches.

Do not touch anything outside the five edits.
````

---

## PROMPT 3 — Fix BaseModels generic-parameter abbreviations

````
You are editing one file in the `requirements/` folder of a Kotlin Multiplatform architecture template at `/Users/maxvoitenko/Projects/Pet/grippo-mobile/`.

### Edit

`requirements/04-base-classes/04-base-models.md` lines ~150–158 contain a table titled "Why these interfaces exist". Two rows use the abbreviation `BaseViewModel<S, D, L>` / `BaseComponent<D>`:

```
| `BaseDirection` | Type constraint on `BaseViewModel<S, D, L>` and `BaseComponent<D>`'s `eventListener` |
| `BaseLoader`    | Type constraint on `BaseViewModel<S, D, L>`; ensures only @Immutable types in loaders |
```

The rest of the chapter (and `requirements/04-base-classes/01-base-viewmodel.md`, `02-base-component.md`) uses the full names `<STATE, DIRECTION, LOADER>`. Replace `BaseViewModel<S, D, L>` with `BaseViewModel<STATE, DIRECTION, LOADER>` and `BaseComponent<D>` with `BaseComponent<DIRECTION>` in both affected rows.

### Verification

```
cd /Users/maxvoitenko/Projects/Pet/grippo-mobile/requirements
grep -n "BaseViewModel<S, D, L>\|BaseComponent<D>" 04-base-classes/04-base-models.md
```

Must return zero matches.

Do not touch any other content.
````

---

## PROMPT 4 — Decide and apply `03-project-config.md` defaults policy

> Этот промпт начинается с вопроса пользователю. Если уже решил — пропусти Step 0 и сразу следуй выбранному варианту.

````
You are editing `requirements/00-overview/03-project-config.md` in a Kotlin Multiplatform architecture template at `/Users/maxvoitenko/Projects/Pet/grippo-mobile/`.

The file is the per-project edit point — a frontmatter block that downstream sub-agents read to know `productName`, `apiClassName`, `backendHost`, etc. Currently its values are the reference repo's literal Grippo values:

```yaml
productName: Grippo
productPackage: com.grippo
apiClassName: GrippoApi
backendHost: grippo-app.com
applicationId: com.grippo.android
iosFrameworkName: shared
iosEnabled: true
firebaseEnabled: true
codexEnabled: auto
prelaunch: false
supportedLocales:
  - en
  - uk
  - ru
typefaceFactory: manrope
featuresWithRootComponentSuffix:
  - home
  - trainings
diHandWrittenModules:
  - GoogleAuthModule
  - AppleAuthModule
```

For a template that gets dropped into a fresh project, this couples the default config to Grippo. The user must decide which posture to take.

## Step 0 — pick the posture (ask the user)

Ask exactly:

> The frontmatter of `requirements/00-overview/03-project-config.md` currently ships with Grippo's literal values. Two postures: (A) **Neutralize** the frontmatter to placeholder defaults (`<Product>`, `<product-domain>.com`, empty lists) so a fresh project starts from a clean slate. (B) **Keep** the Grippo values as a sample and strengthen the docstring + `requirements/README.md` to make the "edit before bootstrapping" instruction more prominent. Which do you want — A or B?

Wait for the answer. Do not assume.

## Step 1A — if "A" (neutralize)

Replace the frontmatter with:

```yaml
---
productName: <Product>
productPackage: com.<org>.<product>
apiClassName: <Product>Api
backendHost: <product-domain>.com
applicationId: com.<org>.<product>.android
iosFrameworkName: shared
iosEnabled: true
firebaseEnabled: true
codexEnabled: auto
prelaunch: true
supportedLocales:
  - en
typefaceFactory: <typeface>
featuresWithRootComponentSuffix: []
diHandWrittenModules: []
---
```

Then, in the body of the same file, add one short paragraph immediately after the `# Project config — single source of truth` heading: `Replace every value in the frontmatter above before the first bootstrap. Placeholders use the same syntax as `00-overview/05-template-conventions.md` §1.`

Verify: `grep -n "Grippo\|grippo" requirements/00-overview/03-project-config.md` returns zero matches.

## Step 1B — if "B" (keep + strengthen)

Leave the frontmatter as is. Make two doc edits:

1. In `requirements/00-overview/03-project-config.md`, immediately after the `# Project config — single source of truth` heading, replace the existing first paragraph (the one starting `Sub-agents under `requirements/sub-agents/` read this file before acting.`) with a stronger warning: `**The frontmatter above is the Grippo reference repo's literal values, shipped as a working example.** Before bootstrapping a new project, replace every field. Sub-agents under `requirements/sub-agents/` read this file before acting; they do not detect that the values still match the reference repo.`

2. In `requirements/README.md`, find the paragraph beginning `Per-project values (productName, locales, prelaunch flag, etc.) live in `requirements/00-overview/03-project-config.md`.` (currently a single sentence). Expand it to: `Per-project values (productName, locales, prelaunch flag, etc.) live in `requirements/00-overview/03-project-config.md`. **That file currently ships with the reference repo's Grippo values as a working sample. Edit every frontmatter field before invoking `launch.md` or any sub-agent.** Sub-agents read it lazily and do not check for unreplaced Grippo defaults.`

Verify: `grep -n "ships with the reference repo's Grippo values" requirements/README.md requirements/00-overview/03-project-config.md` returns two matches (one per file).

## Constraint

Apply only the chosen branch (A or B). Do not edit anything else.
````

---

## После всех правок

```bash
cd /Users/maxvoitenko/Projects/Pet/grippo-mobile
# Sanity check — strict leakage signatures still return only documented exceptions:
grep -rln -E '(grippo|TrainingResponse|TrainingEntity|MuscleEntity|EquipmentEntity|WeightFormatState|HeightFormatState|ProfileBodyState|WorkoutHistory|RatingPicker|MuscleColors|MuscleLoad|TrainingStreak|GoalProgress|stubTraining|stubWeightHistory|grippo-app\.com|com\.grippo)' \
  requirements/ --include='*.md' \
  | grep -v 'requirements/tasks/' \
  | grep -v 'requirements/sub-agents/' \
  | grep -v '03-project-config.md' \
  | grep -v '05-template-conventions.md' \
  | grep -v 'requirements/README.md' \
  | grep -v '04-glossary.md'
```

Должно вернуть **пусто**. Если есть строки — открой файл, проверь, intentional ли это, и при необходимости вынеси отдельным промптом.
