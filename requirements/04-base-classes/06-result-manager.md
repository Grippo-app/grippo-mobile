# `ResultManager` and `ResultEmitter`

`ResultManager` + `ResultEmitter` implement the cross-component result mechanism. See `03-architecture-patterns/04-cross-component-results.md` for **when** to use them. This document specifies **how** they work.

## `ResultEmitter` (singleton)

```kotlin
@Single
internal class ResultEmitter {
    private val resultChannel = Channel<Pair<String, BaseResult>>(capacity = Channel.BUFFERED)

    val results: Flow<Pair<String, BaseResult>> = resultChannel.receiveAsFlow()

    fun <T : Any> sendResult(key: ResultKey<Result<T>>, data: T) {
        resultChannel.trySend(key.key to Result(data))
    }

    fun clear() {
        resultChannel.close()
    }
}
```

- **`@Single`** — one instance per app, scoped by Koin.
- `resultChannel: Channel<Pair<String, BaseResult>>` with `Channel.BUFFERED` capacity (~64 slots).
- `results: Flow` via `receiveAsFlow()` — **single-consumer**. Multiple subscribers compete via FIFO.
- `sendResult` uses `trySend` — non-blocking. Returns `ChannelResult` but we ignore it; if the buffer is full (64 unread events), the event is dropped.

## `ResultManager` (per-component)

```kotlin
@Factory
internal class ResultManager(
    private val resultEmitter: ResultEmitter,
    @InjectedParam val coroutineScope: CoroutineScope,
) {
    private val activeSubscriptions = mutableListOf<Job>()

    fun <T : BaseResult> observeResult(
        key: ResultKey<T>,
        onResult: suspend (T) -> Unit,
    ) {
        val job = resultEmitter.results
            .filter { (resultKey, _) -> resultKey == key.key }
            .onEach { (_, result) -> @Suppress("UNCHECKED_CAST") onResult(result as T) }
            .launchIn(coroutineScope)
        activeSubscriptions.add(job)
    }

    fun <T : Any> sendResult(key: ResultKey<Result<T>>, data: T) {
        resultEmitter.sendResult(key, data)
    }

    fun clear() {
        activeSubscriptions.forEach { it.cancel() }
        activeSubscriptions.clear()
    }
}
```

- **`@Factory`** — one instance per Component (each Component gets its own `coroutineScope` via `@InjectedParam`).
- `activeSubscriptions: MutableList<Job>` tracks all subscriptions so `clear()` can cancel them on component destroy.
- `observeResult`:
  - Filters the emitter's flow by key match.
  - Casts the matched `BaseResult` to `T` (the type is enforced at the call site by `ResultKey<T>`).
  - Launches the collection on `coroutineScope`.
  - Adds the `Job` to the active list.
- `sendResult`: thin pass-through to the emitter.
- `clear()`: cancels all active subscriptions. Called from `BaseComponent.lifecycle.doOnDestroy`.

## `BaseComponent` integration

```kotlin
private val resultManager: ResultManager by inject { parametersOf(coroutineScope) }

init {
    lifecycle.doOnDestroy {
        // ...
        resultManager.clear()
        coroutineScope.cancel()
    }
}

protected fun <T : BaseResult> observeResult(key: ResultKey<T>, onResult: suspend (T) -> Unit) {
    resultManager.observeResult(key, onResult)
}

protected fun <T : Any> sendResult(key: ResultKey<Result<T>>, data: T) {
    resultManager.sendResult(key, data)
}
```

VM authors interact via `BaseComponent.observeResult / sendResult` (or directly via `ResultManager` if needed inside the VM).

## Channel semantics — important caveats

### Single-consumer

```kotlin
val results: Flow<Pair<String, BaseResult>> = resultChannel.receiveAsFlow()
```

`receiveAsFlow()` produces a **cold flow over a hot channel**. If two collectors run concurrently, they compete on FIFO: each event goes to exactly one collector.

**Practical implication:** if two `observeResult(keyA, ...)` subscriptions run for the same key in the same Component (or across the live Component tree), each event goes to **one** subscriber, the other gets nothing. Worse, if subscriber A filters for `keyA` and subscriber B filters for `keyB`, **and an event for `keyB` arrives while A is the "current" recipient at the channel head**, A drops it (filter mismatch) and the event is gone — B never sees it.

**Rule:** at most one `observeResult` per `ResultKey` across the live Component tree at any given time. The intended pattern is **one subscription per Component per protocol**, with `when` dispatch on sealed subtypes (see `03-architecture-patterns/04-cross-component-results.md`).

### Buffered

```kotlin
private val resultChannel = Channel<Pair<String, BaseResult>>(capacity = Channel.BUFFERED)
```

`Channel.BUFFERED` is platform-dependent but typically 64 slots. Bursts of `sendResult` don't block. After 64 unread events, `trySend` drops new events silently.

In practice, bursts of 64+ events without a consumer mean something is wrong (consumer not subscribed). Don't rely on the buffer; subscribe before triggering the producer.

### No replay

A subscriber that starts after the event was sent does **not** see it. The Channel is not a SharedFlow.

**Pattern:** the consumer Component's `init { observeResult(...) }` runs **before** the consumer triggers the producer (typically via navigation push that creates the producer Component). The order:

1. Consumer Component is created.
2. Consumer's `init` calls `observeResult(key) { ... }`.
3. Consumer triggers something that navigates to the Producer Component.
4. Producer Component eventually calls `sendResult(key, data)`.
5. Producer pops; Consumer receives the result.

If steps 2 and 4 swap (Producer sends before Consumer subscribes), the event is lost.

## Why not `SharedFlow(replay = 1)`?

`SharedFlow` supports multiple collectors and replay, but has different semantics:

- **Replay can confuse**: a Consumer Component that's recreated after process death would replay the last event, even though the action was already handled.
- **Multi-collector** is unwanted: two subscribers on the same key both seeing the same event would cause double-processing (e.g. "delete training" handled twice).

The single-consumer Channel is intentional. The cost is the discipline of "one subscription per key" — enforced by convention, not by the runtime.

## Anti-patterns

- **Two `observeResult` for the same key in one Component** — channel FIFO eats one of them.
- **Sending a result during component init** — consumer's subscription hasn't started yet; event lost.
- **Using `ResultManager` instead of `DialogConfig` callback** for picker-style flows — over-engineering.
- **Storing the `ResultKey` as a `var`** — keys are constants; declare them as `val` or `companion object` properties.
- **Sending a non-`@Serializable` payload** — payloads don't survive process death; if the producer sends and dies before the consumer pops back to subscribe, the event is lost anyway. (This is fine in practice — `ResultManager` is for live flows, not durable messaging.)

## Tying it back to the architecture

`ResultManager` is the **escape hatch** when `DialogConfig` callbacks and constructor lambdas don't reach. Most features never need it. When they do, it's because:

- Two screens in unrelated feature modules exchange data through the root navigator's pop semantics.
- A confirmation dialog opens a date picker, which opens a time picker, and the time picker's result must reach the confirmation initiator.

If a feature is using `ResultManager` for plain pickers, refactor to use `DialogConfig.onResult` instead.
