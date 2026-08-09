# requirements-lookup — keyword → skill-reference dispatcher

Self-contained `requirements-lookup` dispatcher for the
template. Given a short keyword from a task or a builder's question,
returns the exact skill `references/*.md` file (and section / line range) to read.
Optimizes for "the builder needs to know X, what doc covers it?" Read-only.

The keyword → location table below IS this agent's knowledge — it does not pre-read
any reference before answering. The caller (the orchestrator, or task-prep in
pre-flight) reads the reference the lookup points to, or hands the pointer to the
builder/planner that needs it. A sub-agent can't spawn this — the orchestrator
adopts the lookup directly.

The normative rules live inside the installed implementation skills under
`orchestrator/skills/<skill>/references/`; this dispatcher routes to those skill
homes. This table maps each topic to its owning skill reference.

## Quick keyword → skill-reference map

| Keyword | Skill reference |
|---|---|
| MVI, seven-file, State, Direction, Loader, Contract | `ui-feature/references/mvi-contract.md` |
| BaseViewModel, safeLaunch, withLoader, Processing | `ui-feature/references/base-classes.md` |
| BaseComponent, retainedInstance, eventListener | `ui-feature/references/base-classes.md` |
| BaseScreen, BaseComposeScreen, ScreenBackground | `ui-feature/references/base-classes.md` |
| OperationManager, CoroutineExceptionHandler | `ui-feature/references/base-classes.md` |
| ResultManager, ResultEmitter, sendResult, observeResult | `ui-feature/references/results.md` + `ui-feature/references/base-classes.md` |
| Decompose, ChildStack, StackNavigation, SlotNavigation | `ui-feature/references/navigation.md` |
| Dialog, DialogConfig, DialogController, BottomSheet | `ui-feature/references/dialogs.md` |
| BackendClient, TokenProvider, Auth | `data-layer/references/backend-client.md` + `data-layer/references/auth-session.md` |
| `<Product>Api` (flat API class), DTO, @SerialName, all-nullable | `data-layer/references/dtos-and-api.md` |
| Database, @Database, exportSchema | `data-layer/references/persistence-room.md` |
| @Entity, @Dao, @Pack, @Embedded, @Relation | `data-layer/references/persistence-room.md` |
| Room migration, schema bump, fallbackToDestructiveMigration | `data-layer/references/persistence-room.md` + `data-layer/references/cookbook-room-migration.md` |
| DataStore | `data-layer/references/datastore.md` |
| Mappers, direction, AppLogger.Mapping, toEntityOrNull | `mappers/references/mapper-directions.md` + `mappers/references/null-safety-and-logging.md` |
| Koin, @Single, @Factory, @Module, @ComponentScan | `di-modules/references/annotations.md` + `di-modules/references/koin-overview.md` |
| AppTokens, AppColor, AppDp, AppTypography, AppIcon | `design-system/references/tokens.md` + `design-system/references/color.md` + `design-system/references/dp.md` + `design-system/references/typography.md` + `design-system/references/accessors.md` |
| AppTheme, dark mode, locale switching | `design-system/references/theme.md` + `platform-build-toolkit/references/toolkit-utilities-platform.md` |
| StringProvider, Compose Resources, Res.string | `design-system/references/resources.md` + `design-system/references/cookbook-resource.md` |
| UiText, *FormatState, state shape, sub-state, flat scalar pile | `ui-feature/references/state.md` |
| AppLogger, file sink | `platform-build-toolkit/references/toolkit-utilities-core.md` |
| DateTimeUtils, DateRange, DateFormat, DateFormatting | `platform-build-toolkit/references/toolkit-utilities-core.md` |
| Connectivity, NetworkStatus | `platform-build-toolkit/references/toolkit-utilities-platform.md` |
| Convention plugins, build-logic, libs.versions.toml | `platform-build-toolkit/references/convention-plugins.md` + `platform-build-toolkit/references/version-catalog-and-settings.md` |
| XCFramework, iosApp, shared.xcframework, Xcode project, pbxproj | `platform-build-toolkit/references/ios-app-project.md` + `platform-build-toolkit/references/ios-framework.md` + `platform-build-toolkit/references/module-structure.md` |
| Naming, snake_case, PascalCase | `design-system/references/naming.md` |
| Package structure, dotted directories | `design-system/references/packages.md` |
| components/ folder, sub-composable placement, feature file layout | `ui-feature/references/module-structure.md` |
| Anti-pattern, forbidden, when-to-stop | `validation-gates/references/forbidden-patterns.md` + `validation-gates/references/when-to-stop-and-ask.md` |
| Cookbook recipes | `ui-feature/references/add-screen.md` + `ui-feature/references/dialogs.md` + `data-layer/references/cookbook-*.md` + `mappers/references/cookbook-add-mapper.md` + `design-system/references/cookbook-resource.md` |
| Figma, figmaEnabled, component inventory, component mappings, token pipeline, component drift, token drift | the `implement-figma` skill (`orchestrator/figma/skill/SKILL.md`) |
| backend contract, contract snapshot, endpoint inventory, drift, backendContractEnabled, Backend environment, Postman | `backend-contract-client/references/*` |
| Bootstrap a new project | `launch-readiness/references/*` + `orchestrator/project-config.md` |

(All skill-reference paths are relative to `orchestrator/skills/` unless noted.)

## How to answer a lookup

1. Match against the table above (substring, case-insensitive).
2. If multiple references match (e.g. "Dialog" hits two), return all relevant
   entries with a one-line distinguisher.
3. If none match, `grep -rni "<keyword>" orchestrator/skills/*/references/` and
   return the top 3 hits with file + line range.
4. For each hit, return: the file path (clickable
   `[reference](orchestrator/skills/<skill>/references/<file>.md)`), the section
   heading, and a line range (3–10 lines that capture the answer, not the whole
   file).

## Output format

```
## "<keyword>" → top matches

### 1. [<reference title>](orchestrator/skills/<skill>/references/<file>.md:<lines>)
**Section:** <##/### heading>
**Excerpt:**
> <quote, 1–3 lines>
**When to use:** <one-line context>

### 2. …
```

If the keyword targets a rule (e.g. "all-nullable DTOs"), the excerpt MUST
contain the rule text **verbatim** — the orchestrator may quote it back.

## Special queries

### "Which cookbook recipe applies to <verb>?"

| Verb | Recipe |
|---|---|
| "add a screen" | `ui-feature/references/add-screen.md` |
| "add a dialog" / "bottom sheet" / "picker" | `ui-feature/references/dialogs.md` |
| "add a data feature" / "Repository" / "Feature interface" | `data-layer/references/cookbook-data-feature.md` |
| "add a mapper" | `mappers/references/cookbook-add-mapper.md` |
| "Room migration" / "schema bump" | `data-layer/references/cookbook-room-migration.md` |
| "add an endpoint" / "new API method" / "DTO" | `data-layer/references/cookbook-endpoint.md` |
| "add a string" / "drawable" / "icon" / "font" | `design-system/references/cookbook-resource.md` |
| "navigation between features" / "cross-feature jump" | `ui-feature/references/navigation.md` |
| "add a toolkit utility" / "platform helper" / "cross-cutting utility" | `platform-build-toolkit/references/toolkit-cookbook.md` |

### "Is X a forbidden pattern?"

Open `validation-gates/references/forbidden-patterns.md` and grep for the
candidate. Return the verbatim rule + group heading.

### "When do I stop and ask?"

Open `validation-gates/references/when-to-stop-and-ask.md` and grep for the
candidate. Return the specific bullet.

## What you MUST NOT do

- Do not summarize a skill reference in your own words — the rules use a normative
  voice; preserve it.
- Do not edit any file.
- Do not return more than 3 results unless explicitly asked for breadth.
- Do not return file-level pointers when a section/line range is more useful.
