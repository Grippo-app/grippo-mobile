# Cross-Component Results (`ResultManager`) & ViewModel data flow

> Examples use `Note` / `Tag` as the generic `<Entity>`. Substitute your domain.

Use **callbacks in `DialogConfig`** for almost all dialog-driven results. Reach
for `ResultManager` only when a callback cannot be threaded:
- Two screens in different `:ui-screen-features:*` modules exchange data with no obvious lambda site.
- An initiator opens a dialog which opens another dialog; the second result must reach the initiator.
- Producer and consumer are split by lifecycle (one component destroyed while the other waits).

## API (in `:ui-core:foundation`)

```kotlin
public interface BaseResult
public data class Result<T : Any>(public val data: T) : BaseResult
public data class ResultKey<T : BaseResult>(public val key: String) { override fun toString() = key }
public object ResultKeys { public fun <T : BaseResult> create(key: String): ResultKey<T> = ResultKey(key) }
```

`ResultEmitter` (singleton, `@Single`) — a single buffered `Channel<Pair<String, BaseResult>>(Channel.BUFFERED)`, exposed as `results: Flow` via `receiveAsFlow()`; `sendResult` uses `trySend` (non-blocking; full buffer drops the event); `clear()` closes the channel.

`ResultManager` (per-component, `@Factory`, `@InjectedParam coroutineScope`) — `observeResult(key, onResult)` filters the emitter flow by key, casts the matched `BaseResult` to `T`, launches on `coroutineScope`, tracks the `Job`; `sendResult` passes through to the emitter; `clear()` cancels all tracked subscriptions.

`BaseComponent` exposes `protected fun observeResult(...)` / `sendResult(...)` to subclasses; `ResultManager` is `internal` and feature code never injects it. A VM that needs to publish a result emits a navigation `Direction`; its Component sends from `eventListener`.

## Channel semantics (MUST understand)

- **Single-consumer.** `ResultEmitter` uses a single `Channel`, not a `SharedFlow`. If two components subscribe to the same `ResultKey`, they **compete** on FIFO — only one receives each event.
  - **Rule: at most one `observeResult` per `ResultKey` across the live component tree at any given time.**
- **Buffered.** `Channel.BUFFERED` (~64 slots). Bursts don't block; after 64 unread events `trySend` drops new events silently.
- **No replay.** A subscription started after the event was sent will **not** see it. The initiator must subscribe before triggering the producer.
- **Filtered by key.** Non-matching events are silently dropped — but because the channel is single-consumer, the matched event is consumed by the **first** FIFO subscriber. Multiple subscribers on **different** keys are safe. Worse case: if A filters `keyA`, B filters `keyB`, and a `keyB` event arrives while A is the current channel head, A drops it (filter mismatch) and B never sees it.

`SharedFlow(replay=1)` is intentionally **not** used: replay would confuse a process-death-recreated consumer, and multi-collector would double-process. The single-consumer Channel is the design; the cost is the "one subscription per key" discipline (by convention, not runtime).

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

`Action` is co-located with the route that produces it. It is **not** a `BaseResult`: `sendResult<T : Any>(key, data: T)` wraps the payload in `Result<T>` (the `BaseResult`); the consumer subscribes to `Result<NotesRouter.Note.Action>`. Keeping `Action` as a plain sealed interface lets `sendResult` infer `T` directly. Primitive payloads use the same shape (`sendResult(key, data = 5)` / `observeResult<Result<Int>>`).

## Subscribing

**One `observeResult` per Component per protocol**, with `when` dispatch on sealed subtypes:

```kotlin
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
```

**Don't** add a second `observeResult` for `Action.Remove` — Channel FIFO will eat one of the two subscriptions' events at random. The consumer's `init { observeResult(...) }` must run **before** the consumer triggers the producer (no replay).

## Producing

The producer is a **Component**, never a ViewModel. The VM emits a navigation `Direction` carrying the payload; the Component sends the result from `eventListener`:

```kotlin
// VM — emits the Direction; does NOT touch ResultManager.
fun onSaveClick() = navigateTo(NoteEditorDirection.Save(buildNote()))

// Component — sends the result when it observes that Direction.
override suspend fun eventListener(direction: NoteEditorDirection) {
    when (direction) {
        is NoteEditorDirection.Save -> {
            sendResult(key = ResultKeys.create("note"), data = NotesRouter.Note.Action.Sync(direction.note))
            back.invoke()
        }
        NoteEditorDirection.Back -> back.invoke()
    }
}
```

## Keying a multi-caller producer (MUST when 2+ callers)

A hardcoded `"note"` key on both sides is safe **only while one screen ever opens the producer.** A second caller misroutes the result silently (single-consumer channel hands each result to whichever caller is FIFO-current). It also breaks the "one observer per key" rule. Fix — **one key per caller, from one exhaustive mapping**:

1. **Thread the caller identity on the producer's route**, as a `@Serializable` field (reuse an existing per-caller field, or add a semantic `origin`). It must live on the **route**, not a runtime `var` or `ComponentIdentifier` — results don't survive process death, so after recreation the producer re-derives its key from the route.

2. **Map identity → key in one place, co-located with the `Action`** in the producer's `*Router.<Screen>`. Exhaustive `when`, **no `else`**; raw key strings live nowhere else:
```kotlin
@Serializable
public data class Note(val note: NoteState, val origin: Origin) : NotesRouter() {
    @Serializable public enum class Origin { Notes, Archive }
    public sealed interface Action { /* Sync, Remove */ }
    public companion object {
        public fun resultKey(origin: Origin): ResultKey<Result<Action>> = when (origin) {
            Origin.Notes -> ResultKeys.create("notes.note@notes")
            Origin.Archive -> ResultKeys.create("notes.note@archive")
        }
    }
}
```

3. **Producer** sends on the key for its own `origin` (threaded from its launch route): `sendResult(key = NotesRouter.Note.resultKey(origin), data = ...)`.

4. **Each caller** observes the key for **its own** identity: `observeResult(NotesRouter.Note.resultKey(Origin.Notes)) { ... }` in one module, `observeResult(NotesRouter.Note.resultKey(Origin.Archive)) { ... }` in another. A third caller is a compile error until it adds an `Origin` + key.

> `Origin` (which caller receives the result; rides the route) is orthogonal to `ComponentIdentifier` (distinguishes multiple **live instances of the same producer** in one stack). A producer may need both.

> Delivery routing is runtime — verify on-device that the launching screen reacts and the others stay quiet.

## Lifecycle

`ResultManager.clear()` is called in `BaseComponent.lifecycle.doOnDestroy` (automatic). Subscriptions auto-cancel on component destroy. Producers can `sendResult` from any lifecycle; consumers must be alive at send time. Payloads live only in memory and **do not survive process death** — `ResultManager` is for live flows, not durable delivery.

## When to use ResultManager vs callback

| Situation | Use |
|---|---|
| Dialog returns a value to the screen that opened it; picker; confirmation yes/no | **Callback in `DialogConfig`** |
| Screen A in module X produces a value for Screen B in module Y (no shared call site) | **`ResultManager`** |
| Multi-step: A opens X, X opens Y, Y produces value for A | **`ResultManager`**, key namespaced by the initiator |
| One producer opened from 2+ callers, each needing the result | **`ResultManager`**, key per caller (above) |
| A screen is destroyed/recreated before the result arrives | **`ResultManager`** (callback would be GC'd) |

## Anti-patterns (MUST avoid)

- **Two `observeResult` for the same key in one Component.** Channel FIFO eats one.
- **Reusing a `ResultKey` across unrelated flows.** Keys are global; collisions cause cross-talk.
- **One hardcoded key shared by every caller of a multi-caller producer.** Silent misdelivery. Derive a key per caller.
- **Sending a result before any consumer subscribes.** No replay; the event is lost.
- **Threading data through `ResultManager` when a callback would work.** Verbose, harder to trace, adds a singleton dependency.
- **Using `ResultManager` for navigation.** Navigation goes via `Direction`. Results carry data only.
- **Storing the `ResultKey` as a `var`.** Keys are constants — `val` or `companion object` properties.
- **Treating `ResultManager` as durable messaging.** Reach for persistence when a result must outlive the process.

---

# ViewModel data flow (the layer the VM owns)

```kotlin
internal class NotesListViewModel(private val noteFeature: NoteFeature) : BaseViewModel<...>(...), NotesListContract {
    init {
        // Observe — Flow of domain, never blocks
        noteFeature.observeNotes(state.value.range.from, state.value.range.to)
            .map { it.toState() }                        // domain-to-state mapper
            .onEach { listState -> update { it.copy(items = listState) } }
            .safeLaunch()

        // Initial fetch — Result, triggers a loader
        safeLaunch(loader = NotesListLoader.LoadingNotes) {
            noteFeature.getNotes(start = state.value.range.from, end = state.value.range.to).getOrThrow()
        }
    }
    override fun onRangeChange(range: DateRange) {
        update { it.copy(range = range) }
        safeLaunch(loader = NotesListLoader.LoadingNotes) {
            noteFeature.getNotes(start = range.from, end = range.to).getOrThrow()
        }
    }
}
```

Rules at this layer (MUST):
- The ViewModel **never** touches `<Product>Api`, `Database`, `Dao`, `Repository`. Only `<X>Feature` (interface).
- `observe...()` returns `Flow<Domain>` — collect it inside `init { }` with `.safeLaunch()`.
- `get...()` returns `Result<T>` — call it inside `safeLaunch(loader = ...) { ... }` with `.getOrThrow()` so errors flow through `ErrorProvider`.
- Map domain to state via `:data-mappers:domain-to-state` (`it.toState()`).

---

# Reference implementation (drop-in sources, `:ui-core:foundation`)

> Replace `<org>`/`<product>` placeholders inside fences AND in each `Target path` line.

## ResultEmitter.kt

Target path: `src/commonMain/kotlin/com/<org>/<product>/core/foundation/internal/result/ResultEmitter.kt`

```kotlin
package com.<org>.<product>.core.foundation.internal.result

import com.<org>.<product>.core.foundation.models.BaseResult
import com.<org>.<product>.core.foundation.models.Result
import com.<org>.<product>.core.foundation.models.ResultKey
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import org.koin.core.annotation.Single

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

## ResultManager.kt

Target path: `src/commonMain/kotlin/com/<org>/<product>/core/foundation/internal/result/ResultManager.kt`

```kotlin
package com.<org>.<product>.core.foundation.internal.result

import com.<org>.<product>.core.foundation.models.BaseResult
import com.<org>.<product>.core.foundation.models.Result
import com.<org>.<product>.core.foundation.models.ResultKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam

@Factory
internal class ResultManager(
    private val resultEmitter: ResultEmitter,
    @InjectedParam val coroutineScope: CoroutineScope
) {
    private val activeSubscriptions = mutableListOf<Job>()

    fun <T : BaseResult> observeResult(key: ResultKey<T>, onResult: suspend (T) -> Unit) {
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
