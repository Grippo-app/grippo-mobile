# grippo-mobile

Grippo is a Kotlin Multiplatform fitness client (Android + iOS from one codebase):
Compose Multiplatform UI, Decompose navigation, Koin DI, Ktor networking, Room
persistence. It is the primary consumer of the Grippo backend at `grippo-app.com`.

This file is the map. It says what the project is, which rules exist, who owns
each rule and how AI work on the repository is organized. The rules themselves
live in the owning skills under `.claude/skills/`; the code owns signatures.

## How to read these rules

- `MUST` / `MUST NOT` are mandatory; `MAY` is a permitted choice.
- Every topic has exactly one owning document. This file and `AGENTS.md` point
  to owners; they never restate a rule in a way that could diverge from it.
  When a summary and its owner disagree, the owner wins and the summary gets fixed.
- Current source establishes actual signatures, versions and callers. An
  existing deviation in the code is not permission to copy it.
- The vocabulary below is used with these exact meanings everywhere in the rulebook.

### Definitions

- **Authorized scope** - the user's request plus every step necessary to deliver
  it: callers, routes, DI registration, resources, coherent migrations. Doing
  those steps needs no repeated approval. **Scope expansion** is new product
  behavior, a change to a locked decision, a new module/dependency the request
  does not imply, or a change to an existing public contract beyond what the
  request implies (migrating the callers of a contract the task itself changes is
  authorized); it requires a question (see [When to stop and ask](#when-to-stop-and-ask)).
- **Owning document / canonical rule** - the one skill or reference that defines
  a topic (listed in the [skills index](#skills-index)). "Apply the owning
  skill's gate" means: open that skill's `SKILL.md`, find its completion gate,
  check every item.
- **Behavior change** - a change to what a screen or dialog shows as data, what a
  control does, navigation or entry points, an operation, validation, persistence
  or an outcome. It triggers the feature-analytics and state-preservation gates.
- **Visual change** - a change to what is rendered or whether UI content is shown:
  layout, tokens, components, artwork, or copy whose length or structure changes
  the layout. A callback-only or Kotlin-visibility change is not visual. It
  triggers brand-design and design-system gates.
- **Visual-only change** - a visual change with identical State, Contract,
  Direction, data and navigation. It needs an impact assessment instead of new
  events or drafts: one sentence per gate naming what was checked (analytics-facing
  symbols for feature-analytics; owners and fields for state-preservation), which
  the `gate-auditor` verifies against the diff.
- **Independent review** - review by a subagent that edited none of the files in
  the task (`code-reviewer`, `design-reviewer`, `gate-auditor`). It is
  pre-authorized by this file; the author's self-review never counts as it.
- **Permitted check** - source/diff review; the rulebook checker
  (`python3 -I .claude/hooks/check-rulebook.py`) and its existing Bash guard
  matrix (`zsh .claude/hooks/bash-guard-tests.sh`); scoped Gradle builds; manual
  runtime checks on an emulator/simulator through the `runtime-verification`
  skill when the user allows running the app. Application tests, linters and
  screenshot tooling may be added only on explicit user request.
- **Record** - a durable file the rulebook names in [Records](#records). The
  reply to the user is the task report; nothing else is a record.
- **Small editorial change** - removing a comment or editing one that
  [code style](.claude/skills/validation-gates/references/code-style.md#comments)
  allows, a wording fix of one string key in all locales, a rename inside one
  file, or documentation that changes no rule, gate, agent right or hook; no
  behavior, contract or layout impact. Self-review only.
- **Significant state** (previews) - every distinct layout branch of a surface
  (for example empty, loading, error, selected) plus one representative populated
  state. Not every combination of inputs.

## Stack

Exact versions and platform pins live in `gradle/libs.versions.toml`,
`gradle.properties` and `build-logic/convention/`. Platform setup and secure
inputs are owned by [platform-build-toolkit](.claude/skills/platform-build-toolkit/SKILL.md).

## Module map

`settings.gradle.kts` is the source of truth for module membership. Groups:

| Group | Modules | Role | Owner skill |
|---|---|---|---|
| App shells | `:androidApp`, `iosApp/` (Xcode, not Gradle) | Thin hosts: Koin start via `Koin.init`, root hosting, platform adapters, deeplink/notification entry | [platform-build-toolkit](.claude/skills/platform-build-toolkit/SKILL.md) |
| Composition root | `:shared` | `Koin.kt` (explicit module list, returns `ApplicationBootstrap`), `RootComponent`/`RootViewModel`/`RootScreen`, `DialogComponent` host, `state/ApplicationState` archive codec | [ui-feature](.claude/skills/ui-feature/SKILL.md), [di-modules](.claude/skills/di-modules/SKILL.md) |
| Screen features | `:ui-screen-features:{screen-api, authorization, main, home, trainings, training, exercises, profile, debug}` | Full-screen flows; `screen-api` holds the `@Serializable` routers and `Deeplink` | [ui-feature](.claude/skills/ui-feature/SKILL.md) |
| Dialog features | `:ui-dialog-features:dialog-api` + bottom-sheet modules | `DialogConfig`/`DialogController` contracts and sheet flows hosted by `DialogComponent` | [ui-feature](.claude/skills/ui-feature/SKILL.md) |
| UI core | `:ui-core:{foundation, state, error:error-provider, error:error-provider-impl}` | `BaseViewModel`/`BaseComponent`/`BaseComposeScreen`, result bus, `PersistenceKey`; reusable `*State`, `UiText`, `*FormatState`, `stub*()`; `AppError` and its dialog mapping | [ui-feature](.claude/skills/ui-feature/SKILL.md) |
| Design system | `:design-system:{core, components, resources:provider, resources:provider-impl, preview}` | `AppTokens`, shared widgets, Compose resources, `StringProvider`, `@AppPreview` | [design-system](.claude/skills/design-system/SKILL.md), [brand-design](.claude/skills/brand-design/SKILL.md), [illustrations](.claude/skills/illustrations/SKILL.md) |
| Domain | `:data-features:feature-api` + implementations (`authorization, user, trainings, exercise-examples, exercise-metrics, muscle, equipment, excluded-muscles, excluded-equipments, weight-history, goal, local-settings`) | Public `*Feature` interfaces, `*UseCase`s and domain models; internal `RepositoryImpl`/`FeatureImpl` | [data-layer](.claude/skills/data-layer/SKILL.md) |
| Services | `:data-services:{backend, database, datastore, google-auth, apple-auth, firebase}` | `GrippoApi`/`BackendClient`/`TokenProvider` and DTOs; Room `Database` with migrations; DataStore wrapper; platform credential providers; Android Firebase adapters | [data-layer](.claude/skills/data-layer/SKILL.md), [backend-contract-client](.claude/skills/backend-contract-client/SKILL.md) |
| Mappers | `:data-mappers:{dto-to-entity, entity-to-domain, dto-to-domain, domain-to-state, state-to-domain, domain-to-dto, domain-to-entity}` | One isolated module per conversion direction | [mappers](.claude/skills/mappers/SKILL.md) |
| Analytics | `:analytics:analytics-api` + feature implementations | Typed `<Feature>Analytics` contracts (`trackEvent`, `trackOperation`, `presentScreen` as implemented); internal encoders, schemas and one annotated module each | [feature-analytics](.claude/skills/feature-analytics/SKILL.md) |
| Toolkit | `:toolkit:{analytics, crash-reporting, messaging}:{api, core}`, `context, http-client, serialization, logger, date-utils, number-utils, theme, localization, image-loader, link-opener, notification-manager, permission-manager, share, connectivity` | Shared utilities and typed service cores | [platform-build-toolkit](.claude/skills/platform-build-toolkit/SKILL.md) |
| Compose libs | `:compose-libs:{chart, konfetti, segment-control, wheel-picker}` | Product-independent widgets styled through parameters | [platform-build-toolkit](.claude/skills/platform-build-toolkit/SKILL.md) |
| Build logic | `build-logic/convention/` | `android.library/application`, `kotlin.multiplatform`, `compose.multiplatform`, `koin.annotation`, `room`, `ios.swiftpackage` conventions | [platform-build-toolkit](.claude/skills/platform-build-toolkit/SKILL.md) |

## Module dependency rules

```
:androidApp / iosApp
        |
     :shared  (composition root: the only module that imports everything)
        |
  :ui-screen-features:*  ->  :ui-dialog-features:*
        |                         |
  :ui-core:foundation, :ui-core:state, :ui-core:error:*
        |
  :design-system:components -> :design-system:core -> :design-system:resources:provider
        |
  :data-features:feature-api        (UI sees ONLY this from the data layer)
        |
  :data-features:<feature>          (internal Repository/FeatureImpl)
        |
  :data-services:{backend, database, datastore, *-auth, firebase}
        |
  :toolkit:*
```

Hard rules:

- UI modules never depend on `:data-services:*`; they consume
  `:data-features:feature-api`. Sole exception: `:ui-screen-features:authorization`
  uses `:data-services:google-auth` and `:data-services:apple-auth` for the
  credential flow before a domain user exists.
- `:data-features:feature-api` never depends on `:data-services:*`.
- Mapper modules never depend on each other. `domain-to-state` and
  `state-to-domain` may depend on `:ui-core:state`; no mapper depends on feature
  UI or Compose components.
- `:analytics:analytics-api` is pure contracts; `:analytics:<feature>` implementations
  import no UI, domain, service, SDK or toolkit core code. UI depends on the API;
  `:shared` wires implementations.
- `:toolkit:*` never depends on the data layer or Firebase SDKs. Documented upward
  exceptions, not to be extended by analogy: `:toolkit:http-client` ->
  `:ui-core:error:error-provider` (for `AppError`) and `:toolkit:date-utils` ->
  `:design-system:core` + `:design-system:resources:provider` (locale-aware formats).
- `:design-system:*` never depends on the data layer or analytics.
- `:data-services:firebase` is consumed only by `:androidApp`.
- Cross-feature navigation goes through `:ui-screen-features:screen-api` routes and
  constructor-threaded callbacks; feature modules never import each other.

## Architecture in one page

Read the owner for the part of the task being changed:

| Topic | Owner |
|---|---|
| MVI and base classes | [ui-feature](.claude/skills/ui-feature/SKILL.md) |
| Navigation, dialogs, results | [navigation](.claude/skills/ui-feature/references/navigation.md), [dialogs](.claude/skills/ui-feature/references/dialogs.md), [results](.claude/skills/ui-feature/references/results.md) |
| UI failures | [error pipeline](.claude/skills/ui-feature/references/error-pipeline.md) |
| Drafts and restoration | [state-preservation](.claude/skills/state-preservation/SKILL.md) |
| Domain, cache and storage | [data-layer](.claude/skills/data-layer/SKILL.md) |
| Wire contracts | [backend-contract-client](.claude/skills/backend-contract-client/SKILL.md) |
| Conversions and DI | [mappers](.claude/skills/mappers/SKILL.md), [di-modules](.claude/skills/di-modules/SKILL.md) |
| Visual intent, tokens and artwork | [brand-design](.claude/skills/brand-design/SKILL.md), [design-system](.claude/skills/design-system/SKILL.md), [illustrations](.claude/skills/illustrations/SKILL.md) |
| Analytics | [feature-analytics](.claude/skills/feature-analytics/SKILL.md) |
| Platform and toolkit | [platform-build-toolkit](.claude/skills/platform-build-toolkit/SKILL.md) |

## Code style and naming

Use [code style](.claude/skills/validation-gates/references/code-style.md),
[Compose structure/runtime](.claude/skills/compose-components/SKILL.md),
[feature composition](.claude/skills/ui-feature/references/compose-rules.md) and
[forbidden patterns](.claude/skills/validation-gates/references/forbidden-patterns.md).

## Locked architectural decisions

- Compose Multiplatform for UI on both platforms.
- Decompose for navigation and lifecycle (not Compose Navigation, not Voyager).
- Koin annotations + KSP for DI (not Hilt, Anvil, Metro or hand-written DSL).
- Room multiplatform for persistence (not SQLDelight); `fallbackToDestructiveMigration(dropAllTables = true)` on both platforms is intentional.
- Ktor for networking; `kotlinx-serialization` for JSON and Decompose state.
- The seven-file Component/Contract/State/Direction/Loader/ViewModel/Screen pattern for every screen and dialog.
- `BaseViewModel` navigation uses a conflated channel - a known one-shot trade-off, not a bug to fix.
- Convention plugins in `build-logic/` are the only way to configure modules; versions live in the catalog.
- Seven mapper directions in seven modules; no inline DTO/Domain/State conversion.
- DTO fields nullable `= null`; required inbound values guarded in mappers; required outbound values supplied by business code.
- Backend owns the API contract; no codegen; DTOs are hand-written.
- Compose Multiplatform Resources for strings and drawables (not `androidx.compose.ui.res`, not Moko).
- Dialog sessions are in-memory and never restored after process death.
- No automated application tests, linters or screenshot tooling unless the user explicitly asks.

Changing any of these is scope expansion and needs a question.

## Scope discipline

- Preserve and attribute working-tree changes under
  [change attribution](.claude/skills/task-orchestrator/references/change-attribution.md).
- Apply the Definitions above and [scope questions](.claude/skills/validation-gates/references/when-to-stop-and-ask.md)
  when a decision is unresolved.
- Sensitive boundaries that need the task to require them, not convenience:
  shared foundation, public routes, Room schemas, dependencies and conventions,
  app shells, network/auth core, tokens, CompositionLocals, `DialogConfig`.
  Changing one of them beyond what the task requires is scope expansion.
  Silence is not acceptance.
- MUST NOT edit `secure/` or `local.properties` as incidental cleanup.

## Verification policy

- MUST review the actual source/diff against the request and every owning skill's
  completion gate before reporting completion.
- Default build check for any Kotlin or resource change:
  `./gradlew :androidApp:assembleDebug`.
- Also run `./gradlew :shared:assembleSharedDebugXCFramework` when the change
  touches any of: `iosMain`; an `expect`/`actual` pair; public API exported to
  Swift (`:shared`, `:toolkit:*:api`, Decompose-facing types); build conventions
  or the version catalog; any Room schema or migration; KSP configuration; or
  when the task is about iOS.
- For a requested TestFlight release that changes only iOS version numbers,
  a successful complete Release XCFramework and signed Xcode archive satisfy the
  iOS framework check above. Source, resource or build changes included in the
  release retain their normal checks.
- Builds needed within the authorized scope need no separate consent; an
  explicit user restriction wins. Rulebook-only work runs the checker instead
  of an app build; Bash guard changes also run the existing guard matrix.
- A check that is available and authorized MUST run; "unverified" is acceptable
  only for checks that are unavailable or restricted by the user, and the reason
  is stated. A green build proves compilation, not runtime behavior.
- Runtime behavior (navigation, dialogs, restoration, deeplinks, rendering) is
  verified only by running the app under
  [runtime-verification](.claude/skills/runtime-verification/SKILL.md). A request
  to check, reproduce or verify on a platform authorizes the emulator/simulator
  for that platform; otherwise ask once, and report runtime behavior as
  unverified when the answer is no.
- The analytics admin synchronization gate is the one check whose absence keeps
  a task unfinished rather than merely unverified; see
  [admin-sync](.claude/skills/feature-analytics/references/admin-sync.md).
- MUST NOT add verification-only production hooks, flags, wrappers, dependencies
  or test infrastructure.

## Definition of done

A task is complete when all of the following hold:

1. Every requested outcome is traced to code and callers, including edge, error
   and cancellation paths relevant to the change.
2. The owning skills' completion gates pass on source review, using the
   behavior/visual triggers in Definitions.
3. Independent review (Definitions) found no unaccepted blocking finding, or the
   change is a small editorial change.
4. The checks required by the verification policy passed, or their absence is
   reported under that policy.
5. Records named below are updated where the task changed what they describe.
6. The reply follows the [final response format](.claude/skills/validation-gates/SKILL.md#final-response-format).

## Records

Only these files are records; everything else is reported in the reply.

| File | Holds | Rule |
|---|---|---|
| `analytics/<feature>/COVERAGE.md` (`<feature>` = any `:analytics:<name>` implementation module, screen or dialog) | The feature's current analytics coverage matrix (route -> Component -> VM -> hook or container reason, callbacks/operations, omissions), its preservation inventory and its admin-sync rows | Replace rows to reflect the current code; no dated journal. Creating the file moves that feature's rows out of the root file and leaves a pointer there |
| `analytics/COVERAGE.md` | Cross-feature analytics contract, index of per-feature files, admin synchronization evidence | Update the affected section; do not append unrelated task narratives |
| `design-system/CONSISTENCY.md` | Concrete role-to-token choices | Update when a role mapping changes |
| `docs/records/<yyyy-mm-dd>-<slug>.md` | A dated decision, review or visual-pass record | Written only when the user asks to keep one |

Tables are allowed in records; replies follow validation-gates.

## Delivery

Git actions and end-of-task hand-off are owned by [delivery](.claude/skills/delivery/SKILL.md).

## Workflow and agents

The main session applies [task-prep](.claude/skills/task-prep/SKILL.md),
[task-orchestrator](.claude/skills/task-orchestrator/SKILL.md) and
[validation-gates](.claude/skills/validation-gates/SKILL.md).
Canonical roles live in `.claude/agents/`; use
[agent runtimes](.claude/skills/task-orchestrator/references/agent-runtimes.md)
for Claude Code and Codex launch, capabilities and protection.

| Agent | Role | Rights | Skills |
|---|---|---|---|
| `scout` | Inventory callers, screens, routes, dependencies, resource usages | Read-only (guarded Bash) | - |
| `code-reviewer` | Independent review of a diff against requirements, gates and forbidden patterns | Read-only | validation-gates |
| `design-reviewer` | Independent visual/brand acceptance with the usage/state inventory and renders | Read-only | brand-design, design-system, compose-components |
| `gate-auditor` | Analytics coverage gate and preservation gate inventories and verdicts | Read-only | feature-analytics, state-preservation |
| `backend-contract-auditor` | Backend evidence gathering and drift tables before wire changes | Read-only + WebFetch | backend-contract-client |
| `analytics-admin-auditor` | Live admin reconciliation through the user's Chrome tab | Repo read-only; admin writes only when authorized | feature-analytics |
| `ui-implementer` | Screens, dialogs, navigation, UI state, drafts, VM analytics hooks, the feature's analytics-api models and `:analytics:<feature>` implementation | Write within owned paths; scoped builds; read-only git | ui-feature, state-preservation, feature-analytics, compose-components |
| `data-implementer` | Feature/Repository/UseCase, API/DTOs, Room, DataStore, mappers, Koin | Write within owned paths; scoped builds; read-only git | data-layer, mappers, di-modules, backend-contract-client |
| `design-implementer` | Tokens, components, resources, previews, artwork integration | Write within owned paths; scoped builds; read-only git | design-system, brand-design, illustrations, compose-components |
| `platform-implementer` | Conventions, catalog, modules, toolkit, shells, XCFramework | Write within owned paths; scoped builds; read-only git | platform-build-toolkit, di-modules |
| `build-verifier` | Runs scoped Gradle targets and reports compile errors | Bash builds; no edits | runtime-verification |
| `runtime-verifier` | Runs the app on emulator/simulator, drives scenarios, screenshots, logs | Bash + simulator tools; no edits | runtime-verification |
| `rulebook-maintainer` | Edits CLAUDE.md, AGENTS.md, skills, agents, hooks and records structure | Write within rulebook paths; read-only git | rulebook-maintenance |

## Skills index

| Skill | Owns | Apply when |
|---|---|---|
| [task-prep](.claude/skills/task-prep/SKILL.md) | Scope clarification, dependency ordering, questions | Any non-trivial request, before coding |
| [task-orchestrator](.claude/skills/task-orchestrator/SKILL.md) | Execution loop, delegation, independent review, recurring findings | Any implementation task |
| [validation-gates](.claude/skills/validation-gates/SKILL.md) | Cross-cut review, forbidden patterns, code style, final response format | Every review and every completion |
| [ui-feature](.claude/skills/ui-feature/SKILL.md) | Screens, dialogs, MVI, navigation, results, error pipeline, UI state and presentation metadata | Any UI/navigation change |
| [compose-components](.claude/skills/compose-components/SKILL.md) | Compose APIs, readable structure, lifetimes, performance, interaction semantics, component audits | Any Compose implementation, refactor or source review |
| [state-preservation](.claude/skills/state-preservation/SKILL.md) | Drafts, restoration, form synchronization, secret exclusion | Any behavior change |
| [feature-analytics](.claude/skills/feature-analytics/SKILL.md) | Typed analytics, screen coverage gate, privacy, admin sync | Any behavior change or analytics task |
| [data-layer](.claude/skills/data-layer/SKILL.md) | Feature/Repository/UseCase, operation outcomes, Room, DataStore, backend client, auth session | Any data/domain/storage change |
| [backend-contract-client](.claude/skills/backend-contract-client/SKILL.md) | Backend evidence gate, drift audits | Any wire change |
| [mappers](.claude/skills/mappers/SKILL.md) | Seven directional conversions, mapper file content, null guards | Any conversion change |
| [di-modules](.claude/skills/di-modules/SKILL.md) | Koin annotations, lifetimes, shared composition | Any binding/registration change |
| [design-system](.claude/skills/design-system/SKILL.md) | Tokens, components, resources, previews, semantic role implementation, visual checks | Any visual change |
| [brand-design](.claude/skills/brand-design/SKILL.md) | Visual intent, domain entity visual semantics, composition, usage/state analysis, acceptance | New or changed product UI |
| [illustrations](.claude/skills/illustrations/SKILL.md) | Decorative artwork style, generation, export, integration | Requested decorative artwork |
| [add-equipment](.claude/skills/add-equipment/SKILL.md) | Equipment identifiers, catalog integration, native diagrams and backend task handoff | New equipment types, independently selectable variants or equipment diagram work |
| [platform-build-toolkit](.claude/skills/platform-build-toolkit/SKILL.md) | Conventions, catalog, new modules, toolkit, shells, iOS framework, secrets, TestFlight release, dependency updates | Any build/platform change or requested TestFlight release |
| [runtime-verification](.claude/skills/runtime-verification/SKILL.md) | Build, install, run, drive and inspect the app on emulator/simulator | Manual runtime checks |
| [bug-investigation](.claude/skills/bug-investigation/SKILL.md) | Reproduce, locate, root-cause, fix at the owner, regression gate | Any bug report |
| [delivery](.claude/skills/delivery/SKILL.md) | Git policy, commit messages, branches, pull requests | When the user asks to commit/push/open a PR |
| [rulebook-maintenance](.claude/skills/rulebook-maintenance/SKILL.md) | Editing this rulebook: ownership map, schemas, checker, records policy | Any change to rules, skills, agents or hooks |

Global plugin skills that contradict the locked decisions MUST NOT be applied to
this repository: `anthropic-skills:android-navigation` (Compose Navigation),
`android-testing` (test infrastructure), `android-di-koin` (DSL modules,
`koinViewModel`), `android-presentation-mvi` (`SavedStateHandle`),
`android-data-layer`, `android-module-structure`, `compose-skill`. General
Compose advice (`android-compose-ui`) may be consulted; project rules win.

## When to stop and ask

Decision triggers, question format and pending answers are owned by
[scope questions](.claude/skills/validation-gates/references/when-to-stop-and-ask.md).
