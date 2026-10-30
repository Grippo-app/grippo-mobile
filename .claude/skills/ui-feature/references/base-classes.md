# Base Classes (`:ui-core:foundation`)

> Note/Tag/AmountPicker/NoteDetail examples are educational, nonexistent APIs. They are not the current route, dependency or source catalog; inspect real callers and signatures before adapting them.

Read the actual foundation sources under `ui-core/foundation/src/commonMain/kotlin/com/grippo/core/foundation/` before changing a base contract. This reference describes usage; it is not a replacement implementation. Result conventions live in [results.md](results.md).

## `BaseViewModel`

The **only** ViewModel base class. Every screen/dialog ViewModel extends it. Provides:
- `state: StateFlow<STATE>` and the only mutator `update { ... }`.
- `loaders: StateFlow<ImmutableSet<LOADER>>` of active operations.
- a conflated `navigator: Flow<DIRECTION>` (`navigateTo(...)`).
- the only coroutine launcher `safeLaunch` — routes uncaught exceptions through the error pipeline.
- lifecycle-aware Flow subscription `Flow<T>.safeLaunch()` that pauses upstream when not RESUMED.

**Forbidden launchers (MUST NOT):** `viewModelScope.launch`, `runBlocking`, `GlobalScope`, raw `CoroutineScope`.

### Signature

```kotlin
public abstract class BaseViewModel<STATE, DIRECTION : BaseDirection, LOADER : BaseLoader>(
    state: STATE,
) : InstanceKeeper.Instance, KoinComponent
```

Implements `InstanceKeeper.Instance` so Decompose retains the instance across config changes; `onDestroy()` (the public `InstanceKeeper.Instance` override) runs when its owning InstanceKeeper destroys it; ordinary retained-component recreation is not fresh-VM restoration. Implements `KoinComponent` for its own wiring only: `OperationManager` (with `parametersOf(coroutineScope)`), `ErrorProvider` and the toolkit `CrashReporting` API resolve through `inject`; `ScreenAnalytics` is resolved with `get<ScreenAnalytics>()` inside `bindScreen`. Feature dependencies stay constructor-injected from the Component's `retainedInstance` block. No SDK transport dependency is allowed. `ResultManager` is injected by `BaseComponent`, not here.

### Public / protected API

```kotlin
public val state: StateFlow<STATE>
protected fun update(updateFunc: (STATE) -> STATE)

public val loaders: StateFlow<ImmutableSet<LOADER>>
protected suspend fun <T> withLoader(loader: LOADER?, block: suspend () -> T): T

public val navigator: Flow<DIRECTION>
protected fun navigateTo(destination: DIRECTION)

protected open val screenPresentation: ((NavigationReason) -> ScreenPresentation)? = null

protected enum class Processing {
    WhileActive,
    Infinity,
}
protected fun safeLaunch(
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    loader: LOADER? = null,
    recovery: ErrorRecovery,
    mapError: (Throwable) -> Throwable = { it },
    block: suspend CoroutineScope.() -> Unit,
): Job
protected fun <T> Flow<T>.safeLaunch(
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    processing: Processing = Processing.WhileActive,
    recovery: ErrorRecovery,
    mapError: (Throwable) -> Throwable = { it },
): Job

override fun onDestroy()
```

### Foundation-internal lifecycle wiring

Inspect `BaseViewModel.kt`; current shape: `attachActivation(Flow<Boolean>)`,
`detachActivation()` and `bindScreen(Lifecycle)` are `internal`. `BaseComponent`
calls them from `doOnCreate`/`doOnDestroy`; feature code never calls or overrides
them. `onDestroy()` is the public `InstanceKeeper.Instance` override.

### Rules

- Kotlin block style and the named-local-function ban: see [code style](../../validation-gates/references/code-style.md#kotlin-blocks); constructor order, `init`, handlers, `update` and named arguments: see [ViewModel layout](../../validation-gates/references/code-style.md#viewmodel-layout).
- `init` starts startup requests/subscriptions directly through `safeLaunch`/Flow.
  The statements it may contain, including method calls, and where long-lived
  observations start are owned by
  [ViewModel layout](../../validation-gates/references/code-style.md#init). Other
  helpers must represent meaningful business operations, not hide startup. A failed
  operation restarts only through the error popup's Retry, in the existing VM scope
  ([Retry and close](error-pipeline.md#retry-and-close)).
- **`state`** is read-only outside the VM. `update { it.copy(...) }` is the **only** mutator (atomic compare-and-swap; thread-safe). **Never** capture `state.value` before a slow op then `update { it.copy(... = capturedValue + ...) }` — a concurrent update is lost; the lambda receives the **current** value.
- **`loaders`** is an `ImmutableSet` (multiple concurrent). `withLoader(MyLoader.X) { block() }` adds X, runs `block`, removes X on completion (success **or** failure via `try/finally`); `loader == null` runs without modifying the set. Useful inside `mapLatest`/`flatMapLatest` chains.
- **`navigator` is conflated** (`Channel.CONFLATED`). Rapid-fire pending `navigateTo()` events may collapse; this is not strict double-tap debounce or an exactly-once guarantee. Do not route navigation commands through ResultManager; it carries business result payloads. Verify the normal Direction chain when ordering matters.
- **`safeLaunch { }`**: launches one Job on the VM scope (`SupervisorJob`) through `OperationManager`. Inside it each attempt runs under `withLoader(loader)`, so the loader shows during an attempt and not while the error popup waits. A failed attempt goes through the shared log/crash/dialog pipeline and suspends until the user picks Retry (next attempt in the same Job) or closes the popup (the Job completes; `ErrorRecovery.Close` runs its `onClose`); `CancellationException` always propagates. Mechanism: [inside `BaseViewModel`](error-pipeline.md#inside-baseviewmodel--recover). `mapError` optionally maps only the dialog representation; logs/crash reporting and analytics keep the original exception. It is pure, returns unknown errors unchanged and cannot recover or navigate. `recovery` is required on both overloads and every call passes it explicitly; it decides whether the popup offers Retry; see [Retry and close](error-pipeline.md#retry-and-close). The owned job runs until completion or cancellation, including ViewModel destruction; the block overload has no lifecycle mode. See [error rules](error-pipeline.md).
- **Suspend helpers**: run in the caller's coroutine and propagate failures to its `safeLaunch`. Local catches, stage flags and recovery wrappers are forbidden; a request driven by a latest-value source follows [error rules](error-pipeline.md#requests-driven-by-a-long-lived-source). Analytics decorates the business operation and preserves its original error.
- **`screenPresentation`** defaults to null for containers. Every independent screen must override the reason-aware presenter; BaseComponent/foundation owns its lifecycle binding. Graph/null resolves no ScreenAnalytics. VM stores no Lifecycle/ComponentContext and adds no activationFlow visibility observer or scope. Verify every new/affected screen through the [mandatory screen coverage gate](../../feature-analytics/SKILL.md#mandatory-screen-coverage-gate).
- **`Flow<T>.safeLaunch()`**: default `Processing.WhileActive` keeps upstream subscribed for a **1-second grace** after leaving RESUMED, then drops it (`emptyFlow()`); re-subscribes when RESUMED again. `Processing.Infinity` keeps emitting off-screen. After its error popup is closed without Retry, the collection has ended and a later RESUMED does not restart it; see [inside `BaseViewModel`](error-pipeline.md#inside-baseviewmodel--recover).

### Flow processing modes

Only `Flow.safeLaunch()` accepts `processing`; both modes use the existing VM scope
and cancel when the ViewModel is destroyed.

| Mode | When |
|---|---|
| `Infinity` | VM-owned work that continues off-screen until completion/cancellation, including finite first-source initialization. |
| `WhileActive` | Streams that pause off-screen: list observers, real-time stats. Default for `Flow.safeLaunch()`. |

`take(1)` inside WhileActive may run again on a new subscription after resume.
One-time form population uses the existing block safeLaunch or a finite Infinity
Flow, with no private initialization flag. Restored inputs and the open draft stay
authoritative; follow the [draft policy](../../state-preservation/SKILL.md#data-backed-form-synchronization).

### Dispatchers (SHOULD)

`Dispatchers.Main.immediate` (default) for state updates/nav/dialog/light glue; `Dispatchers.IO` for explicit heavy IO (rare — IO lives in Repository); `Dispatchers.Default` for CPU-heavy work. Don't switch casually.

### `onDestroy`

The base closes its navigator and cancels the VM-owned coroutine scope; existing
safeLaunch jobs and subscriptions are cancelled. Inspect BaseViewModel before
changing this lifecycle contract; do not duplicate its implementation in a leaf.

### What MUST NOT be in a ViewModel

- Private restoration/edit flags, previous-source caches or shadow State copies,
  including equivalents hidden in wrappers or mutable closures. Keep feature data
  in State/update and derive conditions from existing values; apply the
  [canonical draft policy](../../state-preservation/SKILL.md#data-backed-form-synchronization).
- `@Composable` functions. Named local function declarations in `init`, constructors, functions or lambdas. Direct `Context`/`Activity` access. Direct API/DAO calls (only `<X>Feature`/`<X>UseCase`). Local catches, `runCatching` or result failure handlers that hide/present failures, including private helpers; call throwing commands inside `safeLaunch`. `runBlocking`. Compose state holders (`mutableStateOf`, `mutableStateListOf`) — use `update { ... }`.

### Partial MVI-only VM excerpt

This educational example omits analytics and draft wiring; it is not a complete
independent screen. A real screen MUST include its typed screenPresentation under
the [mandatory analytics screen gate](../../feature-analytics/SKILL.md#mandatory-screen-coverage-gate)
and apply the preservation gate to required drafts.

```kotlin
internal class FooViewModel(private val barFeature: BarFeature) :
    BaseViewModel<FooState, FooDirection, FooLoader>(FooState()), FooContract {
    init {
        barFeature.observeBars()
            .onEach(::provideBars)
            .safeLaunch(recovery = ErrorRecovery.Retry)
    }

    private fun provideBars(value: List<Bar>) {
        val items = value.toState()

        update { it.copy(items = items) }
    }

    override fun onRefreshClick() {
        safeLaunch(loader = FooLoader.Refresh, recovery = ErrorRecovery.Retry) {
            barFeature.getBars()
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

---

## `BaseComponent`

Decompose component base. Delegates `ComponentContext`; owns the `ViewModel` via `retainedInstance { ... }`; subscribes to `viewModel.navigator` and dispatches `Direction`s to `eventListener`; bridges Decompose `Lifecycle` into the VM's activation flow; exposes `observeResult`/`sendResult`.

### Signature & required overrides

```kotlin
public abstract class BaseComponent<DIRECTION : BaseDirection>(
    componentContext: ComponentContext,
) : ComponentContext by componentContext, KoinComponent

protected abstract val viewModel: BaseViewModel<*, DIRECTION, *>
protected abstract suspend fun eventListener(direction: DIRECTION)
@Composable public abstract fun Render()
```

### Lifecycle wiring (built in)

On **create**: obtain the retained VM, call `bindScreen(lifecycle)`, attach activation, then collect navigator into eventListener. Binding precedes delivery of navigation emitted by VM init. On **destroy**: detach activation, clear ResultManager, cancel the Component's scope. ScreenAnalytics closes presentation on pause/destroy. The Component's `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` serves navigator/result subscriptions, not screen analytics or the VM's business work.

### `eventListener`

`when (direction) { ... }` over every `Direction` subtype (compiler-enforced exhaustive). Each branch maps to a constructor-injected lambda (`back`, `toX`) **or** a nav op on this Component's own `StackNavigation` (for a stack-owning feature root). It is `suspend`. Ordinary screen ViewModels call the non-suspending `DialogController.open(config)`; nested dialog Components forward semantic directions through typed constructor callbacks, and the host uses the originating step's Session. See [dialogs.md](dialogs.md).

### `Render` & ViewModel creation (MUST)

```kotlin
@Composable
override fun Render() {
    val state = viewModel.state.collectAsStateMultiplatform()
    val loaders = viewModel.loaders.collectAsStateMultiplatform()
    NoteDetailScreen(state.value, loaders.value, viewModel)
}

override val viewModel = componentContext.retainedInstance {
    NoteDetailViewModel(dialogController = getKoin().get(), noteFeature = getKoin().get(), /* ... */)
}
```

- Use `collectAsStateMultiplatform()`, **not** `collectAsState`.
- All dependencies, including stateless feature analytics, are pulled via `getKoin().get()` inside `retainedInstance { ... }`, not threaded through the Component constructor. Feature analytics stays in the VM; every independent screen's required screenPresentation is bound by BaseComponent/foundation. Containers keep default null. See [feature analytics](../../feature-analytics/SKILL.md).
- Stack-owning components pass `this` to the Screen (`ProfileScreen(this, state, loaders, viewModel)`) so it can read `component.childStack`.

### Constructor parameters (MUST)

A Component's constructor takes `componentContext`, **navigation callbacks**
(`back`, `close`, `toX`), **ordinary initial business inputs** from a route or
accepted parent flow (`initialRange`, `userId`, accepted selection), and
**child-component factories** supplied by the composition root under
[nested cross-feature flows](navigation.md#nested-cross-feature-flows).
Feature interfaces, UseCases, DialogControllers, repositories and analytics
services resolve from Koin inside retained VM construction.

### Business draft preservation

Use protected `restoreState(PersistenceKey<T>): T?` inside the existing retainedInstance
factory and `saveState(PersistenceKey<T>, supplier: () -> T)` once during construction.
The latter registers a lazy supplier; the shell's StateKeeper.save reads current
VM state. [State-preservation](../../state-preservation/SKILL.md#ownership-and-implementation)
owns descriptors, draft precedence and initialization; apply its mandatory gate.

### Result observation

Inspect `BaseComponent.kt`; current shape: `protected fun <T : BaseResult> observeResult(key: ResultKey<T>, onResult: suspend (T) -> Unit)` and `protected fun <T : Any> sendResult(key: ResultKey<Result<T>>, data: T)`. The emitter wraps `data` in `Result(data)` (`ResultEmitter.kt`); callers never construct `Result` themselves. See `references/results.md`.

### Back handling

`init { backHandler.register(BackCallback(onBack = viewModel::onBack)) }`. `viewModel.onBack()` (a Contract method) can update state, then `navigateTo(Direction.Back)`.

### What a Component MUST NOT do

Render UI directly (`Render()` calls a `<X>Screen` function). Hold state. Call `<X>Feature`/`<X>UseCase`. Cache `getKoin().get()` in fields. Subscribe to long-lived flows (the VM does). Mutate a parent's router stack (use a constructor lambda).

### Root components with their own stack

A stack-owning feature root owns a private `StackNavigation` + `Value<ChildStack<<X>Router, Child>>` and is a **`public class`** (constructed from `:shared`):

Inspect the actual [ProfileComponent](../../../../ui-screen-features/profile/src/commonMain/kotlin/com/grippo/profile/ProfileComponent.kt): its Back handler pops a nonempty back stack and calls close only at the feature root. Leaf children forward Back to that handler. MUST distinguish returning to a previous child from exiting the feature; do not wire every child Back directly to close.

Pop/back is wired through `BackCallback(onBack = viewModel::onBack)` + `backHandler.register(...)`, not by inspecting `navigation.popOrNull()` in `eventListener` — the VM owns back semantics. Nested graph Components may own their own stacks, as AuthProcess and ProfileCreation do. Each graph handles its own navigation; leaf Components delegate navigation through callbacks.

---

## `BaseComposeScreen` & `ScreenBackground`

A thin Composable wrapper at the root of every screen: sets background, lays out in a `Column`, registers a tap-anywhere gesture that clears focus (dismisses the keyboard).

Inspect [BaseScreen.kt](../../../../ui-core/foundation/src/commonMain/kotlin/com/grippo/core/foundation/BaseScreen.kt) for the exact implementation; do not copy a replacement from this reference. Current shape: `BaseComposeScreen(background: ScreenBackground.Color, content: @Composable ColumnScope.() -> Unit)`; `ScreenBackground` is a `@Stable sealed interface` with exactly one subtype, `@Immutable data class Color(val value: Color)`.

Notes: `Column` (not `Box`/`LazyColumn`); **no** `fillMaxSize()` (content drives height); `clickable` with `indication = null` (no ripple); the `content` lambda receives `ColumnScope`.

### Rules (MUST)

- **Every top-level Screen is wrapped in `BaseComposeScreen`** — both stack and dialog screens.
- **Background color from `AppTokens`** — `AppTokens.colors.background.screen` for stack screens; `AppTokens.colors.background.dialog` for dialog screens.
- **Leaf dialog screens reuse `BaseComposeScreen`** (`ScreenBackground.Color(...background.dialog)`); their spacing, horizontal padding and scrolling-body ending follow step 3 of the [dialogs](dialogs.md) recipe. They add no toolbar or duplicate title: the shared host renders `BottomSheetToolbar` from the active config's `title: UiText`, with Back for nested steps and Close for the entire session.

### What it does NOT do

No `Toolbar` (the screen adds its own). No system-insets handling (edge-to-edge is Activity-level; per-screen insets via `Toolbar`/`OverlayContainer`). No scrolling container (use `LazyColumn` inside `content`).

---

## Marker interfaces & identity types

Source: the `com.grippo.core.foundation.models` package in `:ui-core:foundation`.

- **`BaseDirection`** — `public interface BaseDirection`. ViewModel nav intents. Per-screen `<Name>Direction` is a `sealed interface : BaseDirection` (compiler-enforced exhaustiveness in `eventListener`); subtypes `data object`/`data class`. **No `@Serializable`** — Directions are consumed immediately; they don't survive process death (the router stack does).
- **`BaseLoader`** — `public interface BaseLoader`. Active async ops. Per-screen `<Name>Loader` is an `@Immutable sealed interface : BaseLoader` (every subtype `@Immutable data object`). A screen with no async ops still declares the interface (empty body).
- **`BaseRouter`** — `public interface BaseRouter`. Nav route marker in `:ui-screen-features:screen-api`. `@Serializable` on the sealed class and every subtype; subtypes carry only serializable payloads.
- **`BaseResult`** + `Result<T>` — `Result<T : Any>(val data: T) : BaseResult` wraps every payload carried through `ResultManager`. Action protocols are plain sealed interfaces nested inside the producer's `*Router.<Screen>` (no `BaseResult` extension); `sendResult` wraps them in `Result<T>`.
- **`ResultKey<T : BaseResult>(val key: String)`** + `ResultKeys.create(key)` — typed channel identifiers; equal by string. **One `ResultKey` per protocol, not per subtype.** The receiver defines the key in its companion object and passes an explicit `resultKey: ResultKey<Result<Payload>>` on the producer route; scope by receiver identity when multiple instances can coexist (see `references/results.md`).
- **`PersistenceKey<T : Any>(key: String, serializer: KSerializer<T>)`** — a final class whose two properties are `internal`: a stable local archive address and its serializer. Descriptor ownership follows [state-preservation](../../state-preservation/SKILL.md#ownership-and-implementation).

### Rules (MUST)

- Don't extend `BaseDirection` with state (Directions are transient; persisted intent belongs in the route).
- Don't extend `BaseLoader` with state (Loaders are tags; communicate progress via a State field).
- Don't extend `BaseRouter` outside `:ui-screen-features:screen-api`.
- Don't extend `BaseResult` yourself — the only implementer is `Result<T>`.
- One `ResultKey` per protocol, not per subtype.

---

## `OperationManager` (internal — VM authors use `safeLaunch`)

`internal interface OperationManager` (`internal/operation/`); `OperationManagerImpl` is `@Factory(binds = [OperationManager::class])` with `@InjectedParam coroutineScope`. `BaseViewModel` gets its own instance bound to its own scope (cancelled in `onDestroy()`):

- **`launch(dispatcher, onError: suspend (Throwable) -> Unit, block)`** — runs `block` on `coroutineScope` with the dispatcher + a `CoroutineExceptionHandler` that, for any non-`CancellationException`, re-launches `onError(t)` on that same scope; `block` runs inside `supervisorScope { block() }`.
- **`whileActive(upstream, activation)`** — the 1-second debounced unsubscription for `Processing.WhileActive`:
  `activation` is RESUMED-true / else-false, provided by `BaseComponent` via `attachActivation(lifecycle.asActiveFlow())`. Returning to RESUMED within 1s cancels the delay (flatMapLatest) and keeps upstream subscribed.

`CancellationException` is **never** treated as an error. `OperationManager` does **not** throttle/debounce/rate-limit, does **not** retry, does **not** log success. `CoreModule` (`@Module @ComponentScan` in `com.grippo.core.foundation`) discovers `OperationManagerImpl`, `ScreenAnalyticsImpl`, `ResultEmitter` and `ResultManager` and is included in `:shared/Koin.kt`.

---

## `ScreenAnalytics` (foundation binding)

`ScreenAnalytics` is a public interface in `foundation/analytics/`; `ScreenAnalyticsImpl` (`internal/analytics/`) is `@Single(binds = [ScreenAnalytics::class]) internal`. `bind` runs `check(lifecycle.state != Lifecycle.State.DESTROYED)`, presents on resume (`Initial` first, `Restore` afterwards) and closes on pause/destroy; exceptions thrown by `present()`/`close()` are swallowed so telemetry never interrupts lifecycle delivery. `BaseViewModel.bindScreen` resolves it with `get<ScreenAnalytics>()` only when `screenPresentation` is non-null. Rules: [feature-analytics](../../feature-analytics/SKILL.md).

---

## Platform helpers (`:ui-core:foundation/platform`, `expect/actual`)

- **`collectAsStateMultiplatform()`** — `collectAsStateWithLifecycle` on Android (pauses below STARTED), `collectAsState` on iOS. **Never use plain `collectAsState` for `viewModel.state`/`viewModel.loaders`** — use this consistently.
- **`platformAnimation()` / `platformStackAnimator()`** — Decompose stack animations: `fade() + slide()` on Android, `iosLikeSlide()` (slide-in-from-right + parallax) on iOS. Use in `*RootScreen`:
  ```kotlin
  ChildStack(stack = stack, animation = stackAnimation(animator = platformStackAnimator())) { child ->
      child.instance.component.Render()
  }
  // per-child custom: stackAnimation { child, _, _, _ -> child.instance.animator() }
  ```

### Adding a new platform helper (SHOULD)

Declare `@Composable public expect fun X()` in commonMain; implement in androidMain + iosMain; one helper per file. **Avoid** `expect/actual` if both implementations are identical — put it in commonMain. These helpers live in `:ui-core:foundation` (not `:toolkit:*`) because they're Composable and tied to Decompose `ChildStack`.

---
