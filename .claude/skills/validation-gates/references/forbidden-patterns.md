# Forbidden patterns (anti-patterns reference)

Apply these cross-cut constraints to actual source/diff. Owning skills define the
implementation and semantic gates; [CLAUDE.md](../../../../CLAUDE.md) owns project
scope and locked decisions. Existing deviations do not authorize copying them.

## Kotlin structure

- **Named local functions** declared inside `init`, constructors, another function
  or a lambda. Declare methods at class scope or functions at file scope. Ordinary
  callbacks, lambdas and DSLs remain allowed; do not confuse them with declarations.
- **Compressed business/control blocks**, including one-line `safeLaunch` or
  `trackOperation` bodies. Keep them multiline. Short single-expression `update`
  and simple Flow transformations stay compact; expand long/multi-operation blocks.
  Follow [the project Kotlin style](../../../../CLAUDE.md#kotlin).
- **Duplicating startup requests solely to keep them literally in `init`** when
  the same complete operation must run after error-dialog dismissal. Use one
  private class-level operation called from `init` and `onError`, in the existing
  VM scope. Do not introduce a local function, new scope or retry loop.

## Coroutines

- **Mechanical cancellation checks around already cooperative suspend calls.**
  Use owned scopes and cancellable APIs; preserve `CancellationException`.
  Explicit `ensureActive()` checks MAY be used when necessary for noncooperative
  CPU work or an SDK/foundation boundary. Review the actual cancellation gap and
  place checks where they prevent continued work or acceptance of a late result;
  do not add them merely to satisfy a template or for telemetry. Equivalent Job
  checks follow the same rule. MUST NOT remove necessary existing checks, turn
  cancellation into success/fallback or weaken the observable error contract.
- **`viewModelScope.launch`, `lifecycleScope.launch`, `GlobalScope.launch`, `CoroutineScope(...).launch`** inside a `BaseViewModel`. Only `safeLaunch` / `Flow.safeLaunch()`.
- **`runBlocking { ... }`** anywhere in production.
- **Manual `CoroutineExceptionHandler`** inside a VM. The pipeline handles errors.
- **Local exception handling that hides, replaces or presents unhandled failures
  in a ViewModel** (`try/catch`, `runCatching`, result failure branches). Unwrap
  with `getOrThrow()` inside `safeLaunch`, including private suspend helpers.
  Do not add extra launches, recovery wrappers or stage flags; see
  [the error pipeline](../../ui-feature/references/error-pipeline.md).
- **`async { }.await()`** to start a single piece of async work. Use `safeLaunch { ... }`. `async` is for parallel forks.

## Collections in state

- **`List<T>`, `Set<T>`, `Map<T>`** (Kotlin defaults). Not Compose-stable.
- **`mutableListOf()`, `mutableStateListOf()`, `mutableMapOf()`** in state. Mutable types break immutability discipline.
- **`buildList { ... }`** if the result is stored in state without `.toImmutableList()`. *(Reviewer-only.)*

## Compose

- **`LaunchedEffect(Unit) { navigateTo(...) }`** for navigation. Use `Direction` + `eventListener`.
- **`mutableStateOf(...)` for logical state**. Logical state goes in the ViewModel; `mutableStateOf` is for local UI animations.
- **`stringResource(R.string.foo)`** from `androidx.compose.ui.res`. Android-only — breaks iOS build.
- **`painterResource(R.drawable.foo)`** from `androidx.compose.ui.res`. Same.
- **Product color/dimension/typography literals in consumers.** Use AppTokens.
  Neutral technical constants (`0.dp`, `Color.Transparent`) and explicit
  representative preview fixtures are permitted; MUST NOT become substitute
  product styling. Apply [Compose rules](../../ui-feature/references/compose-rules.md#anti-patterns-must-avoid).
- **Product `TextStyle(fontSize = ...)` inline** in feature code. Use `AppTokens.typography.<token>()`; the explicit preview-fixture exception above still applies.
- **`MaterialTheme.colorScheme.*`** in feature code. Use `AppTokens.colors.*`.
- **`androidx.compose.material3.Button`** in feature code. Use `:design-system:components/Button`.
- **`@Composable` calls from a `BaseViewModel`.** VMs don't know Compose.
- **Flat-scalar explosion of a domain entity in a composable signature.** A composable that renders an entity takes its `*State` model as one parameter, not `name: String, surname: String, avatarUrl: String, …` mirroring the model. Exception: domain-agnostic primitives in `:design-system:components` / `:compose-libs:*` (Button, Chip) stay slot/enum-driven. See the ui-feature skill, references/compose-rules.md § "Entity composables". Not mechanically greppable — reviewer-enforced.
- **Non-`*Screen` UI placed flat beside the seven MVI files**, or inlined into a fat `*Screen.kt`, instead of the sub-screen's `components/` subfolder (one cohesive composable per file). See the ui-feature skill, references/module-structure.md § "File layout for a feature". Reviewer-enforced.

## Data layer

- **`Flow<Result<T>>`** introduced for ordinary observation. Preserve existing Flow contracts; upstream exceptions follow the shared error pipeline. Result-returning commands must propagate cancellation.
- **Returning DTOs from a Repository.** Map to domain.
- **Inline mappers in Repository / ViewModel.** Use `:data-mappers:*`.
- **`!!` on a DTO field.** Forbidden; use `?: return null` (via `AppLogger.Mapping.log`).
- **Non-null DTO fields or non-null collection defaults**, in responses AND request bodies. Every DTO field is nullable with `= null`; request construction still supplies API-required values. See the data-layer DTO reference.
- **Required entity fields nullable.** Guard required values during mapping;
  semantically optional Entity/Domain fields remain nullable. Apply
  [target null semantics](../../mappers/references/null-safety-and-logging.md).
- **Raw `String` for a closed-set field in a *domain* model.** A field from a backend dictionary (type, status, kind, category, …) is a domain `<X>Enum` (pure labels — `key` + `companion object of(key)`) or a `sealed interface <X>` (variants carry payload — base `val key` + per-variant `data class`), not a bare `String`. The raw `String` stays only in the DTO / Entity replica; the `…→domain` mapper promotes it via `<X>Enum.of(value)`. Same stance for timestamps (`LocalDateTime`, not `String`) and durations (`Duration`, not `Long`). See [DTO/API contracts](../../data-layer/references/dtos-and-api.md) and [mappers](../../mappers/SKILL.md).
- **Auto-generated numeric entity IDs replacing established identity.** Preserve server-provided IDs for synced records and business-owned local IDs for drafts; mappers do not generate identity.
- **Swallowing repository exceptions or cancellation.** Preserve existing Result/throwing contracts; at a suspend Result boundary rethrow CancellationException rather than wrapping it as failure.
- **Mutating an API-backed cache before the request succeeds.** Local authoritative drafts/preferences write through their own repository contract; they do not require an unrelated network response.
- **Skipping range reconciliation** (`deleteByCreatedAtRangeExceptIds`). "Deleted on another device" rows linger.
- **Bypassing BackendClient for Grippo backend requests.** Use GrippoApi → BackendClient. Existing platform credential-provider OAuth exchange retains its approved transport boundary; it does not authorize a second backend client.
- **Direct `Context` import in `commonMain`**. Use `NativeContext` from `:toolkit:context`.
- **Subgrouping `GrippoApi`** into `AuthApi`, `NotesApi`, etc. Flat is intentional. *(Reviewer-only.)*

## Dependency Injection

- **Hand-written `module { single { ... } }`** for a new DI module. Use annotations; preserve the existing FeatureApiModule carve-out.
- **`@Single` without `binds = [Interface::class]`** when registering an impl for an interface. Consumers can't `inject<Interface>()`.
- **Missing module in `:shared/Koin.kt`.** Runtime "no definition found".
- **`getKoin().get()` in a Composable.** Wrong layer.
- **A second hand-written DI module.** Existing FeatureApiModule UseCase registration is the sole carve-out; authorized UseCase additions may extend it.
- **Unjustified DI lifetime.** Select @Single/@Factory from actual ownership; stateless shared analytics is @Single, per-operation managers retain their existing @Factory lifetime.
- **A Koin-injectable type as a `*Component` constructor parameter.** Feature interfaces, UseCases, `DialogController`, repositories, managers, auth/link providers and analytics services resolve via `getKoin().get()` inside `componentContext.retainedInstance { <X>ViewModel(...) }`, not through Component constructors. Components receive context, navigation callbacks and ordinary initial business inputs from a route or accepted parent flow. Analytics stays in VM, with lifecycle binding only in foundation; no feature provider/route matrix or manual bind. See [feature analytics](../../feature-analytics/SKILL.md) and the ui-feature base-classes reference.

## Navigation

Screen-result rules are defined in the ui-feature skill,
[results.md](../../ui-feature/references/results.md). Review these explicitly:

- **Hardcoded receiver key inside a returning screen.** Receiver companion owns
  the definition; producer receives explicit `resultKey` through its route.
- **Back in an observer to close another screen**, or parent close-and-resend
  relays for standard screen returns. Producer uses `sendResult → back`; updates
  that keep the screen open only send data.
- **Runtime selection/navigation guards** such as `selectionSent`, `resultHandled`
  or `isClosing`, including equivalent flags hidden in Jobs, sets or wrappers.
- **Claiming a navigation race without evidence**, or claiming conflated navigation
  provides strict debounce/exactly-once behavior. Check actual callback chains and
  queued/immediate dispatch for result-driven navigation.


- **Compose Navigation alongside Decompose.** One nav library only. *(Reviewer-only.)*
- **`LaunchedEffect(Unit) { navigate(...) }`.** Direction + eventListener.
- **Routes carrying lambdas.** Not serializable; crash on backgrounding.
- **Routes without `@Serializable`.** Same.
- **Mutable routes** (with `var` fields). Routes are `data class` / `data object`. *(Reviewer-only.)*
- **Cross-feature import**: `:ui-screen-features:home` importing from `:ui-screen-features:profile`. Use `:ui-screen-features:screen-api`.

## Errors

- **Local `try/catch`, `runCatching`, `Result.onFailure`, `recover`, `fold` or
  `getOrElse` that hide, replace or present unhandled failures in a ViewModel**,
  including private helpers and extensions. Use `safeLaunch` → `ErrorProvider`.
  Private suspend helpers propagate original errors through the same pipeline;
  no extra launches, scopes, recovery wrappers or stage flags.
  `Result.onSuccess` alone does not propagate failure. The required Result MUST
  be unwrapped with `getOrThrow()` inside the owned error pipeline or propagated
  to a caller that handles it; failure MUST NOT be silently discarded.
- **Impure `safeLaunch.mapError`** or replacing original errors before analytics,
  logging or crash reporting. Mapping only supplies a standard dialog representation
  for known exceptions; return unknown errors unchanged and preserve cancellation/fatal errors.
- **Using `safeLaunch.onError` as an immediate handler.** It runs after the error
  dialog is dismissed, not when the exception is thrown, and does not suppress the dialog.
- **Custom error dialogs** outside `DialogConfig.ErrorDisplay`.
- **Recording exceptions to Firebase manually.** Pipeline does it.
- **`Result.getOrNull()` without handling `null`.** Silently drops failures.
- **Consuming `CancellationException`.** Must propagate; shared error dispatch excludes it.
- **Throwing raw `Throwable` from `validateResponse`.** Throw `AppError` subtypes. *(Reviewer-only.)*

## State

- **Private ViewModel runtime flags, previous-source caches or shadow snapshots**
  for initialization, restoration, edit detection or synchronization, including
  `restorePending`, `previousSource` and `previousSourceSelection`. Equivalent
  flags/copies hidden in State, wrappers, providers or mutable closures are also
  forbidden. Keep actual feature values in State/update and use existing initial
  inputs for one-time population; see
  [the draft policy](../../state-preservation/SKILL.md#data-backed-form-synchronization).
- **`String` for localizable values.** Use `UiText`.
- **Raw `String` where an existing suitable `*FormatState` supports the input**
  (email, password, name, text, amount or date). MUST reuse its validation/display
  contract. A plain query may remain String only when no existing formatter fits
  its behavior. A new formatter requires a real unmet need, not a new class per field.
- **`var` in state classes.** All `val`.
- **Mutable collections in state.** Immutable only.
- **State that duplicates a sub-state**'s field. Derive locally.
- **Flat scalar pile.** 15+ primitive fields, or booleans encoding one lifecycle (`isRunning` + `hasCompletedTest` + `activePhase`). Group clusters into sub-states, fixed sets into enums, the lifecycle into a sealed interface. See the ui-feature skill, references/state.md § "No flat scalar piles". Reviewer-enforced.

## Build

- **Inline version strings** (`"2.3.21"`). Use the catalog.
- **`apply(plugin = "...")`** instead of `plugins { id("...") }`.
- **`repositories { ... }` in module scripts.** Forbidden by `FAIL_ON_PROJECT_REPOS`.
- **`compileSdk = 36` in a module**. Already in the convention.
- **`@OptIn(...)` for globally-opted-in experimentals**. Already in `KotlinMultiplatformConventionPlugin`.
- **`api(...)` everywhere.** Default is `implementation`.
- **`mavenLocal()`** in production builds.
- **Multiple `Json` instances** with different configs. One `:toolkit:serialization` singleton. *(Reviewer-only.)*

## Logging

- **`println(...)` outside the logging sink implementation** in production.
  Consumers MUST use `AppLogger`; its existing stdout sink may use println.
- **`android.util.Log.*`** in `commonMain`. Doesn't compile.
- **Calling `AppLogger.General.error(...)` after a Ktor exception.** Pipeline already logs.
- **Skipping `AppLogger.Mapping.log(value) { msg }`** in DTO → Entity / Domain mappers. Required.

## Verification infrastructure

- **Verification that violates the canonical
  [project verification policy](../../../../CLAUDE.md#verification-policy)**, including
  test sources/infrastructure introduced without an explicit user request.
- **Verification-only production hooks or global fake modes.** Keep constructor
  injection and module boundaries clean; do not add flags, singleton resets or
  reflection scaffolding for checks.
- **Claiming runtime behavior from source checks.** Record what ran and what remains
  unverified, including native lifecycle, SDK delivery and generated DI.

## Resources

- **Hardcoded user-facing product strings in Composables.** Extract to resources; explicitly marked preview-fixture text is permitted.
- **Missing keys in a locale's `strings.xml`.** Translator might miss; runtime falls back to English.
- **Raster substitutes for UI icons.** Use the existing AppIcon ImageVector pattern. *(Reviewer-only.)*
- **Resource files in feature modules.** Centralize in `:design-system:resources:provider`.
- **`@JvmStatic`** in `commonMain`. JVM-only. *(Reviewer-only.)*

## Icons and API usage

- **Inline placeholder ImageVector path data** in components/previews. Real UI
  icons live in AppIcon; use an existing real icon or implement the authorized
  resource before its consumer. Raster imagery defaults to WebP, with a justified
  PNG exception; raster UI-icon substitutes are not a shortcut.
- **New calls to deprecated APIs** when the pinned stack has a supported suitable
  alternative. Verify against the catalog and actual sources, rather than a
  remembered library version. Existing cookbook drift must be corrected, not copied.
- **Deprecated-library claims without evidence.** Ordinary Flow.collect lambdas
  are not categorically forbidden. ViewModel subscriptions still use Flow.safeLaunch;
  foundation/platform internals retain their owned coroutine design.
- **Channel for continuous UI/data observation** when an existing Flow provides
  it. Foundation's conflated navigation channel is a locked one-shot signal policy.
- **New catalog/dependency versions or @OptIn configuration** silently introduced
  during feature work. Changes must be within the authorized build/platform scope.

## Comments

Do not narrate obvious code, embed transient quiz/task IDs or claim incomplete
TODO/NotImplementedError production paths as finished work. Keep only comments
that explain a real invariant or non-obvious decision. Record verification and
tradeoffs in the task report; no fixed report format or artificial line cap applies.

## Architecture-shape

- **UI module imports `:data-services:*` directly.** Use `:data-features:feature-api`. Narrow exceptions tolerated for SDK-style services that have no `:data-features:*` wrapper: `:data-services:google-auth` and `:data-services:apple-auth` (consumed directly by `:ui-screen-features:authorization` to fetch ID tokens). Anything domain-shaped still goes through `:data-features:feature-api`.
- **Feature UI/domain calls Firebase or constructs toolkit wire events directly.**
  Apply [feature-analytics](../../feature-analytics/SKILL.md): UI main depends on
  analytics-api and constructs typed feature facts for trackEvent; only the
  implementation constructs toolkit AnalyticsEvent payloads and owns matrices.
  Reject UI/domain/services/SDK imports in analytics implementation, public
  implementation types, manual production DI, analytics state/retention wrappers, duplicate
  leaf/container tracking and extra jobs/retries solely for instrumentation.
  Review actual eligible VM callbacks and every independent screen's required hook
  against actual routes/rendered surfaces and coverage; containers must keep null.
  Reject missing/wrong hooks, ownerless screen enum entries and screen-hook omissions
  under the canonical mandatory screen coverage gate. Preserve the
  whole business return/error contract. Result-returning calls unwrap inside the
  decorated block; direct-value throwing calls stay direct. Reject reporting errors
  replacing business results and shared host/config plumbing inferring dialog outcomes.
- **`:data-features:feature-api` imports `:data-services:*`.** Pure contracts.
- **`:toolkit:*` imports `:design-system:*`.** Toolkit is below. Sole exception: `:toolkit:date-utils` reads locale-aware format tokens from `:design-system:resources:provider` and `:design-system:core` — inspect actual [toolkit ownership](../../platform-build-toolkit/references/toolkit-utilities-core.md), build files and [project dependency rules](../../../../CLAUDE.md#module-dependency-rules).
- **`:design-system:*` imports `:data-features:*`.** Design is pure UI.
- **`:data-mappers:*` import each other.** Each direction is isolated.
- **New unrelated features mixed into one module** without an authorized architecture decision. Extend an existing feature in its owning module.
- **Two `Database` classes.** One per app.
- **Multiple `Koin.init` calls.** One per app.

A change to a locked rule needs an explicit scope decision. Existing authorization remains valid; use [scope questions](when-to-stop-and-ask.md) for new decisions.
