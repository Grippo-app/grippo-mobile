# Forbidden patterns (anti-patterns reference)

Apply these cross-cut prohibitions to the actual source/diff. Owning skills define
implementation and semantic gates; [code style](code-style.md) owns the positive
style rules; [CLAUDE.md](../../../../CLAUDE.md) owns scope and locked decisions.
An existing deviation in the code never authorizes copying it.

## Kotlin structure

- **Private receiver extension helpers or unrelated utilities embedded in another
  owner's file.** Apply [helper ownership](code-style.md#visibility-and-files)
  and the shared model stub convention it links.
- **Named local functions** declared inside `init`, constructors, another function
  or a lambda. Declare methods at class scope or functions at file scope. Ordinary
  callbacks, lambdas and DSLs remain allowed; do not confuse them with declarations.
- **Compressed business/control blocks**, including one-line `safeLaunch` or
  `trackOperation` bodies. Keep them multiline; short single-expression `update`
  and simple Flow operators stay compact. See [Kotlin blocks](code-style.md#kotlin-blocks).
- **ViewModel layout drift** - constructor groups out of order, a statement in
  `init` outside the permitted list, a mapper/use-case/feature call inside an
  `update` lambda, an `update` lambda that computes values instead of copying
  precomputed locals (unless the result depends on the current state), or a
  `provide*` handler with more than one `update`.
  See [ViewModel layout](code-style.md#viewmodel-layout).
- **Duplicating a startup request to keep it literally in `init`** when the same
  operation must also run after error-dialog dismissal. Use one private
  class-level method called from `init` and `onError`; no local function, new
  scope or retry loop.

## Coroutines

- **`delay`/sleep to pace a business operation** (for example a fixed pause inside
  a tracked save). Business blocks complete as fast as their work allows; UX
  timing belongs to presentation.

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
  in a ViewModel** (`try/catch`, `runCatching`, result failure branches). Call throwing
  commands inside `safeLaunch`, including private suspend helpers.
  Request isolation, recovery wrappers and stage flags follow
  [the error pipeline](../../ui-feature/references/error-pipeline.md).
- **`async { }.await()`** to start a single piece of async work. Use `safeLaunch { ... }`. `async` is for parallel forks.

## Collections in state

- **`List<T>`, `Set<T>`, `Map<T>`** (Kotlin defaults). Not Compose-stable.
- **`mutableListOf()`, `mutableStateListOf()`, `mutableMapOf()`** in state. Mutable types break immutability discipline.
- **`buildList { ... }`** if the result is stored in state without `.toImmutableList()`. *(Reviewer-only.)*

## Compose

Apply [shared Compose rules](../../compose-components/references/structure-and-runtime.md),
[feature composition rules](../../ui-feature/references/compose-rules.md) and
[design-system rules](../../design-system/SKILL.md) for their owned prohibitions.
A ViewModel never calls composables; follow [base-class boundaries](../../ui-feature/references/base-classes.md).

## Data layer

- **`Flow<Result<T>>`** introduced for ordinary observation. Preserve existing Flow contracts; upstream exceptions follow the shared error pipeline. Command contracts follow [operation outcomes](../../data-layer/references/operation-outcomes.md#throwing-suspend-contracts-canonical).
- **Returning DTOs from a Repository.** Map to domain.
- **Inline mappers in Repository / ViewModel.** Use `:data-mappers:*`.
- **`!!` on a DTO field.** Forbidden; use `?: return null` (via `AppLogger.Mapping.log`).
- **Non-null DTO fields or non-null collection defaults**, in responses AND request bodies. Every DTO field is nullable with `= null`; request construction still supplies API-required values, and a body field that must be sent as explicit `null` carries `@EncodeDefault(EncodeDefault.Mode.ALWAYS)`. See [DTO/API contracts](../../data-layer/references/dtos-and-api.md).
- **Required entity fields nullable.** Guard required values during mapping;
  semantically optional Entity/Domain fields remain nullable. Apply
  [target null semantics](../../mappers/references/null-safety-and-logging.md).
- **Raw `String` for a closed-set field in a *domain* model.** A field from a backend dictionary (type, status, kind, category, …) is a domain `<X>Enum` (pure labels — `key` + `companion object of(key)`) or a `sealed interface <X>` (variants carry payload — base `val key` + per-variant `data class`), not a bare `String`. The raw `String` stays only in the DTO / Entity replica; the `…→domain` mapper promotes it via `<X>Enum.of(value)`. Same stance for timestamps (`LocalDateTime`, not `String`) and durations (`Duration`, not `Long`). See [DTO/API contracts](../../data-layer/references/dtos-and-api.md) and [mappers](../../mappers/SKILL.md).
- **Auto-generated numeric entity IDs replacing established identity.** Preserve server-provided IDs for synced records and business-owned local IDs for drafts; mappers do not generate identity.
- **Swallowing repository exceptions or cancellation.** Apply [throwing suspend contracts](../../data-layer/references/operation-outcomes.md#throwing-suspend-contracts-canonical); local catches, Result commands and generic recovery wrappers in this chain are forbidden.
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
- **A Koin-injectable type as a `*Component` constructor parameter.** Feature
  interfaces, UseCases, `DialogController`, repositories, managers, auth/link
  providers and analytics services resolve with `getKoin().get()` inside
  `componentContext.retainedInstance { <X>ViewModel(...) }`. A Component
  constructor takes only the context, navigation callbacks and ordinary initial
  business inputs from a route or accepted parent flow.
- **Feature-owned analytics lifecycle wiring** - a provider/route matrix, a manual
  `bind` or a lifecycle service resolved in the feature. Analytics stays in the
  ViewModel; foundation binds the screen hook. See
  [feature analytics](../../feature-analytics/SKILL.md).

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
  request isolation follows the [error pipeline](../../ui-feature/references/error-pipeline.md#requests-driven-by-a-long-lived-source), with no new scopes, recovery wrappers or stage flags.
  Business commands throw directly. A required platform credential Result MUST
  be unwrapped with `getOrThrow()` inside the owned error pipeline or propagated
  to a caller that handles it; failure MUST NOT be silently discarded.
- **Impure `safeLaunch.mapError`** or replacing original errors before analytics,
  logging or crash reporting. Mapping only supplies a standard dialog representation
  for known exceptions; return unknown errors unchanged and preserve cancellation/fatal errors.
- **Using `safeLaunch.onError` as an immediate handler.** It runs after the error
  dialog is dismissed, not when the exception is thrown, and does not suppress the dialog.
- **Custom error dialogs** outside `DialogConfig.ErrorDisplay`.
- **Recording exceptions to Firebase manually.** Pipeline does it.
- **Required credential Result converted to fallback success** through `getOrNull()`,
  `getOrElse` or an early return, even when the fallback value is handled. Apply the
  [error pipeline](../../ui-feature/references/error-pipeline.md); preserve the original failure.
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
- **Raw `String` where an existing `*FormatState` fits the input** (the 15 types
  in `ui-core/state/.../formatters/`: Email, Password, Name, Date, DateTime,
  DateRange, Duration, Weight, Height, Volume, Repetitions, Intensity, Density,
  Multiplier, Percentage). Reuse its validation/display contract. A plain query
  may stay `String` only when no formatter fits; a new formatter needs a real
  unmet need, not a class per field.
- **`var` in state classes.** All `val`.
- **Mutable collections in state.** Immutable only.
- **State that duplicates a sub-state**'s field. Derive locally.
- **Shadow ViewModel stores or derivable State fields** — apply
  [minimal runtime state](../../ui-feature/references/state.md#minimal-runtime-state).
- **Redundant domain constructor fields** — apply
  [minimal domain models](../../data-layer/references/repositories.md#minimal-domain-models).
- **Flat scalar pile.** 15+ primitive fields, or booleans encoding one lifecycle (`isRunning` + `hasCompletedTest` + `activePhase`). Group clusters into sub-states, fixed sets into enums, the lifecycle into a sealed interface. See the ui-feature skill, references/state.md § "No flat scalar piles". Reviewer-enforced.

## Build

- **Inline version strings** (`"2.3.21"`). Use the catalog.
- **`apply(plugin = "...")`** instead of `plugins { id("...") }`.
- **`repositories { ... }` in module scripts.** Forbidden by `FAIL_ON_PROJECT_REPOS`.
- **`compileSdk`, `minSdk`, `targetSdk`, JVM toolchain or namespace in a module script**. The conventions own them (compileSdk 37, minSdk 26, targetSdk 36, JVM 19); only `:androidApp` adds its `applicationId`.
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

- **Test sources, linters, screenshot tooling or other verification infrastructure
  introduced without an explicit user request** (see the
  [verification policy](../../../../CLAUDE.md#verification-policy)).
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

Comments that narrate obvious code, embed task identifiers, or present an
unfinished `TODO`/`NotImplementedError` path as done. The positive rule is in
[code style](code-style.md#comments).

## Architecture-shape

- **UI module imports `:data-services:*` directly.** Use `:data-features:feature-api`. Sole tolerated exception: `:data-services:google-auth` and `:data-services:apple-auth`, consumed by `:ui-screen-features:authorization` to obtain provider credentials before a domain user exists. Anything domain-shaped goes through `:data-features:feature-api`.
- **Feature UI or domain code calling Firebase or building toolkit wire events.**
  UI depends on `:analytics:analytics-api` and emits typed feature facts from the
  ViewModel; only `:analytics:<feature>` implementations build toolkit payloads.
- **Analytics implementation importing UI, domain, services, SDKs or toolkit core**, exposing public implementation types, or using manual production DI.
- **Analytics-only state**: retention wrappers, extra jobs or retries, duplicate
  leaf/container tracking, a screen enum entry without an owning ViewModel, a
  missing or wrong `screenPresentation` on an independent screen.
- **Telemetry altering business outcomes**: a reporting error replacing a business
  result, required work moved outside the decorated block, a throwing use case
  reshaped to fit the decorator, or host/config plumbing inferring dialog outcomes.
  The owning rules are in [feature analytics](../../feature-analytics/SKILL.md).
- **`:data-features:feature-api` imports `:data-services:*`.** Pure contracts.
- **`:toolkit:*` imports `:design-system:*` or `:ui-core:*`.** Toolkit is below both. The two documented exceptions, not to be extended by analogy: `:toolkit:date-utils` -> `:design-system:core` + `:design-system:resources:provider` (locale-aware format tokens) and `:toolkit:http-client` -> `:ui-core:error:error-provider` (`AppError`). See [toolkit ownership](../../platform-build-toolkit/references/toolkit-utilities-core.md) and [dependency rules](../../../../CLAUDE.md#module-dependency-rules).
- **`:design-system:*` imports `:data-features:*`.** Design is pure UI.
- **`:data-mappers:*` import each other.** Each direction is isolated.
- **New unrelated features mixed into one module** without an authorized architecture decision. Extend an existing feature in its owning module.
- **Two `Database` classes.** One per app.
- **Multiple `Koin.init` calls.** One per app.

A change to a locked rule needs an explicit scope decision. Existing authorization remains valid; use [scope questions](when-to-stop-and-ask.md) for new decisions.
