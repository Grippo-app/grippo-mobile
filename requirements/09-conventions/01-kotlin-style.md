# Kotlin Style

Code style rules that apply across every module.

## `explicitApi()` is enabled

Every top-level declaration **must** have an explicit visibility modifier (`public`, `internal`, `private`). The Kotlin compiler enforces it. There is no implicit-public.

```kotlin
// ✅ correct
public fun foo() { ... }
internal fun bar() { ... }
private fun baz() { ... }

// ❌ compile error: explicit visibility required
fun foo() { ... }
```

### Visibility defaults by location

| Location | Default visibility |
|---|---|
| `:data-features:feature-api` types | `public` |
| `:data-features:<feature>` impls | `internal` (only `<X>FeatureModule` is `public`) |
| `:data-services:*` types | `internal` (only entry-points like `GrippoApi`, `Database` are `public`) |
| `:ui-screen-features:*`, `:ui-dialog-features:*` | `internal` (only public Routers in `:screen-api` / `:dialog-api`) |
| `:design-system:*` | `public` (consumed everywhere) |
| `:toolkit:*` | `public` for the interface; `internal` for impl |
| `:data-mappers:*` | `public` top-level functions |

## File and class naming

- Files: **PascalCase** matching the primary class/interface (`TrainingRepository.kt`, `BaseViewModel.kt`).
- Composables (functions): **PascalCase** (`ProfileBodyScreen`, `WeightPickerComponent`).
- Other functions: **camelCase** (`getCurrentToken`, `toEntity`).
- Properties: **camelCase** (`accessToken`, `isOnline`).
- Classes/interfaces/objects: **PascalCase** (`TrainingsRepository`, `AppTokens`).
- Constants: **SCREAMING_SNAKE_CASE** inside `companion object` (`const val MAX_RETRIES = 3`).
- Sealed type subtypes: **PascalCase** (`AppError.Network.NoInternet`).

## File layout

One file = one top-level class/interface/object. Exception: tightly-related sealed-type families (e.g. `TrainingResponse` + `ExerciseResponse` + `IterationResponse` in one file) — only when they're conceptually inseparable.

```kotlin
package com.<org>.<product>.trainings

import ...

@Immutable
internal data class TrainingsListState(
    val items: ImmutableList<Training> = persistentListOf(),
    val filter: TrainingFilter = TrainingFilter.All,
)
```

State classes use **default constructor values** for the initial state; the ViewModel constructs them with `TrainingsListState()`. A `companion object Empty` is the **Contract** convention (see `02-naming.md`), not the State convention.

## Imports

- **No wildcard imports.** `import com.foo.bar.*` is forbidden. Use specific imports.
- **No unused imports.** IDE auto-clean keeps these out.
- **Order**: Kotlin/Java stdlib → third-party (alphabetical) → first-party (`com.<org>.<product>.*`).

## Coroutines

- **Only `safeLaunch` / `Flow.safeLaunch()`** inside a ViewModel. `viewModelScope.launch`, `GlobalScope`, `runBlocking`, raw `CoroutineScope(...).launch` are forbidden.
- **`Dispatchers.Main.immediate`** is the default. Use `Dispatchers.IO` only when explicitly needed (Heavy IO inside the VM; usually IO already runs in `BackendClient`).
- **`Dispatchers.Default`** for CPU-bound work (sorting large lists, complex computation).
- **`withContext(Dispatchers.IO)`** in services that touch I/O (e.g. `BackendClient.invoke`).
- **`runCatching` for Repository methods**. `runBlocking { ... }` for tests only (never production).

## Flows

- **`Flow<T>` for observation**; `StateFlow<T>` for state holders.
- **`combine(flowA, flowB) { a, b -> ... }`** for joining streams. Never `zip` (which waits for both — wrong semantics).
- **`distinctUntilChanged()`** before emitting to state to avoid recomposition cascades.
- **`map { }.distinctUntilChanged()`** in that order — map first, then dedupe.
- **`flatMapLatest`** for "cancel previous on new emission".
- **`combine` / `merge` / `flatMap*`** all return cold flows; collect via `.safeLaunch()` in `init { }`.

## Collections

- **Immutable in state**: `ImmutableList<T>`, `ImmutableSet<T>`, `PersistentList<T>` from `kotlinx-collections-immutable`. Mutable collections (`List`, `Set`, `Map` — Kotlin's default) are **not Compose-stable**.
- **`emptyList()`** for non-state defaults; **`persistentListOf()`** in state.
- **Builder pattern**: `persistentListOf(a, b, c).add(d)` returns a new list; not in-place.
- **`buildList { ... }`** for one-off construction; convert to `ImmutableList` if stored in state.

## Nullability

- **Avoid `!!`.** Use `?: error("reason")` or `?: return null` or `requireNotNull(value) { "msg" }`.
- **Use `?:` for fallbacks.** `volume ?: 0f`.
- **Use `?.let { ... }`** for null-safe transformations.
- **`runCatching { ... }`** for Result types. `try/catch` only in narrow domain-recovery cases.

## Equality

- **`==`** for value equality (`==` calls `.equals()`).
- **`===`** for reference equality (rarely needed).
- **`data class`** gives free `equals`/`hashCode`/`copy`.

## When to use `data class`

Use `data class` for:
- DTOs.
- Entities.
- Domain models.
- States, Loaders' params (with `@Immutable`).
- Configurations.

Don't use `data class` for:
- Services with behavior (`@Single` services are regular classes).
- ViewModels (they're not data — they're behavior).
- Classes that aren't meaningfully comparable.

## When to use `data object` vs `object`

- **`data object`** for empty sealed-type cases (`Back`, `Close`, `Done`). Generates a nice `toString()` and `equals()`.
- **`object`** for stateful singletons that have behavior (`AppLogger`, `DateTimeUtils`, `AppTokens`).

## Long expressions

- **Break at logical points**: chain calls each on a new line; multi-line `if/when` over multi-line one-liners.
- **Trailing commas** in multi-line argument lists and parameter declarations.

```kotlin
fun foo(
    a: String,
    b: Int,
    c: List<Foo>,
): Bar = TODO()

bar.combine(other) { a, b ->
    a + b
}.distinctUntilChanged()
    .onEach { update { state -> state.copy(value = it) } }
    .safeLaunch()
```

## Comments

- **Default: no comments.** Names and structure tell the story.
- **One-liner WHY comment** for non-obvious decisions: a workaround, a known limitation, a TODO with context.
- **KDoc on `public` types** if the type's purpose isn't obvious from the name. Don't write KDoc just to repeat the name (`/** A button. */ class Button`).
- **No multi-paragraph KDoc.** Be terse.

## Visibility rules per layer

| Layer | Public types | Internal types |
|---|---|---|
| `:design-system:*` | Tokens, Composables, `AppTheme` | Color/typography/dp impls (`DarkColor`, `*ProviderImpl`) under `internal/` |
| `:ui-core:foundation` | `Base*`, `ResultKey`, helpers | `OperationManagerImpl`, `ResultManager`, `ResultEmitter` |
| `:ui-screen-features:screen-api` | `*Router` sealed classes, `Deeplink` | (none — pure API module) |
| `:ui-screen-features:<feature>` | `<Feature>RootComponent`, top-level routes | All sub-screens (`Component`, `Screen`, etc.) |
| `:data-features:feature-api` | `<X>Feature`, domain models, `<X>UseCase` | (none) |
| `:data-features:<feature>` | `<X>FeatureModule` | `<X>RepositoryImpl`, `<X>FeatureImpl` |
| `:data-services:*` | Entry types (`GrippoApi`, `Database`, `DataStore`), Koin modules | All impls (`BackendClient`, `TokenProvider`, DAOs) |
| `:toolkit:*` | Interfaces and entry points | All impls |
| `:data-mappers:*` | Top-level extension functions | (none) |

## `@Suppress`

- **Avoid `@Suppress` at file scope** — narrow it to the specific declaration.
- **Common acceptable suppresses**:
  - `@Suppress("UNCHECKED_CAST")` after an explicit type check.
  - `@Suppress("NO_ACTUAL_FOR_EXPECT")` on KSP-generated `expect/actual` (e.g. `DatabaseConstructor`).
  - `@Suppress("UnstableApiUsage")` in `settings.gradle.kts` for `dependencyResolutionManagement`.
- **Document why** in a one-line comment above the suppress.

## Anti-patterns

- **`@JvmStatic`** in commonMain (Kotlin/Native doesn't have JVM).
- **`@JvmField`** in commonMain.
- **`lateinit var`** for non-test fields. Inject via constructor or use `by lazy { ... }`.
- **`var`** in `@Immutable` data classes. State is immutable.
- **Single-letter parameter names** outside lambdas (`x`, `y`, `i`). Even `it` in long lambdas should be renamed for clarity.
- **`return@somelambda`** without a clear named target. Use an explicit `if (...) return@onEach`.
- **Catching `Throwable` broadly** instead of specific types. Exception: `runCatching { ... }` at API boundaries.
- **`fun <T> Any?.cast(): T = this as T`** helpers. Use explicit `as` with `@Suppress("UNCHECKED_CAST")`.
