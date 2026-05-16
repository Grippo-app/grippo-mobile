# Cross-Component Results (`ResultManager`)

> **Illustrative domain.** Code blocks below use `Note` / `Tag` / `User` as the generic `<Entity>` / `<RelatedEntity>` for examples. Substitute identifiers from your product domain.

Use **callbacks in `DialogConfig`** for almost all dialog-driven results. Reach for `ResultManager` only when a callback cannot be threaded:

- Two screens in different `:ui-screen-features:*` modules need to exchange data, and there's no obvious place to put the lambda.
- An initiator opens a dialog which opens another dialog; the second dialog's result must reach the initiator.
- The producer and consumer are split by lifecycle (one component is destroyed while the other waits).

## API

In `:ui-core:foundation`:

```kotlin
public interface BaseResult

public data class Result<T : Any>(public val data: T) : BaseResult

public data class ResultKey<T : BaseResult>(public val key: String) {
    override fun toString(): String = key
}

public object ResultKeys {
    public fun <T : BaseResult> create(key: String): ResultKey<T> = ResultKey(key)
}
```

`ResultEmitter` (singleton, `@Single`):

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

`ResultManager` (per-component, `@Factory`):

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

`BaseComponent` exposes the methods to subclasses:

```kotlin
public abstract class BaseComponent<DIRECTION : BaseDirection>(...) {
    private val resultManager: ResultManager by inject { parametersOf(coroutineScope) }

    protected fun <T : BaseResult> observeResult(
        key: ResultKey<T>,
        onResult: suspend (T) -> Unit,
    ) {
        resultManager.observeResult(key, onResult)
    }

    protected fun <T : Any> sendResult(key: ResultKey<Result<T>>, data: T) {
        resultManager.sendResult(key, data)
    }
}
```

## Channel semantics

- **Single-consumer.** `ResultEmitter` uses a single `Channel<...>`, not a `SharedFlow`. If two components subscribe to the same `ResultKey`, they **compete** on FIFO — only one receives each event.
  - **Rule:** at most one `observeResult` per `ResultKey` across the live component tree at any given time.
- **Buffered.** `Channel.BUFFERED` (~64 slots). Bursts of `sendResult` from a producer don't block.
- **No replay.** A subscription started after the event was sent will **not** see it. Initiator must subscribe before triggering the producer.
- **Filtered by key.** Non-matching events are silently dropped — but because the channel is single-consumer, the matched event is consumed by the **first** subscriber in FIFO order. Multiple subscribers on **different** keys are safe.

## Action types

Result protocol types live nested inside the **producer screen's** `*Router.<Screen>.Action`:

```kotlin
// :ui-screen-features:screen-api/NotesRouter.kt
@Serializable
public sealed class NotesRouter : BaseRouter {

    @Serializable
    public data class Note(val note: NoteState) : NotesRouter() {

        public sealed interface Action {
            public data class Sync(val note: NoteState) : Action
            public data class Remove(val id: String) : Action
        }
    }

    @Serializable public data class Completed(/* ... */) : NotesRouter()
}
```

The `Action` type is co-located with the route that produces it. Both producer and consumer reference `NotesRouter.Note.Action.Sync(...)` / `Action.Remove(...)` — the contract is in one place.

Note: `Action` is **not** a `BaseResult`. `sendResult<T : Any>(key, data: T)` wraps the payload in `Result<T>` (which is itself the `BaseResult`), so the consumer subscribes to `Result<NotesRouter.Note.Action>` — see `observeResult` example below. Keeping `Action` as a plain sealed interface lets `sendResult` infer `T` directly.

## Subscribing

**One `observeResult` per Component per domain.** Because the channel is single-consumer, two subscriptions to different keys inside one component are fine; two subscriptions to the **same** key inside one component compete.

```kotlin
internal class NotesRootComponent(
    componentContext: ComponentContext,
    ...
) : BaseComponent<NotesDirection>(componentContext) {

    init {
        observeResult<Result<NotesRouter.Note.Action>>(
            key = ResultKeys.create("note"),
            onResult = { result ->
                when (val action = result.data) {
                    is NotesRouter.Note.Action.Sync -> viewModel.updateNote(action.note)
                    is NotesRouter.Note.Action.Remove -> viewModel.removeNote(action.id)
                }
            }
        )
    }
    // ...
}
```

A single subscription with `when` dispatch on the sealed subtypes is the idiomatic shape. **Don't** add a second `observeResult` for `Action.Remove` — Channel FIFO will eat one of the two subscriptions' events at random.

## Producing

```kotlin
internal class NoteEditorViewModel(...) : BaseViewModel<...>(...), ... {

    private val resultManager: ResultManager by inject { parametersOf(coroutineScope) }

    fun onSaveClick() {
        val note = buildNote()
        resultManager.sendResult(
            key = ResultKeys.create("note"),
            data = NotesRouter.Note.Action.Sync(note),
        )
        navigateTo(NoteEditorDirection.Back)
    }
}
```

## Lifecycle

`ResultManager.clear()` is called in `BaseComponent.lifecycle.doOnDestroy` (handled automatically). Subscriptions auto-cancel on component destroy. Producers can call `sendResult` from any lifecycle; consumers must be alive at send time.

## When to use ResultManager vs callback

| Situation | Use |
|---|---|
| Dialog returns a value to the screen that opened it | **Callback in `DialogConfig`** |
| Picker (amount, date, ...) | **Callback in `DialogConfig`** |
| Confirmation modal returning yes/no | **Callback in `DialogConfig`** |
| Screen A in `:ui-screen-features:note-detail` produces a value for Screen B in `:ui-screen-features:notes` (different modules, no shared call site) | **`ResultManager`** |
| Multi-step flow: A opens dialog X, X opens dialog Y, Y produces value for A | **`ResultManager`** with key namespaced by the initiator |
| A screen is destroyed and recreated before the result arrives | **`ResultManager`** (callback would be GC'd) |

## Anti-patterns

- **Two `observeResult` calls for the same key in one Component.** Channel is FIFO single-consumer; results will be silently lost.
- **Reusing a `ResultKey` across unrelated flows.** Keys are global; collisions cause cross-talk.
- **Sending a result before any consumer subscribes.** No replay; the event is lost.
- **Threading data through `ResultManager` when a callback would work.** Verbose, harder to trace, and adds a singleton dependency.
- **Using `ResultManager` for navigation.** Navigation goes via `Direction`. Results carry data only.
