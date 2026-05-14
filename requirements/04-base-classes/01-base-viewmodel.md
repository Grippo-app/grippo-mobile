# `BaseViewModel`

`BaseViewModel` is the **only** ViewModel base class in the project. Every screen and dialog's ViewModel extends it. The class lives in `:ui-core:foundation` and provides:

- A `StateFlow<STATE>` and the only way to update it (`update { ... }`).
- A `StateFlow<ImmutableSet<LOADER>>` of currently active operations.
- A conflated `Flow<DIRECTION>` for navigation intents (`navigateTo(...)`).
- The only coroutine launcher (`safeLaunch`) — automatically routes uncaught exceptions through the error pipeline.
- Lifecycle-aware Flow subscription (`Flow<T>.safeLaunch()`) that pauses upstream when the screen is not RESUMED.

Manual `viewModelScope.launch`, `runBlocking`, `GlobalScope`, and raw `CoroutineScope` are **forbidden** — see `13-anti-patterns/01-forbidden-patterns.md`.

## Class signature

```kotlin
public abstract class BaseViewModel<STATE, DIRECTION : BaseDirection, LOADER : BaseLoader>(
    state: STATE,
) : InstanceKeeper.Instance, KoinComponent
```

- `STATE`, `DIRECTION`, `LOADER` are generics constrained to the marker interfaces.
- Initial state is passed via the constructor.
- Implements `InstanceKeeper.Instance` so Decompose retains the instance across configuration changes; `onDestroy()` is called when the host component is destroyed.
- Implements `KoinComponent` so the VM can `by inject<...>()` services not threaded through the constructor (the project uses constructor injection by default; field injection is used internally for `OperationManager` and `ErrorProvider`). `FirebaseProvider` is referenced as a static `object` (not Koin-injected). `ResultManager` is injected by `BaseComponent`, not by `BaseViewModel`.

## Public / protected API

```kotlin
// STATE
public val state: StateFlow<STATE>
protected fun update(updateFunc: (STATE) -> STATE)

// LOADERS
public val loaders: StateFlow<ImmutableSet<LOADER>>
protected suspend fun <T> withLoader(loader: LOADER?, block: suspend () -> T): T

// NAVIGATION
public val navigator: Flow<DIRECTION>
protected fun navigateTo(destination: DIRECTION)

// COROUTINE LAUNCHERS
protected enum class Processing { WhileActive, Infinity }

protected fun safeLaunch(
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    processing: Processing = Processing.Infinity,
    loader: LOADER? = null,
    onError: (() -> Unit) = {},
    block: suspend CoroutineScope.() -> Unit,
): Job

protected fun <T> Flow<T>.safeLaunch(
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    processing: Processing = Processing.WhileActive,
    onError: (() -> Unit) = {},
): Job

// ACTIVATION (called by BaseComponent automatically)
internal fun attachActivation(activationFlow: Flow<Boolean>)
internal fun detachActivation()

// LIFECYCLE
override fun onDestroy()
```

## Internal wiring

```kotlin
private val coroutineScope: CoroutineScope = CoroutineScope(
    context = SupervisorJob() + Dispatchers.Main.immediate,
)

private val operationManager by inject<OperationManager> {
    parametersOf(coroutineScope)
}
```

`OperationManager` is `@Factory(binds = [OperationManager::class])` with an `@InjectedParam coroutineScope: CoroutineScope`. Each VM instance gets its own `OperationManager` bound to its own scope; the scope is cancelled in `onDestroy()`.

## STATE — `state` and `update`

```kotlin
public val state: StateFlow<STATE>
protected fun update(updateFunc: (STATE) -> STATE)
```

- `state` is read-only outside the VM.
- `update { it.copy(...) }` is the **only** way to mutate. The implementation does `(state as MutableStateFlow).update(updateFunc)`.
- `update { }` is **thread-safe** (atomic compare-and-swap inside `MutableStateFlow.update`). Multiple coroutines may call it concurrently.
- **Never** capture `state.value` before a slow operation and then `update { it.copy(... = capturedValue + ...) }` — a concurrent update will be lost. Inside `update { ... }` the lambda receives the **current** value.

## LOADERS — `loaders` and `withLoader`

```kotlin
public val loaders: StateFlow<ImmutableSet<LOADER>>
protected suspend fun <T> withLoader(loader: LOADER?, block: suspend () -> T): T
```

- `loaders` is an `ImmutableSet` — multiple loaders can be active concurrently.
- `withLoader(MyLoader.X) { block() }`:
  - Adds `MyLoader.X` to the set.
  - Runs `block()`.
  - Removes `MyLoader.X` on completion (success **or** failure, via `try/finally`).
- If `loader == null`, the block runs without modifying the set.
- Useful inside `mapLatest`/`flatMapLatest` chains where individual flows want loader behavior without spinning up a new `safeLaunch`.

## NAVIGATION — `navigator` and `navigateTo`

```kotlin
public val navigator: Flow<DIRECTION>
protected fun navigateTo(destination: DIRECTION)
```

- Internal implementation: `Channel<DIRECTION>(Channel.CONFLATED)`.
- **`navigator` is conflated.** Rapid-fire `navigateTo()` calls may collapse — if the user double-taps a button that fires two `navigateTo(Direction.OpenX)` calls, only one Open may be delivered. This is **intentional** — debouncing built into navigation. If you need every event delivered, use `ResultManager` (separate channel) or design the Direction so the second event is a no-op.

## COROUTINE LAUNCHERS — `safeLaunch`

### `safeLaunch { ... }`

```kotlin
protected fun safeLaunch(
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    processing: Processing = Processing.Infinity,
    loader: LOADER? = null,
    onError: (() -> Unit) = {},
    block: suspend CoroutineScope.() -> Unit,
): Job
```

- Launches `block` on the VM's `coroutineScope` with `dispatcher` + `SupervisorJob` + a `CoroutineExceptionHandler`.
- If `loader != null`, the loader is added to `loaders` before launch and removed via `job.invokeOnCompletion { ... }` (success, failure, or cancellation). The `withLoader` helper is **not** used internally by `safeLaunch`; it's a separate building block for use inside `mapLatest`/`flatMapLatest` chains.
- If `block` throws (non-`CancellationException`): `AppLogger.General.error` + `FirebaseProvider.recordException` + `ErrorProvider.provide(...)` + invoke `onError`.
- `Processing.Infinity` (default for `safeLaunch { }`): the block runs to completion regardless of host lifecycle.
- `Processing.WhileActive`: see below.

### `Flow<T>.safeLaunch()`

```kotlin
protected fun <T> Flow<T>.safeLaunch(
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    processing: Processing = Processing.WhileActive,
    onError: (() -> Unit) = {},
): Job
```

- Subscribes to the flow on the VM's scope.
- Default `processing = WhileActive`: the upstream flow is **paused for 1 second** after the host leaves RESUMED state, then unsubscribed if still inactive. Re-subscribed when RESUMED again. This avoids waste while the screen is off-screen but still responds to short backgrounding without a full re-subscribe.
- `Processing.Infinity`: the flow keeps emitting even when the screen is off — useful for streams that must run while a workout is in progress, etc.

## Processing modes

```kotlin
protected enum class Processing { WhileActive, Infinity }
```

| Mode | When to use |
|---|---|
| `Infinity` | Critical work that must complete: form submission, save, sign-up flow. Default for `safeLaunch { }`. |
| `WhileActive` | Streams that should pause when off-screen: list observers, real-time stats. Default for `Flow.safeLaunch()`. |

The 1-second debounce on `WhileActive` is implemented by `OperationManager.whileActive(upstream, activation)`:

```kotlin
override fun <T> whileActive(upstream: Flow<T>, activation: Flow<Boolean>): Flow<T> =
    activation
        .flatMapLatest { active ->
            if (active) flowOf(true) else flow { delay(1.seconds); emit(false) }
        }
        .distinctUntilChanged()
        .flatMapLatest { active -> if (active) upstream else emptyFlow() }
```

`activation` is provided by `BaseComponent` via `attachActivation(lifecycle.asActiveFlow())` in `init`.

## Dispatchers

| Dispatcher | When |
|---|---|
| `Dispatchers.Main.immediate` (default) | State updates, navigation, dialog show, light glue. |
| `Dispatchers.IO` | Explicit heavy IO inside the VM (rare — IO already lives in Repository/`BackendClient.invoke`). |
| `Dispatchers.Default` | CPU-heavy work (sorting/mapping large collections). |

Don't switch dispatchers casually. `Main.immediate` is fine for most VM work because the heavy lifting is in Repository/API/Mappers.

## Error pipeline

See `03-architecture-patterns/07-error-pipeline.md`. The summary:

```
throw inside safeLaunch
  → OperationManager's CoroutineExceptionHandler catches
  → AppLogger.General.error(...)
  → FirebaseProvider.recordException(...)
  → ErrorProvider.provide(exception, callback = onError)
  → DialogController.show(DialogConfig.ErrorDisplay(state, onClose = onError))
```

The VM author writes `feature.x().getOrThrow()` — the rest is automatic.

## `onDestroy` (lifecycle)

```kotlin
override fun onDestroy() {
    _navigator.close()
    coroutineScope.cancel()
}
```

Called by Decompose when the host Component is destroyed. Closes the navigator channel (so any in-flight collectors complete cleanly) and cancels the VM's `coroutineScope` so all in-flight work is dropped. Subscriptions to `Flow`s via `Flow.safeLaunch()` are cancelled automatically because their `Job` is on the cancelled scope.

## What MUST NOT be in a ViewModel

- **`@Composable` functions.** VM does not know Compose.
- **Direct access to `Context`/`Activity`.** Use `NativeContext` from `:toolkit:context` if a platform handle is truly needed (very rare).
- **Direct API/DAO calls.** Only `<X>Feature` / `<X>UseCase` from `:data-features:feature-api`.
- **Manual `try/catch`** (except for explicit domain logic on `Result.onSuccess` / `runCatching`).
- **`runBlocking`** anywhere.
- **Compose state holders.** No `mutableStateOf`, no `mutableStateListOf` — use `update { ... }` on the StateFlow.

## Minimal VM template

```kotlin
internal class FooViewModel(
    private val barFeature: BarFeature,
) : BaseViewModel<FooState, FooDirection, FooLoader>(FooState.Empty), FooContract {

    init {
        barFeature.observeBars()
            .map { it.toState() }
            .onEach { update { state -> state.copy(items = it) } }
            .safeLaunch()
    }

    override fun onRefreshClick() {
        safeLaunch(loader = FooLoader.Refresh) {
            barFeature.getBars().getOrThrow()
        }
    }

    override fun onItemClick(id: String) {
        navigateTo(FooDirection.OpenDetail(id))
    }

    override fun onBack() {
        navigateTo(FooDirection.Back)
    }
}
```
