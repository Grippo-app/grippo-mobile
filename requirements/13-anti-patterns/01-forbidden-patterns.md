# Forbidden Patterns

Things you must not do — refuse to write them, refuse to commit them. Listed roughly by layer.

## Coroutines

- **`viewModelScope.launch`, `lifecycleScope.launch`, `GlobalScope.launch`, `CoroutineScope(...).launch`** inside a `BaseViewModel`. Only `safeLaunch` / `Flow.safeLaunch()`.
- **`runBlocking { ... }`** anywhere in production. Tests only.
- **Manual `CoroutineExceptionHandler`** inside a VM. The pipeline handles errors.
- **`try { ... } catch (e: Throwable) { ... }`** inside a VM (except `Result.onSuccess { ... }` / `runCatching { ... }` at domain boundaries). Let errors flow through `safeLaunch` → `ErrorProvider`.
- **`async { }.await()`** to start a single piece of async work. Use `safeLaunch { ... }`. `async` is for parallel forks.

## Collections in state

- **`List<T>`, `Set<T>`, `Map<T>`** (Kotlin defaults). Not Compose-stable.
- **`mutableListOf()`, `mutableStateListOf()`, `mutableMapOf()`** in state. Mutable types break immutability discipline.
- **`buildList { ... }`** if the result is stored in state without `.toImmutableList()`.

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

## Data layer

- **`Flow<Result<T>>`** for observation. Observations always succeed; `Result` is for mutations.
- **Returning DTOs from a Repository.** Map to domain.
- **Inline mappers in Repository / ViewModel.** Use `:data-mappers:*`.
- **`!!` on a DTO field.** Forbidden; use `?: return null` (via `AppLogger.Mapping.log`).
- **Non-null DTO fields.** All DTO fields are nullable + default `= null` (defensive).
- **Required entity fields nullable.** Entities are post-validation; nullable belongs in DTOs.
- **`@PrimaryKey(autoGenerate = true) val id: Long`** in entities. IDs come from the server.
- **Catching exceptions in a Repository** silently. `runCatching { ... }` returns `Result`; let the caller handle.
- **Writing to a DAO before `Result.onSuccess { ... }`**. Speculative writes leave the cache inconsistent on failure.
- **Skipping range reconciliation** (`deleteByCreatedAtRangeExceptIds`). "Deleted on another device" rows linger.
- **Bypassing `BackendClient`** with a raw `HttpClient.request { ... }`. All HTTP goes through `GrippoApi → BackendClient`.
- **Direct `Context` import in `commonMain`**. Use `NativeContext` from `:toolkit:context`.
- **Subgrouping `GrippoApi`** into `AuthApi`, `TrainingsApi`, etc. Flat is intentional.

## Dependency Injection

- **Hand-written `module { single { ... } }`** Koin DSL for new code. Use annotations (`@Single`, `@Factory`, `@Module @ComponentScan`).
- **`@Single` without `binds = [Interface::class]`** when registering an impl for an interface. Consumers can't `inject<Interface>()`.
- **Missing module in `:shared/Koin.kt`.** Runtime "no definition found".
- **`getKoin().get()` in a Composable.** Wrong layer.
- **Module hand-DSL outside tests.**
- **`@Factory` for stateless services.** Use `@Single`.

## Navigation

- **Compose Navigation alongside Decompose.** One nav library only.
- **`LaunchedEffect(Unit) { navigate(...) }`.** Direction + eventListener.
- **Routes carrying lambdas.** Not serializable; crash on backgrounding.
- **Routes without `@Serializable`.** Same.
- **Mutable routes** (with `var` fields). Routes are `data class` / `data object`.
- **Cross-feature import**: `:ui-screen-features:home` importing from `:ui-screen-features:profile`. Use `:screen-api`.

## Errors

- **Manual `try/catch` in VM** to hide errors. Use the pipeline.
- **Custom error dialogs** outside `DialogConfig.ErrorDisplay`.
- **Recording exceptions to Firebase manually.** Pipeline does it.
- **`Result.getOrNull()` without handling `null`.** Silently drops failures.
- **Catching `CancellationException`.** Must propagate.
- **Throwing raw `Throwable` from `validateResponse`.** Throw `AppError` subtypes.

## State

- **`String` for localizable values.** Use `UiText`.
- **`String` for form fields.** Use `*FormatState`.
- **`var` in state classes.** All `val`.
- **Mutable collections in state.** Immutable only.
- **State that duplicates a sub-state**'s field. Derive locally.

## Build

- **Inline version strings** (`"2.3.21"`). Use the catalog.
- **`apply(plugin = "...")`** instead of `plugins { id("...") }`.
- **`repositories { ... }` in module scripts.** Forbidden by `FAIL_ON_PROJECT_REPOS`.
- **`compileSdk = 36` in a module**. Already in the convention.
- **`@OptIn(...)` for globally-opted-in experimentals**. Already in `KotlinMultiplatformConventionPlugin`.
- **`api(...)` everywhere.** Default is `implementation`.
- **`mavenLocal()`** in production builds.
- **Multiple `Json` instances** with different configs. One `:toolkit:serialization` singleton.

## Logging

- **`println(...)`** in production. Use `AppLogger`.
- **`android.util.Log.*`** in `commonMain`. Doesn't compile.
- **Logging PII** (full tokens, emails). Truncate or hash.
- **Calling `AppLogger.General.error(...)` after a Ktor exception.** Pipeline already logs.
- **Skipping `AppLogger.Mapping.log(value) { msg }`** in DTO → Entity / Domain mappers. Required.

## Testing

- **Production code that requires test-only setup.** Architecture should test-friendly without backflips.
- **Tests in modules without an opt-in.** This project doesn't write tests by default.

## Resources

- **Hardcoded strings in Composables.** Extract.
- **Missing keys in a locale's `strings.xml`.** Translator might miss; runtime falls back to English.
- **PNG icons for vector candidates.** Use SVG/vector.
- **Resource files in feature modules.** Centralize in `:design-system:resources:provider`.
- **`@JvmStatic`** in `commonMain`. JVM-only.

## Architecture-shape

- **UI module imports `:data-services:*` directly.** Use `:data-features:feature-api`. Narrow exceptions tolerated for SDK-style services that have no `:data-features:*` wrapper: `:data-services:firebase` (a UI VM may call `FirebaseProvider.logEvent(...)` for analytics), `:data-services:google-auth` and `:data-services:apple-auth` (consumed directly by `:ui-screen-features:authorization` to fetch ID tokens). Anything domain-shaped still goes through `:data-features:feature-api`.
- **`:data-features:feature-api` imports `:data-services:*`.** Pure contracts.
- **`:toolkit:*` imports `:design-system:*`.** Toolkit is below. Sole exception: `:toolkit:date-utils` reads locale-aware format tokens from `:design-system:resources:provider` and `:design-system:core` — see `02-module-structure/02-dependency-rules.md` for the canonical list of tolerated exceptions.
- **`:design-system:*` imports `:data-features:*`.** Design is pure UI.
- **`:data-mappers:*` import each other.** Each direction is isolated.
- **Two features sharing the same module.** One module per feature.
- **Two `Database` classes.** One per app.
- **Multiple `Koin.init` calls.** One per app.

If you ever feel the need to break one of these rules, **stop and ask** — there's almost always an existing pattern that handles the case you're trying to solve. See `13-anti-patterns/02-when-to-stop-and-ask.md`.
