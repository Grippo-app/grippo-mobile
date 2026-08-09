# Trigger collision — static analysis

Static collision report for the 11 authored skill packages. Derived from each
skill's `SKILL.md` frontmatter `description` (the
trigger surface) and the per-skill `fixtures/expected-routing.json`
`primaryTriggers`. This is the **static** pre-analysis: it asserts that the
authored primary triggers are mutually distinct and disambiguated. Runtime
routing remains a live-system concern outside this static register.

Scope note: `implement-figma` (canonical source
`orchestrator/figma/skill/SKILL.md`, installed to `.claude/skills/implement-figma/`)
is referenced below as a routing neighbour for design-system but is **not** one
of the 11 authored skeletons; it carries no fixtures here.

## Primary-trigger table (11 skills)

| Skill | Primary triggers (distinct) |
| --- | --- |
| task-prep | prep TASK_<N> · promote a backlog item to todo · set acceptance anchors · classify builders and builder order · re-evaluate the answered questions sidecar · split a task before it runs |
| task-orchestrator | run task TASK_<N> · orchestrate the build loop · drive builders to done · route validators back to builders · reopen TASK_<N> · drop a task |
| ui-feature | add a screen / sub-screen / tab · seven-file MVI contract (State/Direction/Loader/Contract/ViewModel/Component/Screen) · wire Decompose navigation · bottom-sheet dialog / picker / modal · scaffold a feature module · :ui-core:state UI models + stub*() previews |
| design-system | :design-system:components shared widget (Button/Input/Chip/Toolbar/Card) · AppColor/AppDp/AppTypography token slot via AppTokens · AppTheme + composition locals · resource: string per locale / drawable / icon / font · @AppPreview / PreviewContainer · frozen binding spec for a widget |
| data-layer | add a :data-features:<name> module + <X>Feature interface · repository + feature-api split · Room entity / DAO / migration · DataStore key-value preference · domain model / enum for the UI · extend an endpoint + DTO on <Product>Api |
| mappers | add a mapper in a :data-mappers:* directional module · DTO→Entity / Entity→Domain / DTO→Domain bridge · Domain→State / State→Domain mapping · to<Target>() / to<Target>OrNull() naming · null-and-drop logging rule · Domain→Body / Domain→Entity (drafts) bridge |
| di-modules | add a Koin @Module @ComponentScan · @Single/@Factory/viewModel binding (binds = [Interface::class]) · register a module in :shared/Koin.kt · compose modules via @Module(includes=[...]) · inline @Single internal fun provider · Koin Annotations + KSP wiring |
| platform-build-toolkit | add or extend a :toolkit:<name> utility · version catalog entry / build-logic convention plugin · build.gradle.kts / settings.gradle.kts authoring · add a :compose-libs:<name> product-agnostic widget · thin :androidApp/:iosApp shell wiring · iOS SwiftPackage framework / :iosApp Xcode project |
| validation-gates | validate the produced code before merge · run the gates (machine-validator + agent-reviewer) · did this break an anti-pattern? · trace acceptance criteria · is scope leaking? · reviewer-only gaps no validator catches |
| backend-contract-client | read the backend contract before building a DTO/endpoint · OpenAPI/Swagger snapshot (contract:refresh-openapi) · Postman enrichment (contract:refresh-postman) · endpoint inventory / per-area field slice · contract drift audit (contract:diff) · endpoint coverage planning (contract:suggest) |
| launch-readiness | bootstrap a new project from an empty directory · run the launch.md pipeline · populate project-config.md from Step-0 answers · scaffold the foundation modules + first feature · verify end-to-end build readiness on both platforms · install skills + seed .arch-map.json |

## Distinctness assertion

No primary trigger phrase is claimed by two of the 11 skills. Each row above is
disjoint at the phrase level — the literal anchor strings (e.g. `prep TASK_<N>`
vs `run task TASK_<N>`, `:data-features` vs `:data-mappers` vs `:design-system`,
`contract:refresh-openapi` vs `launch.md`) do not repeat across rows. The static check
passes: **0 duplicate primary triggers across the 11 skills.**

## Near-neighbour RISK pairs (intra-project) and their disambiguation

These pairs share a *domain*, not a *trigger phrase*. The triggers were chosen so
the literal anchors diverge; the `allowedSecondary` arrays in each
`expected-routing.json` encode the legitimate spillover.

- **task-prep ↔ task-orchestrator** — both speak of "TASK_<N>" and "builders".
  Disambiguator: lifecycle stage. task-prep owns `backlog/` → `pending/` →
  `todo/` (`prep`, `promote`, `acceptance anchors`, `classify builders`);
  task-orchestrator owns `todo/` → `done/` (`run task`, `orchestrate the loop`,
  `drive builders to done`, `route validators`, `reopen`, `drop`). The verb
  `prep`/`promote` vs `run`/`orchestrate` is the split.

- **ui-feature ↔ design-system** — both touch Compose. Disambiguator: a
  *product* screen/feature/MVI/Decompose component (ui-feature) vs a *reusable*
  `:design-system:components` widget, token slot, theme, or resource
  (design-system). "screen / ViewModel / navigation / dialog" → ui-feature;
  "Button/Chip widget / AppColor token / AppTheme / string-drawable-icon-font" →
  design-system.

- **data-layer ↔ mappers** — both live under `:data-*`. Disambiguator: module
  prefix. `:data-features:` + repository + Room + DTO + `<Product>Api` →
  data-layer; `:data-mappers:` + `to<Target>()` direction + null-and-drop →
  mappers. A mapper is one direction × one area; a data feature owns the
  interface + persistence.

- **data-layer ↔ backend-contract-client** — both mention "endpoint" and "DTO".
  Disambiguator: build vs consult. Writing/extending the Kotlin DTO / endpoint /
  data feature → data-layer; reading the committed OpenAPI/Postman snapshot,
  inventory, or running a drift/coverage check → backend-contract-client. The
  `contract:*` session verbs and "snapshot / drift / Swagger" are the
  contract-client tell.

- **di-modules ↔ everything that needs wiring** — a new feature/repo/ViewModel
  often *also* needs a Koin binding. Disambiguator: di-modules fires only on the
  *binding itself* (`@Module`, `@Single`, `binds = [...]`, `:shared/Koin.kt`).
  The builder skill (data-layer / ui-feature) owns the impl; di-modules owns the
  registration — hence di-modules appears as `allowedSecondary`, not primary, on
  those prompts.

- **platform-build-toolkit ↔ design-system** — `:compose-libs:*`
  (product-agnostic widget, toolkit) vs `:design-system:components`
  (product-styled widget). Disambiguator: module path + "version catalog /
  convention plugin / build.gradle" anchors route to the toolkit.

- **launch-readiness ↔ platform-build-toolkit** — both scaffold modules / touch
  the iOS project. Disambiguator: launch-readiness is the *from-scratch*
  `launch.md` one-shot on an empty directory (manual Steps 3/7/11);
  platform-build-toolkit is the *post-bootstrap* per-module builder. "bootstrap /
  empty directory / launch.md / Step-0" → launch-readiness.

No RISK pair shares a primary trigger; each is separated by a literal anchor.
The only residual risk is *prompt ambiguity by the user* (e.g. "add a feature"
with no layer named), which the trigger-fixtures gate exercises through the committed prompt corpus.
