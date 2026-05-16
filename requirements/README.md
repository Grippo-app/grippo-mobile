# System Requirements — KMP Mobile Project

This folder contains the system requirements for a new Kotlin Multiplatform (KMP) mobile project. The requirements are derived from the architecture of the `grippo-mobile` reference project and are intended to be **technology- and product-agnostic**: package names, product strings, and domain types from the reference project must be replaced when applied to a new project.

## How to read this

- Each top-level folder is a **chapter**. Folders are numbered for reading order; files inside each folder are also numbered.
- Files are written as normative requirements: "module X **MUST** depend on Y", "every screen **MUST** consist of seven files", etc. Where a rule has a stated trade-off, the trade-off is named.
- All Kotlin code in the documents is a **reference implementation**, not a literal copy-paste target. Substitute the placeholders (`<Product>`, `<org>`, `<product>`, `<product-domain>`, `com.<org>.<product>`) with the new project's values per the Replacement checklist below and per `00-overview/05-template-conventions.md` §1.
- Where the reference repo has an **intentional inconsistency** (e.g. dotted directory names in some legacy modules), the document calls it out — new code should follow the modern convention.

## Index

### Foundations

- `00-overview/` — vision, glossary, architecture map.
- `01-tech-stack/` — Kotlin/Compose/Decompose/Koin/Ktor/Room versions and the version catalog.
- `12-gradle-build/` — convention plugins, settings.gradle, sample module builds, iOS XCFramework setup.

### Structure

- `02-module-structure/` — module graph, dependency rules, what each module group contains.
- `08-dependency-injection/` — Koin composition root, annotation usage.

### Patterns

- `03-architecture-patterns/` — MVI, Decompose navigation, dialog navigation, cross-component results, data flow, repository pattern, error pipeline, process-death restoration.
- `04-base-classes/` — `BaseViewModel`, `BaseComponent`, `BaseScreen`, `OperationManager`, `ResultManager`, platform helpers.

### UI

- `05-design-system/` — `AppTokens`, color/dp/typography tokens, theme, preview.
- `11-state-and-formatters/` — `UiText`, `*FormatState` (email/password/name/date/date-range/amount).

### Data

- `06-data-layer/` — Ktor `BackendClient`, `TokenProvider` (mutex + retry), `<Product>Api` (single-file flat API), DTOs (all-nullable convention), Room database, entities + `*Pack` (`@Embedded`+`@Relation`), migrations, DataStore.
- `07-mappers/` — seven directional mapper modules, null-friendly mapping pattern.

### Utilities

- `10-toolkit/` — context, http-client, serialization, logger, date-utils, connectivity, notification/permission managers, link-opener, theme/localization, image-loader.

### Style and discipline

- `09-conventions/` — Kotlin style, naming, packages, Compose-specific rules.
- `13-anti-patterns/` — forbidden patterns; when to stop and ask.
- `14-cookbook/` — step-by-step recipes for the most common tasks.

### Iteration

- `launch.md` — the iterative prompt used to bootstrap a new project from scratch using these requirements.

## Reference project

These requirements were extracted from a working KMP project. When in doubt, **read working code from that reference** as the ground truth. These documents are a high-fidelity summary, not a replacement.

## Replacement checklist (before applying to a new project)

When forking these requirements for a new project, replace these tokens consistently across every document:

| Token | Reference value | New value |
|---|---|---|
| Root package | `com.grippo.*` | `com.<org>.<product>.*` |
| API class prefix | `Grippo` (e.g. `GrippoApi`) | `<Product>` |
| Root project name | `grippo-mobile` | `<product>-mobile` |
| iOS XCFramework name | `shared` | (keep or rename) |
| Backend host | `grippo-app.com` | `<product-domain>.com` |
| Android `namespace` root | `com.grippo` | `com.<org>.<product>` |
| Application ID | `com.grippo.android` | `com.<org>.<product>.android` |
| Domain types | `Training`, `Exercise`, ... | new-project domain |
| Primary example type | `Training` (in pattern walkthroughs) | `Note` |
| Secondary example type | `Muscle`, `Equipment` (in `@Relation` examples) | `Tag` |
| Tertiary example type (if needed) | other domain | `Item` |
| Numeric `*FormatState` | `WeightFormatState`, `HeightFormatState`, ... | `AmountFormatState` |
| Worked example | "Workout history" | "Note archive" |

Full conventions: see `00-overview/05-template-conventions.md`.

Do **not** rename: `BaseViewModel`, `BaseComponent`, `BaseScreen`, `AppTokens`, `AppTheme`, `UiText`, `EmailFormatState`, `PasswordFormatState`, `NameFormatState`, `DateFormatState`, `DateTimeFormatState`, `DateRangeFormatState`, `AmountFormatState`, `OperationManager`, `ResultManager`, `DialogConfig`, `DialogController`, `NativeContext`, `AppLogger`, `DateTimeUtils`. These are infrastructure names; keeping them stable across projects makes the pattern recognizable.

Per-project values (productName, locales, prelaunch flag, etc.) live in `requirements/00-overview/03-project-config.md`. Sub-agents read this file at start of every task. Edit it before bootstrapping a new project.

## What ships to a new project

`requirements/` is the portable package — copy this entire folder to a new KMP project's root and follow `launch.md`. Everything inside `requirements/` is project-agnostic (after applying `launch.md` Step 1.5 substitutions).

## Sub-agents — install before first use

The `sub-agents/` folder ships specialized Claude Code sub-agents (orchestrator, builders, validators, helpers) that automate task execution against this architecture. They are **not** auto-discovered by Claude Code — they must be installed into the new project's `.claude/agents/` directory before they become callable as `subagent_type` values. Without this step, `Agent(subagent_type: "orchestrator", ...)` and every related call fail with an "unknown agent type" error.

Recommended (symlink — edits in `requirements/sub-agents/` propagate immediately):

```bash
mkdir -p .claude/agents
ln -sf "$(pwd)/requirements/sub-agents/builders/"*.md   .claude/agents/
ln -sf "$(pwd)/requirements/sub-agents/validators/"*.md .claude/agents/
ln -sf "$(pwd)/requirements/sub-agents/helpers/"*.md    .claude/agents/
```

Alternative (copy — snapshot, no propagation):

```bash
mkdir -p .claude/agents
cp requirements/sub-agents/builders/*.md   .claude/agents/
cp requirements/sub-agents/validators/*.md .claude/agents/
cp requirements/sub-agents/helpers/*.md    .claude/agents/
```

After installation, drop a task spec at `requirements/tasks/TASK_<N>_<title>.md` and ask Claude to *"run task TASK_N_<title>.md"*. The parent session invokes `orchestrator`, which drives the rest. See `sub-agents/README.md` for the full agent inventory, execution flow, and Codex-plugin integration.
