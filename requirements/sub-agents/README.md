# Sub-Agents — Task Execution Toolkit

Specialized Claude Code sub-agents that implement and verify changes against the architecture defined in `requirements/`. The set is split into three roles:

| Role | Purpose | Folder |
|---|---|---|
| **Builders** | Implement a specific kind of change (screen, dialog, data feature, mapper, endpoint, migration, resource, cross-feature nav). | `builders/` |
| **Validators** | Self-check the result against the architecture (dependency rules, MVI pattern, anti-patterns, naming, DI wiring, Compose stability, data-layer boundaries, build green). They are **gates**: the orchestrator does not declare a task done until every relevant validator passes. | `validators/` |
| **Helpers** | Task intake, orchestration, codebase lookup, Codex review loop. | `helpers/` |

## Execution flow

```
requirements/tasks/TASK_N_TITLE.md
        ↓
[helpers/task-intake] — parses the task, picks builders + validators
        ↓
[helpers/orchestrator] — runs the loop:
        ├── builders/* (one or more, in dependency order)
        ├── validators/* (every applicable one — all MUST pass)
        ├── (optional) codex-plugin-cc review
        └── [helpers/codex-review-loop] — digests codex feedback, re-runs builders
        ↓
Task done (under-key) when every validator passes AND codex has no further findings.
```

Helpers and validators run **on every task** (validators are always-on invalidators of premature "done"). Builders run only when their kind of change is requested.

## How tasks are written

Place task specs at `requirements/tasks/TASK_<N>_<TITLE>.md`. A task file states **what** to build, not **how** — the agents read `requirements/` for the architecture. Minimum task shape:

```markdown
# TASK 1 — Add "Workout history" screen

## Goal
Add a sub-screen to `:ui-screen-features:profile` showing the user's workout
history for a configurable date range.

## Inputs
- Source data: `WorkoutHistoryFeature.observeWorkoutHistory(start, end)` (assume it exists).
- Entry point: a card on `ProfileBodyScreen` already triggers `onWorkoutHistoryClick`.

## Acceptance
- New route under `ProfileRouter.WorkoutHistory(initialRange: DateRange)`.
- Seven MVI files following `requirements/14-cookbook/01-add-screen.md`.
- iOS XCFramework + Android debug both build green.
```

The task file is the **only** thing the user has to write. The agents pull architecture from `requirements/` and the live code.

## Triggering an execution

1. Drop the task file in `requirements/tasks/`.
2. Tell the parent Claude session: *"Run task `TASK_1_workout_history.md`."*
3. Parent invokes `helpers/orchestrator`.
4. Orchestrator drives the rest.

The user does not invoke individual builders or validators by hand. They can, though, for one-off touch-ups (e.g. *"run validators/anti-pattern-scanner on the current branch"*).

## Codex plugin integration

The Codex plugin (`https://github.com/openai/codex-plugin-cc`) provides an external reviewer. The orchestrator hands off to it **after** every internal validator passes — the plugin's review becomes the second gate. If Codex flags issues, `helpers/codex-review-loop` digests the report, re-routes to the right builder, and re-runs validators. The cycle repeats until Codex returns clean.

The plugin must be installed in the parent Claude Code session. See the plugin's README for installation.

## Installing the agent definitions

These markdown files are written in the standard Claude Code sub-agent format (YAML frontmatter + body). To make them discoverable as `subagent_type` values:

```bash
# Option A: copy into the project's local agents directory
mkdir -p .claude/agents
cp -R requirements/sub-agents/builders/*.md .claude/agents/
cp -R requirements/sub-agents/validators/*.md .claude/agents/
cp -R requirements/sub-agents/helpers/*.md .claude/agents/

# Option B: symlink so edits stay in one place
mkdir -p .claude/agents
ln -sf "$(pwd)/requirements/sub-agents/builders/"*.md .claude/agents/
ln -sf "$(pwd)/requirements/sub-agents/validators/"*.md .claude/agents/
ln -sf "$(pwd)/requirements/sub-agents/helpers/"*.md .claude/agents/
```

Option B is recommended for an active project — every edit under `requirements/sub-agents/` propagates immediately.

## Agent inventory

### Builders (one per cookbook recipe)

| Agent | What it builds | Recipe |
|---|---|---|
| `screen-builder` | A sub-screen inside an existing `:ui-screen-features:*` feature. | `14-cookbook/01-add-screen.md` |
| `dialog-builder` | A `:ui-dialog-features:*` bottom sheet feature. | `14-cookbook/02-add-dialog.md` |
| `data-feature-builder` | A `:data-features:*` module + feature-api contract. | `14-cookbook/03-add-data-feature.md` |
| `mapper-builder` | A new mapper file inside an existing `:data-mappers:*` direction. | `14-cookbook/04-add-mapper.md` |
| `endpoint-builder` | A new method on `<Product>Api` + DTOs. | `14-cookbook/06-add-endpoint.md` |
| `room-migration-builder` | A new `Migration<N>To<N+1>` + schema bump. | `14-cookbook/05-add-room-migration.md` |
| `resource-builder` | Strings (all locales), drawables, icons, fonts. | `14-cookbook/07-add-resource.md` |
| `cross-feature-nav-builder` | Wires navigation from one feature into another. | `14-cookbook/08-add-cross-feature-nav.md` |

### Validators (always-on invalidators)

| Agent | What it checks | Authoritative chapter |
|---|---|---|
| `architecture-validator` | Module dependency rules (UI → feature-api only, no data-services from UI, etc.). | `02-module-structure/02-dependency-rules.md` |
| `mvi-contract-validator` | Seven-file MVI per screen/dialog; `State` defaults, `Contract.Empty`, `BaseViewModel` ctor pattern. | `03-architecture-patterns/01-mvi-contract.md`, `04-base-classes/*` |
| `anti-pattern-scanner` | Forbidden patterns (viewModelScope, `runBlocking`, `LaunchedEffect(Unit)` for nav, `Color(0xFF...)` inline, `stringResource(R.string...)`, ...). | `13-anti-patterns/01-forbidden-patterns.md` |
| `naming-convention-validator` | File/class/function/package naming, sealed-type shape, `stub*()` previews. | `09-conventions/02-naming.md`, `09-conventions/03-packages.md` |
| `di-validator` | `@Single(binds = [...])`, `@Module @ComponentScan`, module registered in `:shared/Koin.kt`. | `08-dependency-injection/*` |
| `compose-stability-validator` | `@Immutable` on state, `ImmutableList`/`ImmutableSet`, no `mutableStateOf` for logical state, no inline `dp`/`sp`/colors outside design-system. | `09-conventions/04-compose-rules.md`, `13-anti-patterns/01-forbidden-patterns.md` |
| `data-layer-validator` | DTO all-nullable + default `= null`, Repository returns domain (never DTO), mappers via `:data-mappers:*`, range reconciliation, `AppLogger.Mapping.log` in DTO→Entity. | `06-data-layer/*`, `07-mappers/*` |
| `build-validator` | `./gradlew :shared:assembleSharedDebugXCFramework` and `./gradlew :androidApp:assembleDebug` both green. | n/a — runs Gradle directly. |

### Helpers

| Agent | What it does |
|---|---|
| `task-intake` | Reads `requirements/tasks/TASK_*.md`, classifies the change (screen / dialog / data-feature / …), enumerates the builders and validators that apply, and returns a structured execution plan to the orchestrator. |
| `orchestrator` | Drives the full execution loop: builders → validators → codex review → fixes → repeat. The single agent the parent session invokes per task. |
| `context-finder` | Locates the existing modules/files relevant to a task (e.g. find `ProfileComponent.kt`, `ProfileRouter.kt`, the existing `ProfileBodyState.kt` shape) so a builder doesn't have to re-grep. |
| `requirements-lookup` | Given a short keyword from a task or a builder's question, returns the exact `requirements/*.md` chapter and line range to read. |
| `codex-review-loop` | Wraps the Codex plugin's review output, classifies findings (architecture / style / bug / scope), and routes each to the right builder for the next iteration. |

## Tool budgets

Each agent declares a minimal tool set in its frontmatter:

- **Builders**: `Read, Edit, Write, Bash, Grep, Glob` — they need to read existing code, write new files, and run targeted gradle assembles.
- **Validators**: `Read, Bash, Grep, Glob` (no `Edit`/`Write`) — they report findings, they don't auto-fix. Auto-fixing belongs to the builder responsible for the violated rule.
- **Helpers**: vary. `orchestrator` gets `Agent` (to spawn the others); `task-intake` and `context-finder` get read-only tools; `codex-review-loop` gets `Bash` + `Agent`.

## What sub-agents do NOT do

- **No new module without an explicit task.** `data-feature-builder` adds a `:data-features:<x>` module because the task asked; it does not add unrelated modules in passing. See `13-anti-patterns/02-when-to-stop-and-ask.md`.
- **No refactors.** A builder fixes exactly what the task requested. Drift in adjacent code is logged, not patched.
- **No version bumps.** `gradle/libs.versions.toml` is off-limits unless a task explicitly targets it.
- **No backend changes.** Mobile-side only. Endpoint shape comes from the backend contract — the `endpoint-builder` verifies the DTO shape but does not invent endpoints.
- **No tests.** This project explicitly opts out of tests by default. Tests are a separate task on explicit request.

## Self-improvement

The sub-agents drift the same way the requirements do — as the codebase evolves, an agent's prompt may reference a renamed class, an outdated rule, or a chapter that moved. See **`invalidate.md` → "Sub-agent auto-calibration"** for the iterative pass that audits each agent against the live requirements and code.
