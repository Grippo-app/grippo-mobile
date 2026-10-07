# Forbidden patterns (anti-patterns reference)

> This skill **owns** this rule set — it is the rule source for `anti-pattern-scanner`
> and the reviewer. The list below is the complete rule set; the
> forbidden-patterns list must not lose entries. Reviewer-only items (no mechanical
> validator) are additionally registered in `../../_index/known-gaps.md`.

Things you must not do — refuse to write them, refuse to commit them. Listed roughly by layer.

## Coroutines

- **`viewModelScope.launch`, `lifecycleScope.launch`, `GlobalScope.launch`, `CoroutineScope(...).launch`** inside a `BaseViewModel`. Only `safeLaunch` / `Flow.safeLaunch()`.
- **`runBlocking { ... }`** anywhere in production. Tests only.
- **Manual `CoroutineExceptionHandler`** inside a VM. The pipeline handles errors.
- **`try { ... } catch (e: Throwable) { ... }`** inside a VM. Two carve-outs only: (1) `Result.onSuccess { ... }` / `runCatching { ... }` at a domain boundary (collapsing a thrown exception into a `Result`), or (2) the VM genuinely wants to recover and **not** show an error to the user — rare, requires a code comment explaining why (see the ui-feature skill, references/error-pipeline.md § Authoring rules). Otherwise let errors flow through `safeLaunch` → `ErrorProvider`.
- **`async { }.await()`** to start a single piece of async work. Use `safeLaunch { ... }`. `async` is for parallel forks.

## Collections in state

- **`List<T>`, `Set<T>`, `Map<T>`** (Kotlin defaults). Not Compose-stable.
- **`mutableListOf()`, `mutableStateListOf()`, `mutableMapOf()`** in state. Mutable types break immutability discipline.
- **`buildList { ... }`** if the result is stored in state without `.toImmutableList()`. *(Reviewer-only — see ../../_index/known-gaps.md → pattern-orphans.)*

## Compose

- **`LaunchedEffect(Unit) { navigateTo(...) }`** for navigation. Use `Direction` + `eventListener`.
- **`mutableStateOf(...)` for logical state**. Logical state goes in the ViewModel; `mutableStateOf` is for local UI animations.
- **`stringResource(R.string.foo)`** from `androidx.compose.ui.res`. Android-only — breaks iOS build.
- **`painterResource(R.drawable.foo)`** from `androidx.compose.ui.res`. Same.
- **Hardcoded `Color(0xFF...)`** in feature code. Use `AppTokens.colors.*`.
- **Hardcoded `12.dp`, `14.sp`**. Use `AppTokens.dp.*`, `AppTokens.typography.*`.
- **`TextStyle(fontSize = ...)` inline** in feature code. Use `AppTokens.typography.<token>()`.
- **`MaterialTheme.colorScheme.*`** in feature code. Use `AppTokens.colors.*`.
- **`androidx.compose.material3.Button`** in feature code. Use `:design-system:components/Button`.
- **`@Composable` calls from a `BaseViewModel`.** VMs don't know Compose.
- **Flat-scalar explosion of a domain entity in a composable signature.** A composable that renders an entity takes its `*State` model as one parameter, not `name: String, surname: String, avatarUrl: String, …` mirroring the model. Exception: domain-agnostic primitives in `:design-system:components` / `:compose-libs:*` (Button, Chip) stay slot/enum-driven. See the ui-feature skill, references/compose-rules.md § "Entity composables". Not mechanically greppable — reviewer-enforced (see `../../_index/known-gaps.md` → pattern-orphans).
- **Non-`*Screen` UI placed flat beside the seven MVI files**, or inlined into a fat `*Screen.kt`, instead of the sub-screen's `components/` subfolder (one cohesive composable per file). See the ui-feature skill, references/module-structure.md § "File layout for a feature". Reviewer-enforced (see `../../_index/known-gaps.md` → pattern-orphans).

## Data layer

- **`Flow<Result<T>>`** for observation. Observations always succeed; `Result` is for mutations.
- **Returning DTOs from a Repository.** Map to domain.
- **Inline mappers in Repository / ViewModel.** Use `:data-mappers:*`.
- **`!!` on a DTO field.** Forbidden; use `?: return null` (via `AppLogger.Mapping.log`).
- **Non-null response DTO fields.** All `<Name>Response` fields are nullable + default `= null` (defensive). Request `<Name>Body` fields are the exception — non-null (client-controlled); see the data-layer skill, references/dtos-and-api.md.
- **Required entity fields nullable.** Entities are post-validation; nullable belongs in DTOs.
- **Raw `String` for a closed-set field in a *domain* model.** A field from a backend dictionary (type, status, kind, category, …) is a domain `<X>Enum` (pure labels — `key` + `companion object of(key)`) or a `sealed interface <X>` (variants carry payload — base `val key` + per-variant `data class`), not a bare `String`. The raw `String` stays only in the DTO / Entity replica; the `…→domain` mapper promotes it via `<X>Enum.of(value)`. Same stance for timestamps (`LocalDateTime`, not `String`) and durations (`Duration`, not `Long`). See the data-layer skill, references/module-structure.md § "Strict domain typing".
- **`@PrimaryKey(autoGenerate = true) val id: Long`** in entities. IDs come from the server.
- **Catching exceptions in a Repository** silently. `runCatching { ... }` returns `Result`; let the caller handle.
- **Writing to a DAO before `Result.onSuccess { ... }`**. Speculative writes leave the cache inconsistent on failure.
- **Skipping range reconciliation** (`deleteByCreatedAtRangeExceptIds`). "Deleted on another device" rows linger.
- **Bypassing `BackendClient`** with a raw `HttpClient.request { ... }`. All HTTP goes through `<Product>Api → BackendClient`.
- **Direct `Context` import in `commonMain`**. Use `NativeContext` from `:toolkit:context`.
- **Subgrouping `<Product>Api`** into `AuthApi`, `NotesApi`, etc. Flat is intentional. *(Reviewer-only — see ../../_index/known-gaps.md → pattern-orphans.)*

## Dependency Injection

- **Hand-written `module { single { ... } }`** Koin DSL for new code. Use annotations (`@Single`, `@Factory`, `@Module @ComponentScan`).
- **`@Single` without `binds = [Interface::class]`** when registering an impl for an interface. Consumers can't `inject<Interface>()`.
- **Missing module in `:shared/Koin.kt`.** Runtime "no definition found".
- **`getKoin().get()` in a Composable.** Wrong layer.
- **Module hand-DSL** unless it is in tests, listed in `diHandWrittenModules`, or the built-in `FeatureApiModule` UseCase-aggregator (see the di-modules skill, references/annotations.md).
- **`@Factory` for stateless services.** Use `@Single`.
- **A Koin-injectable type as a `*Component` constructor parameter.** A Decompose `*Component` receives only its `ComponentContext`, navigation args, and `() -> Unit` callbacks. Feature interfaces, UseCases, `DialogController`, repositories, managers, and auth/link providers are Koin-injectable and must be resolved via `getKoin().get()` inside the component's own `componentContext.retainedInstance { <X>ViewModel(...) }` block — never threaded through the constructor down to the ViewModel. See the ui-feature skill, references/base-classes.md § `BaseComponent`.

## Navigation

- **Compose Navigation alongside Decompose.** One nav library only. *(Reviewer-only — see ../../_index/known-gaps.md → pattern-orphans.)*
- **`LaunchedEffect(Unit) { navigate(...) }`.** Direction + eventListener.
- **Routes carrying lambdas.** Not serializable; crash on backgrounding.
- **Routes without `@Serializable`.** Same.
- **Mutable routes** (with `var` fields). Routes are `data class` / `data object`. *(Reviewer-only — see ../../_index/known-gaps.md → pattern-orphans.)*
- **Cross-feature import**: `:ui-screen-features:home` importing from `:ui-screen-features:profile`. Use `:ui-screen-features:screen-api`.

## Errors

- **Manual `try/catch` in VM** to hide errors. Use the pipeline (for the two permitted carve-outs see § Coroutines above).
- **Custom error dialogs** outside `DialogConfig.ErrorDisplay`.
- **Recording exceptions to Firebase manually.** Pipeline does it.
- **`Result.getOrNull()` without handling `null`.** Silently drops failures.
- **Catching `CancellationException`.** Must propagate.
- **Throwing raw `Throwable` from `validateResponse`.** Throw `AppError` subtypes. *(Reviewer-only — see ../../_index/known-gaps.md → pattern-orphans.)*

## State

- **`String` for localizable values.** Use `UiText`.
- **`String` for form fields.** Use `*FormatState`.
- **`var` in state classes.** All `val`.
- **Mutable collections in state.** Immutable only.
- **State that duplicates a sub-state**'s field. Derive locally.
- **Flat scalar pile.** 15+ primitive fields, or booleans encoding one lifecycle (`isRunning` + `hasCompletedTest` + `activePhase`). Group clusters into sub-states, fixed sets into enums, the lifecycle into a sealed interface. See the ui-feature skill, references/state.md § "No flat scalar piles". Reviewer-enforced (see `../../_index/known-gaps.md` → pattern-orphans).

## Build

- **Inline version strings** (`"2.3.21"`). Use the catalog.
- **`apply(plugin = "...")`** instead of `plugins { id("...") }`.
- **`repositories { ... }` in module scripts.** Forbidden by `FAIL_ON_PROJECT_REPOS`.
- **`compileSdk = 36` in a module**. Already in the convention.
- **`@OptIn(...)` for globally-opted-in experimentals**. Already in `KotlinMultiplatformConventionPlugin`.
- **`api(...)` everywhere.** Default is `implementation`.
- **`mavenLocal()`** in production builds.
- **Multiple `Json` instances** with different configs. One `:toolkit:serialization` singleton. *(Reviewer-only — see ../../_index/known-gaps.md → pattern-orphans.)*

## Logging

- **`println(...)`** in production. Use `AppLogger`.
- **`android.util.Log.*`** in `commonMain`. Doesn't compile.
- **Logging PII** (full tokens, emails). Truncate or hash. *(Reviewer-only — see ../../_index/known-gaps.md → pattern-orphans.)*
- **Calling `AppLogger.General.error(...)` after a Ktor exception.** Pipeline already logs.
- **Skipping `AppLogger.Mapping.log(value) { msg }`** in DTO → Entity / Domain mappers. Required.

## Testing

- **Production code that requires test-only setup.** Architecture must be test-friendly without backflips: constructor injection and boundary interfaces, never `if (isTest)`, public singleton resets, global fake modes or reflection into private state.
- **Test dependencies added by hand or outside a test-bearing module.** Capabilities arrive only through the opt-in `*.test.convention` plugins; production source sets, publications and the XCFramework stay test-free.
- **A test that merely restates the implementation.** Expected values derive from the acceptance contract, not from copying the production formula; a test that would still pass on the old bug proves nothing.
- **`assertTrue(true)`/non-null-only assertions, sleeps, retry-until-pass, shared mutable fixtures, real network or production databases in tests.**
- **Weakening or deleting an existing assertion to make a change green.** A removed contract needs an owner decision, not a quieter test.
- **A screenshot standing in for a behavioral test.** Roborazzi captures prove "looks like the design"; interaction and state contracts need their own tests (machine authority: `orchestrator/tasks/test-policy.json`).

## Resources

- **Hardcoded strings in Composables.** Extract.
- **Missing keys in a locale's `strings.xml`.** Translator might miss; runtime falls back to English.
- **PNG icons for vector candidates.** Use SVG/vector. *(Reviewer-only — see ../../_index/known-gaps.md → pattern-orphans.)*
- **Resource files in feature modules.** Centralize in `:design-system:resources:provider`.
- **`@JvmStatic`** in `commonMain`. JVM-only. *(Reviewer-only — see ../../_index/known-gaps.md → pattern-orphans.)*

## Inline `ImageVector.Builder` placeholders in `:design-system:components/*.kt`

When `AppTokens.icons` has no real icons yet, the temptation in a widget's `@Preview` is to hand-roll a diamond `ImageVector.Builder` inline so the preview compiles. Forbidden:

- Inline placeholder ImageVectors decay into permanent code — the preview ships diamond-glyph artifacts long after real icons exist.
- They live in `:design-system:components`, but `ImageVector` authoring belongs in `:design-system:resources:provider/icons/` — splits ownership.
- They block `Find Usages` on real icons: `AppTokens.icons.Bell` will never surface a `<Widget>.kt` placeholder that's "kind of" an icon.

**Do instead:** either (a) add the real icon to `AppIcon` first (one `resource-builder` task), then use `AppTokens.icons.<Name>` in the preview, or (b) skip the icon-bearing preview variant entirely until a real icon exists. This pattern is reviewer-enforced (see `../../_index/known-gaps.md` → pattern-orphans); the grep `ImageVector.Builder(` under `:design-system:components/` is the suggested manual check.

## Deprecation

New code uses the **newest non-experimental API available in the current codebase**. Most rules above are deprecation rules in disguise (`viewModelScope.launch`, `MaterialTheme.*` in feature code, `module { ... }` hand-DSL, …). This section collects the residual rules under one principle. The rules in this skill are the authoritative form for agent-written code.

- **`@Deprecated`-annotated API in a new call site** when a non-deprecated alternative exists in the codebase. If only the deprecated form is available (replacement unstable), write the call AND record it in `## Outcome` → `### Caveats` with a one-line reason; add a `// why:` line at the site.
- **Pre-Compose / pre-Decompose / pre-Koin-annotation patterns.** No `android.view.View` subclassing, no XML layouts, no `androidx.fragment.app.Fragment`, no `androidx.lifecycle.ViewModel`, no hand-written `module { single { ... } }` Koin DSL outside the `diHandWrittenModules` allowlist in project-config or the built-in `FeatureApiModule` UseCase-aggregator (see the di-modules skill, references/annotations.md).
- **Deprecated coroutines APIs.** Use `Flow.collect(collector)` / `flow.onEach { ... }.launchIn(scope)`. Forbidden: the deprecated `Flow.collect { suspending-lambda }` overload, `kotlinx.coroutines.experimental.*`, the old reactive-streams adapters in `kotlinx-coroutines-reactive`.
- **`Channel` for UI / data observation** where a `Flow`-equivalent exists. Channels are for cross-coroutine handoff and one-shot signals; observation is `Flow`. *(Reviewer-only — see ../../_index/known-gaps.md → pattern-orphans.)*
- **Material1 / Material2 widgets or `MaterialTheme.*` in feature code.** Use `:design-system:components` + `AppTokens.*`. See § Compose above for the specific imports.
- **Code that requires the old API surface of a library when the catalog pins a newer version.** `gradle/libs.versions.toml` is authoritative — write against the pinned version, not against a remembered older one. Bumps to the catalog are off-limits unless the task explicitly targets them.
- **Introducing a new `@Deprecated` annotation in a feature task.** Deprecating an existing symbol changes the public contract and routes consumers through a follow-up cycle; that belongs to a dedicated task, not to a feature task that happens to touch the file.

When two non-deprecated APIs are available and you can't tell which is current, defer to the one used by the skills' cookbook references. If the cookbook itself uses a now-deprecated API, raise a follow-up task to update the cookbook — don't silently substitute a third API.

## Outcome appendix

The orchestrator writes a structured `## Outcome` appendix to every task it moves to `done/` (see the task-orchestrator skill — references/outcome-appendix.md). The site parses this appendix to render the done card; drift in shape silently breaks the board. The heading list and the `Status`/`Reviewer` value sets restated below are canonical in `orchestrator/contracts/outcome-shape.json` (see outcome-appendix.md's field rules) — if that contract changes, re-sync this prose. The shapes below are forbidden.

- **Skip the outcome appendix on a done transition.** Forbidden. Every task that lands in `done/` must carry the full `## Outcome` section (heading + six sub-sections: `### Build gates`, `### Runtime verify`, `### Acceptance trace`, `### Caveats`, `### Follow-ups`, `### Files touched`, plus all four typed fields; the optional `### Execution log` digest — see the task-orchestrator skill, references/outcome-appendix.md — may additionally follow `### Files touched`, but it is parser-additive and never required). The typed fields must satisfy the canonical enums/timestamp/integer contract; if one cannot be established, finalization stops instead of inventing `<unknown>`. For list sections that permit it, use exactly one `- none` bullet rather than omitting the heading or leaving it empty.
- **Free-form prose in `### Caveats` or in an `### Acceptance trace` note.** One bullet = one line ≤120 characters. Long discussions belong in a follow-up task with its own backlog file, not in the appendix.
- **Edit an outcome appendix after the file lands in `done/`.** Done is immutable. If the outcome is wrong, invoke the canonical `transition-task-state.mjs reopen` flow (which archives the exact done bytes and strips the appendix), then fix the work and re-run. Never move or edit the durable column files directly.
- **`## Outcome` section in a `backlog/`, `pending/`, or `todo/` file.** Forbidden — the appendix only belongs in `done/`. `task-intake` rejects todo files that carry the trailer; `task-prep` rejects backlog/pending files that do.
- **`partially-completed` task in `done/` without a matching `### Follow-ups` bullet.** A partial completion means something wasn't delivered; the missing piece must exist as a follow-up task (backlog or todo). An orphan `partially-completed` status is a record of failure with no path forward.

*(The `## Outcome appendix` shape is self-enforced — see ../../_index/known-gaps.md → self-enforced.)*

## Orchestrator scope

The orchestrator coordinates. It does not write product code, and it is never itself a reviewer. See the task-orchestrator skill references: `run-loop.md` for the hard no-inline-write rule and `validator-routing.md` Step 5 for review fallback.

- **Orchestrator inline-write of product code.** Forbidden. If `task-intake` returns a task that does not map to any builder skill, the orchestrator escalates with `BLOCKED: no builder maps to this task` and offers the user three options (extend the catalog / split the task / reject). It does NOT silently write the code itself. This rule has no exception for "trivial" or "one-line" code — trivial product code is still product code.
- **Self-review as fallback for external review.** Forbidden. When the configured reviewer (Codex or `internal-reviewer`) is unavailable, the orchestrator escalates per the `codexEnabled` policy. It never performs an inline review pass of its own. Self-review by the agent that coordinated writing the code yields zero independence — the writer's blind spots become the reviewer's blind spots and the pass means nothing.

*(`## Orchestrator scope` is self-enforced — see ../../_index/known-gaps.md → self-enforced.)*

## Comments

The root project rule is "default to writing no comments". The rules in this skill are the authoritative form for agent-written code. The shapes below are forbidden — `anti-pattern-scanner` greps for them and routes findings back to the responsible builder.

- **`TODO` / `FIXME` / `XXX` in code the builder claims as done.** If the work isn't done, the task isn't done — escalate, don't park.
- **Task-ID citations in code comments.** `// see TASK_12`, `// added for TASK_4` — IDs belong in commit messages and the `## Outcome` appendix, not in code that outlives them.
- **Question-ID citations in code comments.** `// see Q3=a`, `// per Q5`, `// Q2=b` — these reference the pending sidecar's transient shape; they're meaningless six months later.
- **File-level prose comments added by the task.** A contiguous block of more than five added `//` (or `*` inside `/* */`) lines that introduces the module / explains its design / restates the trade-off. Move the prose to a skill reference or to `## Outcome` → `### Caveats`.
- **Step-by-step narration of what the code does.** Numbered comments like `// 1. Build query / // 2. Run it / // 3. Map result` describe what the next lines already say. The code is the documentation.
- **Restatement of a builder's trade-off in code.** The reasoning behind a choice (chose A over B because…) belongs in the task's `## Outcome` → `### Caveats`, not inline. The code only shows the choice that won.

## Architecture-shape

- **UI module imports `:data-services:*` directly.** Use `:data-features:feature-api`. Narrow exceptions tolerated for SDK-style services that have no `:data-features:*` wrapper: `:data-services:firebase` (a UI VM may call `FirebaseProvider.logEvent(...)` for analytics), `:data-services:google-auth` and `:data-services:apple-auth` (consumed directly by `:ui-screen-features:authorization` to fetch ID tokens). Anything domain-shaped still goes through `:data-features:feature-api`.
- **`:data-features:feature-api` imports `:data-services:*`.** Pure contracts.
- **`:toolkit:*` imports `:design-system:*`.** Toolkit is below. Sole exception: `:toolkit:date-utils` reads locale-aware format tokens from `:design-system:resources:provider` and `:design-system:core` — see the platform-build-toolkit skill, references/module-structure.md § directional dependency rules for the canonical list of tolerated exceptions.
- **`:design-system:*` imports `:data-features:*`.** Design is pure UI.
- **`:data-mappers:*` import each other.** Each direction is isolated.
- **Two features sharing the same module.** One module per feature.
- **Two `Database` classes.** One per app.
- **Multiple `Koin.init` calls.** One per app.

If you ever feel the need to break one of these rules, **stop and ask** — there's almost always an existing pattern that handles the case you're trying to solve. See `when-to-stop-and-ask.md`.
