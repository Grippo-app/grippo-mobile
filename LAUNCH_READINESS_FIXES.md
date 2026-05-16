# Launch Readiness Fixes — copy-paste prompts

> **Maintenance tooling for the grippo-mobile reference repo.** Do not copy this file to a new project bootstrapped from `requirements/`. Portable audit prompts live at `requirements/invalidate*.md`.

Аудит `requirements/tasks` и `requirements/sub-agents` показал, что система **почти готова** к bootstrap нового KMP-проекта, но есть критичные мостики между `launch.md` → `03-project-config.md` → sub-agents, которые сорвут первую задачу.

Каждый раздел ниже = один самодостаточный промт. Алгоритм:

1. Открой новый чат Claude Code в **корне этого проекта** (CWD должен содержать `requirements/sub-agents/`).
2. Выбери раздел (см. порядок ниже).
3. Скопируй содержимое внутреннего `text`-блока полностью.
4. Вставь в чат, отправь.
5. После завершения сверь по секции **Acceptance** в самом промте.

После применения всех — пайплайн готов к работе на новом проекте.

---

## Порядок применения

```
КРИТИЧНЫЕ (обязательно до bootstrap):
  Prompt 1   launch.md ↔ project-config bridge      независимый
  Prompt 2   templatize 03-project-config.md         независимый
  Prompt 3   orchestrator.md Step 0 bugs             независимый
  Prompt 4   screen-builder.md description + flow    независимый

ВАЖНЫЕ (улучшают качество):
  Prompt 5   refresh invalidate-templatize log       независимый
  Prompt 6   verify codex-plugin-cc reality          research, независимый
  Prompt 7   document tooling vs portable files      независимый
  Prompt 8   seed tasks/ with example TASK file      независимый

МЕЛКИЕ:
  Prompt 9   CLAUDE.md handoff в launch.md           независимый
```

Все промпты независимы — можно запускать параллельно в разных чатах. Только не применять одновременно к одному файлу (Prompts 3 и 4 трогают разные файлы, безопасно).

---

## Prompt 1 — Make `03-project-config.md` mandatory in `launch.md`

> Effort: ~20 min · Risk: low · Depends on: none

**Что чинит:** `launch.md` Step 14 говорит «Optionally also fill in `03-project-config.md`», но каждый sub-agent (orchestrator, builders, validators, reviewers) читает этот файл и без корректных значений падает или генерит мусор. Это критичный логический разрыв.

````text
# Goal

The repo at the current working directory contains `requirements/launch.md` — a bootstrap prompt for new Kotlin Multiplatform projects. The prompt currently treats `requirements/00-overview/03-project-config.md` as optional, but every sub-agent in `requirements/sub-agents/` reads it as a required input. Fix the contradiction by making project-config setup a mandatory, explicit step in `launch.md`, with a clear mapping from the Step 0 clarifying questions to the frontmatter fields the agents consume.

# Context

Read first:
- `requirements/launch.md` — the bootstrap prompt (target of this fix).
- `requirements/00-overview/03-project-config.md` — the config file the agents consume; note its frontmatter fields.
- `requirements/sub-agents/README.md` — see "Sub-agents read `03-project-config.md` before each task" (top of file).
- `requirements/sub-agents/helpers/orchestrator.md` Step 0 — depends on `apiClassName`, `iosEnabled`, `firebaseEnabled`.
- `requirements/sub-agents/builders/data-service-scaffold-builder.md` — depends on `apiClassName`, `productPackage`, `backendHost`, `productName`, `iosEnabled`.
- `requirements/sub-agents/builders/resource-builder.md` — depends on `supportedLocales`, `typefaceFactory`.

The frontmatter fields the agents read:
`productName`, `productPackage`, `apiClassName`, `backendHost`, `applicationId`, `iosFrameworkName`, `iosEnabled`, `firebaseEnabled`, `codexEnabled`, `prelaunch`, `supportedLocales`, `typefaceFactory`, `featuresWithRootComponentSuffix`, `diHandWrittenModules`.

# Steps

## 1. Map Step 0 questions to frontmatter fields

In `launch.md`, find Step 0 (`## Step 0 — gather context`). Right after the existing numbered question list (currently 8 questions), insert a sub-section titled `### Field mapping` with a table:

```
| Step 0 question                | 03-project-config.md field        |
|---|---|
| Product name                   | productName, productPackage (lowercased), apiClassName (productName + "Api") |
| Organization name              | productPackage (com.<org>.<product>) |
| Backend host                   | backendHost |
| Application ID                 | applicationId |
| First product domain           | (informational — agents do not read this; used in Step 8) |
| Locales to support             | supportedLocales (YAML list) |
| Auth methods                   | diHandWrittenModules (append GoogleAuthModule / AppleAuthModule per choice) |
| Firebase                       | firebaseEnabled |
```

Plus four fields the user does not get asked about but MUST be set:
- `iosEnabled` — default `true`; false only if the project intentionally drops iOS.
- `codexEnabled` — default `auto`.
- `prelaunch` — default `true` for a fresh project (room-migration-builder allows destructive fallback). Flip to `false` when the app ships.
- `iosFrameworkName` — default `shared`.
- `typefaceFactory` — name the project picks for its typeface factory function (e.g. `inter`, `roboto`). Default placeholder `<typeface>` until decided.
- `featuresWithRootComponentSuffix` — start as `[]`; the orchestrator updates it when a feature is forced into the suffixed form.

## 2. Promote project-config filling to a new explicit step

Move project-config filling FROM Step 14 ("Install sub-agents") TO a new Step 1.5, inserted between Step 1 (read the requirements) and Step 2 (initialize the build system).

The new step's body:

```
## Step 1.5 — populate 03-project-config.md

Open `requirements/00-overview/03-project-config.md`. Replace every value in the YAML frontmatter with the project-specific value from Step 0, per the field-mapping table. For fields the user did not specify, use the defaults noted in Step 0's mapping table.

Verify:
- No value in the frontmatter still reads `<Product>`, `<product>`, `<org>`, `<product-domain>`, or any other placeholder.
- `featuresWithRootComponentSuffix` and `diHandWrittenModules` are empty `[]` or contain only modules the project actually intends to ship.
- `prelaunch: true` for an unreleased project; `firebaseEnabled` matches the Step 0 answer.

Do NOT proceed to Step 2 until 03-project-config.md is fully populated. Every sub-agent invoked later reads this file.
```

## 3. Update Step 14 (sub-agent install)

Find the existing line in Step 14: `Optionally also fill in requirements/00-overview/03-project-config.md with the answers gathered in Step 0.`

Replace with: `Verify requirements/00-overview/03-project-config.md was populated in Step 1.5. If you skipped Step 1.5 for any reason, do it now before invoking any sub-agent — they will all fail with BLOCKED otherwise.`

## 4. Update Step 0's closing instruction

Find the line at the end of Step 0: `Save the answers as a reference; reuse them through every step.`

Replace with: `Save the answers AND copy them into 03-project-config.md per the field-mapping table above in Step 1.5. The answers are not just a reference — they become the runtime configuration every sub-agent reads.`

# Acceptance

- `launch.md` contains a new Step 1.5 titled "populate 03-project-config.md".
- Step 0 contains a `### Field mapping` table with all 8 questions mapped.
- The word "Optionally" no longer appears anywhere near `03-project-config.md` in the file.
- Step 14's wording shifts from "fill in optionally" to "verify it was filled in earlier".

# Report back

When done, post:

1. Files edited (paths).
2. The exact wording inserted as Step 1.5 (verbatim).
3. Confirmation: `rg -i 'optional' requirements/launch.md | rg 'project-config|03-project'` returns zero lines.

# Constraints

- Do not change Step 0's question list (only ADD the field mapping below it).
- Do not change Steps 2–13's content (only Step 1.5 inserted, Step 14 reworded).
- Do not modify `requirements/00-overview/03-project-config.md` — that's Prompt 2's job.
- Do not modify any sub-agent file.
- Preserve the existing markdown style and line endings.
````

---

## Prompt 2 — Templatize `03-project-config.md` to neutral defaults

> Effort: ~15 min · Risk: low · Depends on: none

**Что чинит:** Frontmatter в `03-project-config.md` сейчас содержит Grippo-конкретные значения (`productName: Grippo`, `featuresWithRootComponentSuffix: [home, trainings]`, `diHandWrittenModules: [GoogleAuthModule, AppleAuthModule]`). Если пользователь забудет переписать — sub-agents будут искать несуществующие фичи и игнорировать ненужные DI-исключения.

````text
# Goal

Rewrite `requirements/00-overview/03-project-config.md` frontmatter to use template placeholders and safe fresh-project defaults instead of the current Grippo-specific values. Add an explicit top-of-file banner so a new project's bootstrapper knows to edit this file before invoking any sub-agent.

# Context

Read first:
- `requirements/00-overview/03-project-config.md` — the file to rewrite.
- `requirements/00-overview/05-template-conventions.md` — defines the placeholder vocabulary (`<Product>`, `<product>`, `<org>`, `<product-domain>`).
- `requirements/sub-agents/README.md` — confirms sub-agents read this file before each task.

# Steps

## 1. Rewrite the frontmatter

Open `requirements/00-overview/03-project-config.md`. Replace the entire frontmatter block (between the two `---` delimiters at the top) with:

```yaml
---
productName: <Product>
productPackage: com.<org>.<product>
apiClassName: <Product>Api
backendHost: <product-domain>.com
applicationId: com.<org>.<product>.android
iosFrameworkName: shared
iosEnabled: true
firebaseEnabled: false
codexEnabled: auto
prelaunch: true
supportedLocales:
  - en
typefaceFactory: <typeface>
featuresWithRootComponentSuffix: []
diHandWrittenModules: []
---
```

Rationale for the changes:
- `productName`, `productPackage`, `apiClassName`, `backendHost`, `applicationId` — template placeholders matching `05-template-conventions.md` §1.
- `iosFrameworkName: shared` — convention from `05-template-conventions.md`.
- `iosEnabled: true` — assume iOS-included; flip to false only if the project intentionally drops iOS.
- `firebaseEnabled: false` — safer default; many fresh projects don't have Firebase set up day one.
- `codexEnabled: auto` — orchestrator falls back to `internal-reviewer` if Codex plugin missing.
- `prelaunch: true` — `room-migration-builder` allows destructive fallback until shipped.
- `supportedLocales: [en]` — minimal single-locale start.
- `typefaceFactory: <typeface>` — placeholder until project picks a font.
- `featuresWithRootComponentSuffix: []` — empty; populated only when needed by orchestrator.
- `diHandWrittenModules: []` — empty; populated when project adds hand-written Koin modules.

## 2. Add a top-of-file banner

Immediately AFTER the closing `---` of the new frontmatter, BEFORE the existing `# Project config — single source of truth` heading, insert:

```markdown
> **Fresh-project state.** Every value in the frontmatter above is a placeholder or a neutral default. Before invoking any sub-agent (`orchestrator`, builders, validators), replace every `<placeholder>` with project-specific values per `requirements/launch.md` Step 1.5. Empty arrays (`featuresWithRootComponentSuffix: []`, `diHandWrittenModules: []`) stay empty until the project actually needs them — sub-agents update them on demand.
```

## 3. Update the "Field meanings" section

Find the bullet for `featuresWithRootComponentSuffix` and `diHandWrittenModules` (the last two bullets in the section). Append to each:

For `featuresWithRootComponentSuffix`: `Start empty (\`[]\`) on a fresh project; the orchestrator appends a feature name only when its first sub-screen collides with the feature root.`

For `diHandWrittenModules`: `Start empty (\`[]\`) on a fresh project; append a module name only when a hand-written \`module { … }\` block is deliberately introduced (typically platform-edge wrappers like Google/Apple auth).`

# Acceptance

- All 15 frontmatter values use placeholders, empty arrays, or neutral fresh-project defaults.
- The string `Grippo` no longer appears in the frontmatter (it can still appear in the body's reference examples — that's documented in `05-template-conventions.md`).
- A banner immediately after the frontmatter explains the file is in fresh-project state and points to `launch.md` Step 1.5.
- The "Field meanings" bullets for `featuresWithRootComponentSuffix` and `diHandWrittenModules` explain fresh-project handling.

Verify with:

```
rg 'Grippo|GoogleAuthModule|AppleAuthModule' requirements/00-overview/03-project-config.md
```

This should return zero hits in the frontmatter block. (A hit in the body is acceptable if it's a clear "example" reference.)

# Report back

When done, post:

1. The new frontmatter block verbatim.
2. The new banner verbatim.
3. The output of the `rg` command above.

# Constraints

- Do not delete fields or rename them. Frontmatter shape stays exactly the same — only values change.
- Do not change the section headings or the "Updating" section at the bottom.
- Preserve YAML indentation (two spaces under `supportedLocales:`).
- Do not modify any other file. This prompt is single-file scope.
````

---

## Prompt 3 — Fix `orchestrator.md` Step 0 bootstrap-check bugs

> Effort: ~20 min · Risk: low · Depends on: none

**Что чинит:** Два бага в bootstrap-check:
1. `RootRouter.kt` помечен как файл `:shared`, но реально живёт в `:ui-screen-features:screen-api`. Step 0 будет валить здоровые проекты.
2. `test -f .../**/Koin.kt` использует `**` без `shopt -s globstar` — glob не рекурсирует, проверка молча врёт.

````text
# Goal

Fix two bugs in `requirements/sub-agents/helpers/orchestrator.md` Step 0 ("Bootstrap check"):

1. The required-artifacts list says `:shared` contains `RootRouter.kt`, but per `requirements/02-module-structure/` and `feature-module-scaffold-builder.md` Step 4, `RootRouter` lives in `:ui-screen-features:screen-api`. The check would incorrectly fail a healthy project.

2. The verification bash uses `test -f <path>/**/<file>` patterns, which require `shopt -s globstar` to recurse. Without it, `**` collapses to `*` (one directory level), and even with it, `test -f` only accepts a single path argument — multi-match globs silently degrade.

# Context

Read first:
- `requirements/sub-agents/helpers/orchestrator.md` — full file, focus on Step 0 (lines roughly 20–46).
- `requirements/sub-agents/builders/feature-module-scaffold-builder.md` Step 4 — confirms `RootRouter.kt` lives in `:ui-screen-features:screen-api`.
- `requirements/02-module-structure/02-dependency-rules.md` — confirms module ownership.
- `requirements/launch.md` Step 9 — confirms what `:shared` ships with.

# Steps

## 1. Fix the required-artifacts list

Find the bullet in orchestrator.md Step 0:

> `- \`:shared\` module exists with \`Koin.kt\`, \`RootComponent.kt\`, \`RootRouter.kt\`, \`RootDirection.kt\`, \`RootContract.kt\`.`

Replace with two bullets:

```
- `:shared` module exists with `Koin.kt`, `RootComponent.kt`, `RootDirection.kt`, `RootContract.kt`.
- `:ui-screen-features:screen-api` module exists with `RootRouter.kt`.
```

## 2. Fix the bash verification block

Find the bash block in orchestrator.md Step 0 (currently uses `test -f .../**/<file>.kt` patterns):

```bash
PROJECT_API=$(rg -m1 '^apiClassName:' requirements/00-overview/03-project-config.md | awk '{print $2}')
test -f settings.gradle.kts || echo "MISSING: settings.gradle.kts"
test -f shared/src/commonMain/kotlin/**/Koin.kt || echo "MISSING: :shared/Koin.kt"
test -f data-services/backend/src/commonMain/kotlin/**/${PROJECT_API}.kt || echo "MISSING: ${PROJECT_API}.kt"
# ...etc
```

Replace the entire bash block with:

```bash
PROJECT_API=$(rg -m1 '^apiClassName:' requirements/00-overview/03-project-config.md | awk '{print $2}')
IOS_ENABLED=$(rg -m1 '^iosEnabled:' requirements/00-overview/03-project-config.md | awk '{print $2}')
FIREBASE_ENABLED=$(rg -m1 '^firebaseEnabled:' requirements/00-overview/03-project-config.md | awk '{print $2}')

check_exists() {
  local root="$1" name="$2" label="$3"
  find "$root" -name "$name" -print -quit 2>/dev/null | grep -q . \
    || echo "MISSING: $label"
}

test -f settings.gradle.kts || echo "MISSING: settings.gradle.kts"
check_exists shared/src/commonMain/kotlin Koin.kt ":shared/Koin.kt"
check_exists shared/src/commonMain/kotlin RootComponent.kt ":shared/RootComponent.kt"
check_exists ui-screen-features/screen-api/src/commonMain/kotlin RootRouter.kt ":ui-screen-features:screen-api/RootRouter.kt"
check_exists data-services/backend/src/commonMain/kotlin "${PROJECT_API}.kt" "${PROJECT_API}.kt"
check_exists data-services/database/src/commonMain/kotlin Database.kt ":data-services:database/Database.kt"
check_exists design-system/resources/provider/src/commonMain/kotlin StringProvider.kt ":design-system:resources:provider/StringProvider.kt"

# Locale check — each supportedLocales entry needs a values-<lang>/strings.xml.
LOCALES=$(awk '/^supportedLocales:/{flag=1; next} /^[a-z]/{flag=0} flag && /^  - /{print $2}' requirements/00-overview/03-project-config.md)
for lang in $LOCALES; do
  case "$lang" in
    en) dir="values" ;;
    *)  dir="values-$lang" ;;
  esac
  find design-system/resources/provider/src/commonMain/composeResources/$dir -name strings.xml -print -quit 2>/dev/null | grep -q . \
    || echo "MISSING: composeResources/$dir/strings.xml (locale '$lang')"
done

# Optional gates per project-config flags.
[ "$IOS_ENABLED" = "true" ] && { [ -d iosApp ] || echo "MISSING: iosApp/ (iosEnabled=true)"; }
[ "$FIREBASE_ENABLED" = "true" ] && { [ -f androidApp/google-services.json ] || echo "MISSING: androidApp/google-services.json (firebaseEnabled=true)"; }
```

Why this works:
- `find ... -print -quit | grep -q .` — terminates on the first match, returns 0 if found, 1 otherwise. Recurses naturally. No `globstar` dependency.
- `check_exists` is a shell function that wraps the pattern with a consistent label.
- The locale loop uses the convention from `requirements/05-design-system/`: `en` → `values/`, others → `values-<lang>/`.
- The `iosEnabled` / `firebaseEnabled` gates respect the project-config flags from Prompt 2's defaults.

## 3. Update the comment after the bash block

Currently the orchestrator says: `If any fail → \`BLOCKED: run requirements/launch.md to bootstrap the project first\`.`

Leave that sentence unchanged — it's correct.

# Acceptance

- `RootRouter.kt` is listed under `:ui-screen-features:screen-api`, not `:shared`.
- The bash block uses `find ... -print -quit | grep -q .` for every recursive check.
- The bash block reads `iosEnabled` and `firebaseEnabled` from project-config and gates the iOS/Firebase checks accordingly.
- The bash block iterates `supportedLocales` to verify each locale's `strings.xml` exists.

Verify the bash block parses by piping it to `bash -n` (syntax check):

```
sed -n '/^```bash$/,/^```$/p' requirements/sub-agents/helpers/orchestrator.md \
  | grep -v '^```' | bash -n -
```

This should exit 0.

# Report back

When done, post:

1. The replacement bullet (verbatim).
2. The new bash block (verbatim).
3. The exit status of the `bash -n` check above.

# Constraints

- Do not modify any other step of `orchestrator.md` (Steps 1–6 stay as-is).
- Do not touch any other agent file.
- Preserve the existing markdown style and the surrounding prose.
- The fix is mechanical — do not redesign the bootstrap-check or add new validations beyond the locale / iOS / Firebase gates noted above.
````

---

## Prompt 4 — Fix `screen-builder.md` description + document single→multi-screen conversion

> Effort: ~30 min · Risk: low · Depends on: none

**Что чинит:** Два пробела в `screen-builder.md`:
1. Frontmatter `description` упоминает «escalates to data-feature-builder + a new screen-feature scaffold» — устаревший путь. Реальный chain — `feature-module-scaffold-builder` → `screen-builder`. `task-intake` читает description для роутинга — устаревший текст направит криво.
2. Сам `screen-builder` не описывает шаг перевода feature root из single-screen (Debug-style) в multi-screen, когда добавляется первый sub-screen в свежесозданный модуль. Эта работа целиком лежит в hand-off ноте `feature-module-scaffold-builder` — хрупко.

````text
# Goal

Fix two issues in `requirements/sub-agents/builders/screen-builder.md`:

1. The frontmatter `description` field references an outdated escalation path. The correct chain is `feature-module-scaffold-builder` → `screen-builder`; the description currently says `data-feature-builder + a new screen-feature scaffold (a wider task)`, which misroutes `task-intake`.

2. The builder's steps do not cover converting a single-screen (Debug-style) feature root to multi-screen shape when adding the first sub-screen to a freshly-scaffolded feature module. This conversion is documented only in the hand-off note from `feature-module-scaffold-builder`, which is fragile — `screen-builder` may not read its predecessor's report carefully.

# Context

Read first:
- `requirements/sub-agents/builders/screen-builder.md` — target of this fix.
- `requirements/sub-agents/builders/feature-module-scaffold-builder.md` — Step 7 (RootComponent.createChild wiring) and the closing hand-off note. The note describes the single→multi-screen conversion in prose.
- `requirements/14-cookbook/01-add-screen.md` — the canonical recipe for adding a sub-screen.
- `requirements/03-architecture-patterns/02-decompose-navigation.md` — `StackNavigation<<Feature>Router>` shape.

# Steps

## 1. Fix the frontmatter description

Find line 3 of `screen-builder.md`:

> `description: Adds a new sub-screen inside an existing :ui-screen-features:* feature module. Use when the task asks for "a new screen", "a sub-screen", "a tab inside <Feature>", or names a navigation target that does not yet exist under an existing feature router. Does NOT create new feature modules — for that, the orchestrator escalates to data-feature-builder + a new screen-feature scaffold (a wider task).`

Replace with:

```
description: Adds a new sub-screen inside an existing :ui-screen-features:* feature module. Use when the task asks for "a new screen", "a sub-screen", "a tab inside <Feature>", or names a navigation target that does not yet exist under an existing feature router. Does NOT create the feature module itself — `feature-module-scaffold-builder` handles that; `task-intake` chains the two when the target feature does not yet exist. When adding the first sub-screen to a freshly-scaffolded (single-screen, Debug-style) feature root, this builder also converts the root to multi-screen shape — see Step 4a in the body.
```

## 2. Insert a new Step 4a — single→multi-screen conversion

Find the existing Step 4 "Wire the route" in `screen-builder.md`. Right after Step 4 ends (just before Step 5 "Update the calling screen (if applicable)"), insert a new Step 4a:

```
### 4a. (If the feature root is single-screen) — convert to multi-screen first

Before applying Step 4's route wiring, check the shape of the feature root component. If `feature-module-scaffold-builder` produced it in **single-screen (Debug-style)** form, the root has none of: an internal `StackNavigation<<Feature>Router>`, a `childStack(...)` declaration, an inner `sealed class Child`. In that case, before writing the new sub-screen's route, perform the conversion below. If the root already owns a `StackNavigation` (i.e. the feature already hosts at least one sub-screen via the stack), skip 4a and proceed to Step 5.

Detect single-screen shape (run from repo root):

```bash
rg -l 'StackNavigation' ui-screen-features/<name>/src/commonMain/kotlin/ 2>/dev/null
```

If empty output, the root is single-screen. Convert as follows:

**4a.1. Replace `RootRouter.<Feature>` payload shape.** In `:ui-screen-features:screen-api/RootRouter.kt`, find the entry created by `feature-module-scaffold-builder`:

```kotlin
@Serializable public data object <Feature> : RootRouter()
```

Replace with:

```kotlin
@Serializable public data class <Feature>(val value: <Feature>Router = <Feature>Router.<FirstSubscreen>) : RootRouter()
```

(`<FirstSubscreen>` is the route subtype you create in Step 4 — pick the obvious default; `<Feature>Router.<FirstSubscreen>` must be a `data object` or have a sensible default-constructed `data class`.)

**4a.2. Update `RootComponent.createChild`.** In `:shared/RootComponent.kt`, find the existing branch:

```kotlin
is RootRouter.<Feature> -> Child.<Feature>(
    <Prefix>Component(
        componentContext = context,
        close = viewModel::onBack,
    )
)
```

Replace with:

```kotlin
is RootRouter.<Feature> -> Child.<Feature>(
    <Prefix>Component(
        componentContext = context,
        initial = router.value,
        close = viewModel::onBack,
    )
)
```

**4a.3. Convert the feature root's `<Prefix>Component` to multi-screen.** Currently it extends `BaseComponent<<Prefix>Direction>` with no internal stack. Rewrite per the standard multi-screen pattern (see `requirements/03-architecture-patterns/02-decompose-navigation.md` and any existing multi-screen `*RootComponent` for reference):

- Add `initial: <Feature>Router` to the constructor.
- Add `private val navigation = StackNavigation<<Feature>Router>()`.
- Add `val stack: Value<ChildStack<<Feature>Router, Child>> = childStack(source = navigation, serializer = <Feature>Router.serializer(), initialConfiguration = initial, key = "<Prefix>Component", childFactory = ::createChild)`.
- Add `private fun createChild(router: <Feature>Router, context: ComponentContext): Child = when (router) { ... }`.
- Introduce an inner `internal sealed class Child(...)` mirroring `RootComponent.Child` shape.

Mirror the closest existing multi-screen feature root in the repo for the exact import set and `childStack` signature.

**4a.4. Update the placeholder `<Prefix>Screen`.** Replace its empty body with a `ChildStack`-driven render (again mirror an existing multi-screen `*RootScreen`).

After 4a is complete, proceed with Step 4's route wiring — now the route slots into the new `StackNavigation<<Feature>Router>` you just introduced.

Verify after 4a, before continuing:

```bash
./gradlew :ui-screen-features:<name>:assemble
```

This should build green. If it fails, you have a partial conversion — fix before proceeding.
```

## 3. Update Step 5's preconditions

Step 5 ("Update the calling screen (if applicable)") starts with the calling-screen context. Leave its body unchanged, but add one sentence at the top of Step 5:

```
If Step 4a fired, Steps 5 and 6 apply to the post-conversion shape.
```

# Acceptance

- `screen-builder.md` frontmatter `description` no longer mentions `data-feature-builder + a new screen-feature scaffold`.
- `screen-builder.md` contains a new `### 4a` step covering single→multi-screen conversion, with explicit 4a.1, 4a.2, 4a.3, 4a.4 sub-steps.
- Step 4a includes a detection bash command (`rg -l 'StackNavigation' ...`) so the builder knows when to fire.
- Step 5 mentions Step 4a in one sentence.

Verify with:

```
rg 'feature-module-scaffold-builder' requirements/sub-agents/builders/screen-builder.md
```

This should now hit (at minimum in the description line).

# Report back

When done, post:

1. The new description line (verbatim).
2. The new Step 4a in full (verbatim).
3. The output of the `rg` command above.

# Constraints

- Do not touch `feature-module-scaffold-builder.md` (its hand-off note stays as the predecessor's signal — Step 4a in screen-builder is the consumer's redundancy, not a replacement).
- Do not modify any other step (1, 2, 3, 4, 5, 6) of `screen-builder.md` beyond the one-sentence Step 5 addition.
- Preserve the existing markdown style and the surrounding "What you MUST NOT do" section at the bottom.
- The Step 4a code blocks use `<Feature>` / `<Prefix>` / `<FirstSubscreen>` placeholders consistently with `05-template-conventions.md`. Do not substitute concrete reference-repo names.
````

---

## Prompt 5 — Refresh `invalidate-templatize.md` audit log

> Effort: ~10 min · Risk: very low · Depends on: none

**Что чинит:** В `invalidate-templatize.md` колонка «Leakage remaining» показывает устаревшие значения (116 у cookbook, 23 у sub-agents, 1 у нескольких строк). По реальному `rg` — 0 везде. Это вводит в заблуждение: создаёт впечатление, что работа не закончена.

````text
# Goal

The Audit log table at the bottom of `requirements/invalidate-templatize.md` has a "Leakage remaining" column whose values are stale. The templatize loop has converged (zero matches against its grep signature on every row's scope), but the log was not updated on the final passes. Recompute the actual values and rewrite the column.

# Context

Read first:
- `requirements/invalidate-templatize.md` — find the Audit log table (Mermaid-style markdown table near the bottom).
- `requirements/00-overview/05-template-conventions.md` — §8 lists the leakage signatures the loop greps.
- `requirements/00-overview/05-template-conventions.md` — §10 has the verification command: `rg -i 'grippo|training|workout|muscle|weightformatstate|profilebody|workouthistory' <file>`.

The current Audit log table rows (verify against the actual file):
- 00-overview, 01-tech-stack, 04-base-classes, 05-design-system, 07-mappers, 08-dependency-injection, 09-conventions, 10-toolkit, 11-state-and-formatters, 12-gradle-build, 13-anti-patterns — Leakage remaining: 0.
- 02-module-structure, 03-architecture-patterns, 06-data-layer, tasks — Leakage remaining: 1.
- 14-cookbook — Leakage remaining: 116.
- sub-agents — Leakage remaining: 23.

# Steps

## 1. Compute the actual leakage per row

Run for each chapter row (replace `<chapter>` with each row):

```bash
rg -ic 'grippo|training|workout|muscle|weightformatstate|profilebody|workouthistory' requirements/<chapter>/ 2>/dev/null | awk -F: '{s+=$2} END {print s+0}'
```

Special rows:
- `sub-agents`: `find requirements/sub-agents -name '*.md' -print0 | xargs -0 rg -ic 'grippo|training|...' 2>/dev/null | awk -F: '{s+=$2} END {print s+0}'`
- `tasks`: `find requirements/tasks -name '*.md' -print0 | xargs -0 rg -ic '...' 2>/dev/null | awk -F: '{s+=$2} END {print s+0}'`

Record the integer result for each of the 17 rows.

## 2. Update the table

For each row whose displayed "Leakage remaining" differs from the computed value, update the cell.

Use a single `Edit` per row (one `replace_all` only if the same row text appears verbatim more than once in the file — it shouldn't).

## 3. Update "Last audited" only if the row's value changed

Per the file's own convention, `Last audited` shifts only on a substantive audit. Since this is a log-correction pass (not a full row audit), leave `Last audited` and `Count` unchanged unless the recomputation surfaces actual leakage you patch.

If a recomputation reveals **nonzero** leakage on a row currently logged as 0, do NOT auto-patch the content — that's `invalidate-templatize.md`'s loop job. Just record it correctly and flag in the report.

# Acceptance

- Every row in the Audit log table has a `Leakage remaining` column value that matches the computed `rg` count.
- The table's other columns (Count, Last audited) are unchanged unless the recomputation surfaced a real discrepancy worth flagging.

Verify by re-running the per-row `rg` commands and diffing against the table.

# Report back

Post a table:

```
| Row | Displayed before | Computed actual | Patched in log? |
|---|---|---|---|
| 00-overview | 0 | 0 | no (no change needed) |
| 02-module-structure | 1 | <X> | yes if X != 1 |
| 14-cookbook | 116 | <X> | yes |
| sub-agents | 23 | <X> | yes |
| ... | ... | ... | ... |
```

Plus a one-liner: "Convergence status: <converged | N rows still nonzero>".

# Constraints

- Do not edit any file other than `requirements/invalidate-templatize.md`.
- Do not change the prompt body of `invalidate-templatize.md` — only the Audit log table cells.
- If a row's computed value is nonzero and was reported as zero, flag it in the report — do not auto-patch the chapter file. That's a separate templatize-loop iteration.
- Do not run the templatize prompt itself in this pass; you are only correcting the log.
````

---

## Prompt 6 — Verify `codex-plugin-cc` reality and tune defaults

> Effort: ~20 min · Risk: low · Depends on: none

**Что чинит:** `orchestrator.md`, `codex-review-loop.md`, `README.md` ссылаются на `https://github.com/openai/codex-plugin-cc`. В `SUBAGENTS_TODO_PROMPTS.md` Prompt 14 явно говорит «research-задача: жив ли codex-plugin-cc?». Если URL мёртв или плагин не существует — путь `codexEnabled: true` сразу даёт `HALT` без альтернативы. Безопасный дефолт `codexEnabled: auto` спасает (fallback на `internal-reviewer`), но факт надо подтвердить.

````text
# Goal

The sub-agents pipeline references `https://github.com/openai/codex-plugin-cc` as the external-review provider. Verify whether this plugin exists and is current, then adjust the references in `requirements/sub-agents/` and `requirements/00-overview/03-project-config.md` accordingly.

# Context

Files that reference the Codex plugin:
- `requirements/sub-agents/README.md` — "External review" section.
- `requirements/sub-agents/helpers/orchestrator.md` — Step 5 routing matrix, includes a detection bash block.
- `requirements/sub-agents/helpers/codex-review-loop.md` — entire file dedicated to the plugin.
- `requirements/sub-agents/helpers/internal-reviewer.md` — Preconditions reference `codexEnabled`.
- `requirements/00-overview/03-project-config.md` — `codexEnabled` field documentation.

# Steps

## 1. Verify plugin existence

Use the `WebFetch` tool (or `Bash` + `curl -sI`) on:

```
https://github.com/openai/codex-plugin-cc
```

Record:
- HTTP status (200, 404, redirect target if any).
- If 200: read the README. Is the plugin: (a) active and recently maintained, (b) archived/deprecated, (c) renamed/moved?
- If 404 or redirect: note the actual current location (search GitHub for "codex plugin claude code" if needed).
- The actual install command (npm package? CLI? Claude Code plugin install command?).
- The actual review command name (e.g. `/codex review`).

## 2. Decide the path

Pick one based on verification:

**Path A — plugin exists and is current.**
- Update each cited file's reference to match the verified URL and command name.
- Confirm the detection bash in `orchestrator.md` Step 5 reflects the real install path (e.g. `~/.claude/plugins/codex-plugin-cc` vs whatever the real location is).
- Confirm the `codex-review-loop.md` Step 1 references the real invocation command.
- Default in `03-project-config.md` stays `codexEnabled: auto`.

**Path B — plugin missing / archived / unverified.**
- Update each cited file with a "Status: unverified — verify before relying on" banner near the first reference.
- Change the default in `03-project-config.md` from `codexEnabled: auto` to `codexEnabled: false` so fresh projects route to `internal-reviewer` without trying to detect.
- Leave `codex-review-loop.md` in place (it's still the integration spec if/when the plugin is confirmed), but add a top-of-file banner: `Status: integration spec for openai/codex-plugin-cc. Plugin existence has not been verified — set codexEnabled to 'auto' or 'true' only after confirming the plugin is real and the install path matches Step 5 detection.`
- Update `orchestrator.md` Step 5's `HALT` message for `codexEnabled: true + Codex missing` to point at `03-project-config.md` field documentation + a `Status` clarification.

Pick Path B if verification is inconclusive — silent assumptions are worse than explicit "unverified".

## 3. Apply edits

Use `Edit` (one per file) for each substitution. Do NOT rewrite whole sections.

If Path B:
- Add the banner at the top of `codex-review-loop.md` (immediately after the frontmatter).
- Change `codexEnabled: auto` → `codexEnabled: false` in `03-project-config.md`.
- Update `03-project-config.md`'s "Field meanings" bullet for `codexEnabled` to clarify the Status.
- Update `orchestrator.md` Step 5 routing table's `codexEnabled: true + Codex missing` HALT message wording.

If Path A:
- Update URLs / commands per the verification.

# Acceptance

- Every reference to the Codex plugin in `requirements/sub-agents/` and `requirements/00-overview/03-project-config.md` is factually current per Step 1 verification.
- The default `codexEnabled` value in `03-project-config.md` matches the verified state (auto if confirmed, false if unverified).
- A reader of `codex-review-loop.md` knows whether the plugin is real (no implicit assumption).

# Report back

Post:

1. Verification result: URL status + plugin status + actual install/invocation commands (or "could not verify").
2. Chosen path (A or B) and rationale (one sentence).
3. Files edited (paths + one-line summary each).
4. The new `codexEnabled` default in `03-project-config.md`.

# Constraints

- Do not invent a plugin URL or commands. If you cannot verify, choose Path B.
- Do not delete `codex-review-loop.md` even on Path B — keep it as an integration spec with a Status banner.
- Do not modify `internal-reviewer.md` beyond updating any cross-references that became wrong (it should already work standalone).
- Do not touch the substance of how the orchestrator routes — only the verbatim references that became stale.
````

---

## Prompt 7 — Document tooling files vs portable `requirements/`

> Effort: ~15 min · Risk: very low · Depends on: none

**Что чинит:** В руте репо лежат `SETUP_TEMPLATIZE.md`, `SUBAGENTS_TODO_PROMPTS.md`, `SUBAGENTS_HOURLY_AUDIT.md` — это рабочие инструменты для **этого** репо (templatize/audit). `SETUP_TEMPLATIZE.md` даже содержит хардкоженный путь `/Users/maxvoitenko/Projects/Pet/grippo-mobile`. Если пользователь скопирует `requirements/` в новый проект и заодно прихватит эти файлы — получит мусор. Сейчас нет явной разметки, что́ копировать, а что́ оставить.

````text
# Goal

Three root-level markdown files at the repo root are maintenance tooling for this specific repo, not portable requirements that ship to a new project:

- `SETUP_TEMPLATIZE.md` — one-shot setup that was already run (contains a hardcoded path).
- `SUBAGENTS_TODO_PROMPTS.md` — historical TODO of prompts that were applied to evolve the sub-agents pipeline.
- `SUBAGENTS_HOURLY_AUDIT.md` — self-healing audit pipeline for sub-agents drift.

Add a clear signpost so a new-project bootstrapper knows: copy `requirements/`, do NOT copy these three files unless intentionally importing the audit tooling.

# Context

Read first:
- `requirements/README.md` — already explains the bootstrap workflow.
- Each of the three target files at repo root (`SETUP_TEMPLATIZE.md`, `SUBAGENTS_TODO_PROMPTS.md`, `SUBAGENTS_HOURLY_AUDIT.md`).
- `requirements/launch.md` Step 14 — references sub-agent installation.

# Steps

## 1. Add a "What ships vs what stays" section to `requirements/README.md`

In `requirements/README.md`, find the existing section "## Sub-agents — install before first use" (near the bottom). Immediately BEFORE it, insert a new section:

```markdown
## What ships to a new project (and what does not)

`requirements/` is the portable package — copy this entire folder to a new KMP project's root and follow `launch.md`. Everything inside `requirements/` is project-agnostic (after applying `launch.md` Step 1.5 substitutions).

The following files at the **reference repo's root** are local maintenance tooling for this repo, NOT part of the requirements package. Do **not** copy them to a new project unless you specifically want to fork the audit/templatize tooling:

- `SETUP_TEMPLATIZE.md` — one-shot setup that initialised the templatize conventions in this repo; already applied, never re-run.
- `SUBAGENTS_TODO_PROMPTS.md` — historical log of prompts applied to evolve `requirements/sub-agents/`.
- `SUBAGENTS_HOURLY_AUDIT.md` — self-healing audit pipeline for this repo's sub-agents drift.
- `LAUNCH_READINESS_FIXES.md` — pre-bootstrap fix prompts for this repo before forking.

The companion `requirements/invalidate.md`, `requirements/invalidate-sub.md`, and `requirements/invalidate-templatize.md` ARE portable — they're audit prompts the new project can use to keep its own `requirements/` aligned with its evolving code.
```

## 2. Add a banner to each of the three root files

For each of `SETUP_TEMPLATIZE.md`, `SUBAGENTS_TODO_PROMPTS.md`, `SUBAGENTS_HOURLY_AUDIT.md`, insert immediately AFTER the existing first-line `#` heading:

```markdown
> **Maintenance tooling for the grippo-mobile reference repo.** Do not copy this file to a new project bootstrapped from `requirements/`. Portable audit prompts live at `requirements/invalidate*.md`.
```

If the file already has a banner (e.g. a TL;DR block), insert this banner before the existing one.

## 3. Add the same banner to `LAUNCH_READINESS_FIXES.md` if it exists at root

If `LAUNCH_READINESS_FIXES.md` exists at the repo root, add the same banner after its first-line `#` heading.

(This file is the pre-bootstrap fix list; it stays in this repo, doesn't ship.)

# Acceptance

- `requirements/README.md` contains the new "What ships to a new project (and what does not)" section, immediately before the existing "Sub-agents — install before first use" section.
- Each of the four root-level tooling files (`SETUP_TEMPLATIZE.md`, `SUBAGENTS_TODO_PROMPTS.md`, `SUBAGENTS_HOURLY_AUDIT.md`, `LAUNCH_READINESS_FIXES.md` if it exists) has a top-of-file `>` blockquote banner stating it is maintenance tooling for this repo.

Verify:

```
for f in SETUP_TEMPLATIZE.md SUBAGENTS_TODO_PROMPTS.md SUBAGENTS_HOURLY_AUDIT.md LAUNCH_READINESS_FIXES.md; do
  [ -f "$f" ] && head -5 "$f" | grep -q 'Maintenance tooling' && echo "OK: $f" || echo "MISSING banner: $f"
done
grep -q '## What ships to a new project' requirements/README.md && echo "OK: README" || echo "MISSING section: README"
```

# Report back

Post:

1. Files edited (paths).
2. The verification script's output (per the bash block above).

# Constraints

- Do not delete or rename any file.
- Do not modify the body content of `SETUP_TEMPLATIZE.md`, `SUBAGENTS_TODO_PROMPTS.md`, `SUBAGENTS_HOURLY_AUDIT.md`, `LAUNCH_READINESS_FIXES.md` beyond inserting the top banner.
- Do not modify `requirements/launch.md`.
- Preserve the markdown style of `requirements/README.md`.
````

---

## Prompt 8 — Seed `tasks/` with a working example

> Effort: ~10 min · Risk: very low · Depends on: none

**Что чинит:** Папка `requirements/tasks/` пуста (только `done/.gitkeep`). У пользователя нет живого образца, чтобы скопировать и быстро запустить orchestrator. README.md помогает, но 200 строк read-mode перед первой задачей — это трение.

````text
# Goal

Create one example TASK file at `requirements/tasks/TASK_0_example_note_archive.md.example` to serve as a working reference for a new-project bootstrapper. The `.example` suffix prevents `task-intake` from picking it up at runtime (it scans `TASK_*.md`, not `*.md.example`).

# Context

Read first:
- `requirements/tasks/README.md` — task contract shape (the four required sections + optional Depends on).
- `requirements/00-overview/05-template-conventions.md` §3 — the canonical worked example is "Note archive".
- `requirements/sub-agents/helpers/task-intake.md` — Step 1, the BLOCKED criteria.
- `requirements/14-cookbook/01-add-screen.md` — the canonical "Note archive" walkthrough.

# Steps

## 1. Write the example file

Create `requirements/tasks/TASK_0_example_note_archive.md.example` with this exact content:

```markdown
# TASK 0 — Add "Note archive" screen (EXAMPLE — not picked up by task-intake)

> This is a reference example demonstrating the task-file shape. The `.example`
> suffix means `task-intake` will not pick it up. To run a real task: copy this
> file to `TASK_<N>_<snake_title>.md`, edit per project needs, then ask the
> parent Claude session to "run task TASK_N_<title>.md".

## Goal

Add a sub-screen to `:ui-screen-features:profile` showing an archive of the
user's notes for a configurable date range. The user opens it from the existing
"Note archive" card on `ProfileOverviewScreen` and can scroll the list, change
the range, and tap a row to open the note's detail screen (out of scope here).

## Inputs

- Source data: `NoteFeature.observeNotes(start, end)` and
  `NoteFeature.getNotes(start, end)` (assume the feature already exists in
  `:data-features:feature-api`).
- Entry point: a card on `ProfileOverviewScreen` already triggers
  `onNoteArchiveClick` — wire it to navigate into the new sub-screen.
- Default range: `DateRangePresets.last30Days()` — passed as the route payload.

## Acceptance

- New route: `ProfileRouter.NoteArchive(initialRange: DateRange)`.
- Seven MVI files under
  `ui-screen-features/profile/src/commonMain/kotlin/com/<org>/<product>/profile/notearchive/`
  following `requirements/14-cookbook/01-add-screen.md`:
  `ProfileNoteArchive{State, Direction, Loader, Contract, ViewModel, Component, Screen}.kt`.
- `ProfileOverviewScreen`'s existing `onNoteArchiveClick` callback now pushes
  `ProfileRouter.NoteArchive(DateRangePresets.last30Days())` onto the profile
  stack navigator.
- Both build gates green:
  - `./gradlew :shared:assembleSharedDebugXCFramework`
  - `./gradlew :androidApp:assembleDebug`

## Out of scope

- Note detail screen, filtering, search, pagination, sharing — each is its own
  follow-up task.
- Changes to `NoteFeature` itself or its repository implementation.
- New strings in any locale other than what the existing infrastructure already
  ships.

## Depends on

(none — this example assumes `NoteFeature` and `ProfileOverviewScreen` are
already in place. A real first-task on a fresh project would likely depend on
TASK_<earlier>_add_note_data_feature being in `tasks/done/`.)
```

## 2. Add a one-liner pointer in `requirements/tasks/README.md`

In `requirements/tasks/README.md`, find the "## How execution works" section. Immediately AFTER the closing of section "## File shape" (and BEFORE "## How execution works"), insert:

```markdown
## Example

A complete reference task ships at `requirements/tasks/TASK_0_example_note_archive.md.example`. Copy it (drop the `.example` suffix), rename per the next available `<N>`, and edit to fit your task.
```

# Acceptance

- `requirements/tasks/TASK_0_example_note_archive.md.example` exists with the four required sections (`## Goal`, `## Inputs`, `## Acceptance`, `## Out of scope`) plus the optional `## Depends on`.
- The file uses canonical template names (`Note`, `<org>`, `<product>`) per `05-template-conventions.md`.
- `requirements/tasks/README.md` has a new "## Example" section pointing at the file.
- The `.example` extension ensures `task-intake`'s glob (`TASK_*.md`) does not pick it up at runtime.

Verify:

```
ls requirements/tasks/TASK_0_example_note_archive.md.example
grep -c '^## Example' requirements/tasks/README.md
```

The first should print the path; the second should print 1.

# Report back

Post:

1. Path to the example file (verbatim).
2. Output of the `ls` and `grep` commands.

# Constraints

- Do not create `TASK_0_*.md` (without `.example`) — `task-intake` would treat it as a real task.
- Do not modify `task-intake.md` or `orchestrator.md`.
- Do not introduce a real `:data-features:notes` module or any source code — this is documentation only.
- Use placeholder package roots (`com.<org>.<product>`) — do not bake in Grippo-specific package names.
````

---

## Prompt 9 — Document `CLAUDE.md` handoff in `launch.md` Step 13

> Effort: ~5 min · Risk: very low · Depends on: none

**Что чинит:** `launch.md` Step 13 говорит «write a new CLAUDE.md» в новом проекте, но не упоминает, что если пользователь скопировал `requirements/` из этого репо, то рядом мог приехать и **этот** `CLAUDE.md` (с Grippo-конкретными правилами на 600+ строк). Без явной инструкции «delete the old one» начинающий получит две противоречивых CLAUDE.md.

````text
# Goal

In `requirements/launch.md` Step 13 ("write CLAUDE.md"), add explicit guidance that any existing CLAUDE.md from the reference repo (grippo-mobile) must be deleted from the new project before writing the new one. Two CLAUDE.md files cannot coexist at the same scope (Claude Code loads them all).

# Context

Read first:
- `requirements/launch.md` Step 13.
- The current `CLAUDE.md` at the reference repo root — note it's grippo-specific (production rules, 600+ lines, references actual data-features modules, etc.).

# Steps

## 1. Insert a precondition at the top of Step 13

In `launch.md` Step 13, immediately after the heading `## Step 13 — write \`CLAUDE.md\``, insert this paragraph:

```
**Precondition.** If you copied `requirements/` from a reference KMP repo (e.g. grippo-mobile), check the new project's root for an existing `CLAUDE.md`. If one is present, delete it — the file you write in this step REPLACES it. Two CLAUDE.md files at the same scope create contradictory rules; Claude Code loads both and applies them in arbitrary order.

```bash
[ -f CLAUDE.md ] && rm CLAUDE.md
```

If the existing CLAUDE.md was authored for your new project (i.e. you didn't import it from a reference repo), STOP and ask the user before deleting — it may contain real project context.
```

## 2. Leave the rest of Step 13 unchanged

The existing Step 13 body (bullet list of what the new CLAUDE.md should describe) is correct — only the precondition is missing.

# Acceptance

- `launch.md` Step 13 starts with a **Precondition** paragraph covering the reference-repo CLAUDE.md handoff.
- The precondition includes the explicit `rm` bash + a safety check for user-authored content.
- The rest of Step 13 (the list of what the new CLAUDE.md should describe) is unchanged.

# Report back

Post:

1. The new precondition paragraph (verbatim).
2. Confirmation: `rg -A 3 '## Step 13 — write' requirements/launch.md` shows the precondition immediately after the heading.

# Constraints

- Do not modify any other step of `launch.md`.
- Do not modify the reference repo's `CLAUDE.md` itself (that's grippo-mobile's own document).
- Preserve the existing markdown style.
````

---

## После применения всех промптов

Запусти финальную проверку готовности:

```bash
# 1. lint sub-agents
bash requirements/sub-agents/lint.sh

# 2. project-config templatized?
rg -q 'productName: <Product>' requirements/00-overview/03-project-config.md && echo OK || echo FAIL

# 3. launch.md ↔ project-config bridge present?
rg -q 'Step 1.5' requirements/launch.md && echo OK || echo FAIL

# 4. orchestrator Step 0 bash valid?
sed -n '/^```bash$/,/^```$/p' requirements/sub-agents/helpers/orchestrator.md \
  | grep -v '^```' | bash -n - && echo OK || echo FAIL

# 5. screen-builder description updated?
rg -q 'feature-module-scaffold-builder' requirements/sub-agents/builders/screen-builder.md \
  && echo OK || echo FAIL

# 6. templatize log refreshed?
# (manual — compare audit table against rg counts)

# 7. example TASK file in place?
ls requirements/tasks/TASK_0_example_note_archive.md.example >/dev/null 2>&1 \
  && echo OK || echo FAIL
```

Если всё OK — система готова к bootstrap нового проекта через `requirements/launch.md`. Поток на новом проекте:

1. Создать новую папку, скопировать `requirements/`.
2. Открыть Claude Code в новой папке, скормить промт из `requirements/launch.md` "## The prompt".
3. Ответить на Step 0 вопросы. Step 1.5 заполнит `03-project-config.md`.
4. Дождаться окончания Step 12 (build green).
5. Step 14 установит sub-agents в `.claude/agents/`.
6. Создать первый TASK файл по образцу `TASK_0_example_note_archive.md.example`.
7. Запросить orchestrator: «Run task TASK_1_<title>.md».
