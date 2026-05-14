# `OperationManager`

`OperationManager` is the internal coroutine launcher used by `BaseViewModel`. It is **not** a public API — VM authors interact with `safeLaunch` / `Flow.safeLaunch`. But understanding its role helps debug edge cases.

## Interface

```kotlin
internal interface OperationManager {
    fun launch(
        dispatcher: CoroutineDispatcher,
        onError: suspend (Throwable) -> Unit,
        block: suspend CoroutineScope.() -> Unit,
    ): Job

    fun <T> whileActive(
        upstream: Flow<T>,
        activation: Flow<Boolean>,
    ): Flow<T>
}
```

## Implementation

```kotlin
@Factory(binds = [OperationManager::class])
internal class OperationManagerImpl(
    @InjectedParam val coroutineScope: CoroutineScope,
) : OperationManager {

    override fun launch(
        dispatcher: CoroutineDispatcher,
        onError: suspend (Throwable) -> Unit,
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        val handler = CoroutineExceptionHandler { _, t ->
            if (t !is CancellationException) coroutineScope.launch { onError(t) }
        }
        return coroutineScope.launch(
            dispatcher + handler + SupervisorJob(coroutineScope.coroutineContext[Job]),
        ) {
            supervisorScope { block() }
        }
    }

    override fun <T> whileActive(
        upstream: Flow<T>,
        activation: Flow<Boolean>,
    ): Flow<T> = activation
        .flatMapLatest { active ->
            if (active) flowOf(true) else flow { delay(1.seconds); emit(false) }
        }
        .distinctUntilChanged()
        .flatMapLatest { active -> if (active) upstream else emptyFlow() }
}
```

## Why `@Factory` with `@InjectedParam`

Each `BaseViewModel` instance has its own `coroutineScope`. To bind that scope to the `OperationManager`, `OperationManager` is `@Factory`-scoped (a new instance per `get()` call) and takes the scope via `@InjectedParam`:

```kotlin
// BaseViewModel
private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

private val operationManager by inject<OperationManager> {
    parametersOf(coroutineScope)
}
```

`parametersOf(coroutineScope)` is the Koin idiom for passing dynamic parameters at `get()` time. Inside `OperationManagerImpl`, `@InjectedParam` receives the scope.

## `launch` semantics

```kotlin
override fun launch(
    dispatcher: CoroutineDispatcher,
    onError: suspend (Throwable) -> Unit,
    block: suspend CoroutineScope.() -> Unit,
): Job
```

- Launches `block` on `coroutineScope` with:
  - The caller's `dispatcher`.
  - A `CoroutineExceptionHandler` that calls `onError(t)` for any non-`CancellationException`.
  - A `SupervisorJob` child of the scope's existing `Job` — failures don't cancel siblings.
- Wraps the block in `supervisorScope { block() }` for further sibling isolation.

`BaseViewModel.safeLaunch` calls this, passing an `onError` lambda that hooks into the error pipeline (`AppLogger`, Crashlytics, `ErrorProvider`).

## `whileActive` semantics

```kotlin
override fun <T> whileActive(
    upstream: Flow<T>,
    activation: Flow<Boolean>,
): Flow<T> =
    activation
        .flatMapLatest { active ->
            if (active) flowOf(true) else flow { delay(1.seconds); emit(false) }
        }
        .distinctUntilChanged()
        .flatMapLatest { active -> if (active) upstream else emptyFlow() }
```

- `activation: Flow<Boolean>` emits `true` when the host component is RESUMED, `false` otherwise (provided by `BaseComponent`).
- When the host leaves RESUMED, the inner `flatMapLatest` starts a `delay(1.seconds)` + emit-false. If the host returns to RESUMED within 1 second, the delay is cancelled (due to `flatMapLatest`'s "cancel previous" semantics) and the `true` re-emits — the upstream stays subscribed.
- After 1 second of off-screen time, `false` propagates and the upstream is dropped (`emptyFlow()`).
- When RESUMED again, `true` triggers a fresh subscription to `upstream`.

This **debounced unsubscription** avoids:
- Bursting subscribe/unsubscribe on quick orientation changes or transient backgrounding.
- Wasting CPU on long-running streams while the screen is genuinely off-screen.

`BaseViewModel`'s `Flow.safeLaunch(processing = Processing.WhileActive)` uses this. `Processing.Infinity` bypasses it.

## CoroutineExceptionHandler details

```kotlin
val handler = CoroutineExceptionHandler { _, t ->
    if (t !is CancellationException) coroutineScope.launch { onError(t) }
}
```

- **`CancellationException` is ignored.** Cancellations are normal coroutine teardown, not errors.
- For any other throwable: launches `onError(t)` on the same scope. The `onError` lambda is the pipeline glue (`AppLogger.General.error`, Crashlytics, `ErrorProvider.provide`).

## Koin registration

`OperationManager` and its impl live in `:ui-core:foundation`. The Koin module:

```kotlin
@Module
@ComponentScan
public class CoreModule
```

`@ComponentScan` discovers `OperationManagerImpl` via its `@Factory(binds = [OperationManager::class])` annotation. `CoreModule` is included in `:shared/Koin.kt`'s `modules(...)`.

## What VM authors care about

- Errors thrown inside `safeLaunch { }` reach the dialog automatically — no manual catch.
- `Flow.safeLaunch(processing = Processing.WhileActive)` pauses upstream when the screen is off-screen for ≥ 1 second.
- `Processing.Infinity` should be reserved for work that must complete regardless of UI state (form submission, background sync started by user).
- Multiple `safeLaunch { }` blocks in one VM run in parallel; each has its own job, none cancels the others if one fails.

## What `OperationManager` does NOT do

- It does not throttle, debounce, or rate-limit calls. Doing so per-VM is the VM's responsibility (`debounce`, `sample`, etc. on flows).
- It does not retry failed calls. Retries happen one level deeper (e.g. `TokenProvider.retryWithBackoff`).
- It does not log success. Only failures.
