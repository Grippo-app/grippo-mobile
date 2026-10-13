# Base Classes (`:ui-core:foundation`)

> Drop-in reference sources live at the end (BaseViewModel/Component/Screen/models/
> OperationManager/platform helpers). Replace `<org>`/`<product>` placeholders both
> inside fences and in each `Target path` line. The `ResultManager`/`ResultEmitter`
> source links and result conventions live in `references/results.md`.

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

Implements `InstanceKeeper.Instance` so Decompose retains the instance across config changes; `onDestroy()` runs when the host component is destroyed. Implements `KoinComponent` for internal `by inject<...>()` (used for `OperationManager` and `ErrorProvider`). Project default is constructor injection. `CrashReporting` is injected from toolkit API inside the error pipeline; no SDK transport dependency is allowed. `ResultManager` is injected by `BaseComponent`, not here.

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
    onError: (() -> Unit) = {},
    mapError: (Throwable) -> Throwable = { it },
    block: suspend CoroutineScope.() -> Unit,
): Job
protected fun <T> Flow<T>.safeLaunch(
    dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    processing: Processing = Processing.WhileActive,
    onError: (() -> Unit) = {},
    mapError: (Throwable) -> Throwable = { it },
): Job

override fun onDestroy()
```

### Rules

- Follow [Kotlin block style](../../../../CLAUDE.md#kotlin): business/control bodies
  and `safeLaunch`/`trackOperation` stay multiline; short single-expression `update`
  and simple Flow transformations remain compact. No named local functions inside
  `init`, constructors, other functions or lambdas; methods/functions belong at
  class/file scope. Ordinary callbacks and DSL lambdas remain allowed.
- `init` normally starts startup requests/subscriptions directly through
  `safeLaunch`/Flow. A complete operation reused for initial loading and a retry
  after dialog dismissal belongs in a private class-level method called by `init`
  and `onError`. Do not duplicate its request sequence or use a local declaration.
  Other helpers must represent meaningful business operations, not hide startup.
  Reuse the existing VM scope and follow the [error rules](error-pipeline.md).
- **`state`** is read-only outside the VM. `update { it.copy(...) }` is the **only** mutator (atomic compare-and-swap; thread-safe). **Never** capture `state.value` before a slow op then `update { it.copy(... = capturedValue + ...) }` — a concurrent update is lost; the lambda receives the **current** value.
- **`loaders`** is an `ImmutableSet` (multiple concurrent). `withLoader(MyLoader.X) { block() }` adds X, runs `block`, removes X on completion (success **or** failure via `try/finally`); `loader == null` runs without modifying the set. Useful inside `mapLatest`/`flatMapLatest` chains.
- **`navigator` is conflated** (`Channel.CONFLATED`). Rapid-fire pending `navigateTo()` events may collapse; this is not strict double-tap debounce or an exactly-once guarantee. For every-event delivery use `ResultManager` or design the Direction so the second event is a no-op.
- **`safeLaunch { }`**: launches on the VM scope with `SupervisorJob` + a `CoroutineExceptionHandler`. If `loader != null`, adds before launch / removes via `invokeOnCompletion`. Unhandled non-cancellation failures use the shared log/crash/dialog pipeline. `mapError` optionally maps only the dialog representation; logs/crash reporting and analytics keep the original exception. It is pure, returns unknown errors unchanged and cannot recover or navigate. `onError` runs after dialog dismissal. The owned job runs until completion or cancellation, including ViewModel destruction; the block overload has no lifecycle mode. See [error rules](error-pipeline.md).
- **Suspend helpers**: run in the caller's coroutine and propagate failures to its `safeLaunch`. No local catches, additional jobs, stage flags or recovery wrappers. Analytics decorates the business operation and preserves its original error. See [error rules](error-pipeline.md).
- **`screenPresentation`** defaults to null for containers. Every independent screen must override the reason-aware presenter; BaseComponent/foundation owns its lifecycle binding. Graph/null resolves no ScreenAnalytics. VM stores no Lifecycle/ComponentContext and adds no activationFlow visibility observer or scope. Verify every new/affected screen through the [mandatory screen coverage gate](../../feature-analytics/SKILL.md#mandatory-screen-coverage-gate).
- **`Flow<T>.safeLaunch()`**: default `Processing.WhileActive` keeps upstream subscribed for a **1-second grace** after leaving RESUMED, then drops it (`emptyFlow()`); re-subscribes when RESUMED again. `Processing.Infinity` keeps emitting off-screen.

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

```kotlin
override fun onDestroy() {
    _navigator.close()
    coroutineScope.cancel()
}
```

Closes the navigator channel and cancels the VM's `coroutineScope`; `Flow.safeLaunch()` subscriptions cancel automatically.

### What MUST NOT be in a ViewModel

- Private restoration/edit flags, previous-source caches or shadow State copies,
  including equivalents hidden in wrappers or mutable closures. Keep feature data
  in State/update and derive conditions from existing values; apply the
  [canonical draft policy](../../state-preservation/SKILL.md#data-backed-form-synchronization).
- `@Composable` functions. Named local function declarations in `init`, constructors, functions or lambdas. Direct `Context`/`Activity` access. Direct API/DAO calls (only `<X>Feature`/`<X>UseCase`). Local catches, `runCatching` or result failure handlers that hide/present failures, including private helpers; use `getOrThrow()` inside `safeLaunch`. `runBlocking`. Compose state holders (`mutableStateOf`, `mutableStateListOf`) — use `update { ... }`.

### Minimal VM template

```kotlin
internal class FooViewModel(private val barFeature: BarFeature) :
    BaseViewModel<FooState, FooDirection, FooLoader>(FooState()), FooContract {
    init {
        barFeature.observeBars()
            .map { it.toState() }
            .onEach { items -> update { it.copy(items = items) } }
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

A Component's constructor takes only `componentContext`, **navigation callbacks** (`back`, `close`, `toX`), and **ordinary initial business inputs** from a route or accepted parent flow (`initialRange`, `userId`, accepted selection). **Forbidden** in ctor params: Feature interfaces, UseCases, DialogControllers, repositories and analytics services — those come from Koin inside retained VM construction.

### Business draft preservation

Use protected `restoreState(PersistenceKey<T>): T?` inside the existing retainedInstance
factory and `saveState(PersistenceKey<T>, supplier: () -> T)` once during construction.
The latter registers a lazy supplier; the shell's StateKeeper.save reads current
VM state. Keep defaults/route seeds explicit at the owning Component and restore
parent data before children. Define `val DRAFT = PersistenceKey("stable address", State.serializer())`
in a private companion object at the bottom of the owning Component. Use the typed
helpers; no separate metadata file, feature-wide matrix, raw draft consume/register
or identifier-derived keys.
See [state-preservation](../../state-preservation/SKILL.md) for the mandatory gate.

### Result observation

`protected fun <T : BaseResult> observeResult(key, onResult)` / `sendResult(key, data)`. See `references/results.md`.

### Back handling

`init { backHandler.register(BackCallback(onBack = viewModel::onBack)) }`. `viewModel.onBack()` (a Contract method) can update state, then `navigateTo(Direction.Back)`.

### What a Component MUST NOT do

Render UI directly (`Render()` calls a `<X>Screen` function). Hold state. Call `<X>Feature`/`<X>UseCase`. Cache `getKoin().get()` in fields. Subscribe to long-lived flows (the VM does). Mutate a parent's router stack (use a constructor lambda).

### Root components with their own stack

A stack-owning feature root owns a private `StackNavigation` + `Value<ChildStack<<X>Router, Child>>` and is a **`public class`** (constructed from `:shared`):

```kotlin
public class ProfileComponent(
    initial: ProfileRouter,
    componentContext: ComponentContext,
    private val close: () -> Unit,
) : BaseComponent<ProfileDirection>(componentContext) {
    override val viewModel: ProfileViewModel = componentContext.retainedInstance { ProfileViewModel() }
    private val backCallback = BackCallback(onBack = viewModel::onBack)
    init { backHandler.register(backCallback) }
    override suspend fun eventListener(direction: ProfileDirection) {
        when (direction) { ProfileDirection.Back -> close.invoke() }
    }
    private val navigation = StackNavigation<ProfileRouter>()
    internal val childStack: Value<ChildStack<ProfileRouter, Child>> = childStack(
        source = navigation, serializer = ProfileRouter.serializer(),
        initialStack = { listOf(initial) }, key = "ProfileComponent",
        handleBackButton = true, childFactory = ::createChild,
    )
    private fun createChild(router: ProfileRouter, context: ComponentContext): Child = when (router) {
        ProfileRouter.Overview -> Child.Overview(ProfileOverviewComponent(componentContext = context, back = viewModel::onBack))
        ProfileRouter.Settings -> Child.Settings(ProfileSettingsComponent(componentContext = context, back = viewModel::onBack))
    }
    @Composable override fun Render() { /* collects state/loaders, calls ProfileScreen(this, ...) */ }
    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Overview(override val component: ProfileOverviewComponent) : Child(component)
        data class Settings(override val component: ProfileSettingsComponent) : Child(component)
    }
}
```

Pop/back is wired through `BackCallback(onBack = viewModel::onBack)` + `backHandler.register(...)`, not by inspecting `navigation.popOrNull()` in `eventListener` — the VM owns back semantics. Nested graph Components may own their own stacks, as AuthProcess and ProfileCreation do. Each graph handles its own navigation; leaf Components delegate navigation through callbacks.

---

## `BaseComposeScreen` & `ScreenBackground`

A thin Composable wrapper at the root of every screen: sets background, lays out in a `Column`, registers a tap-anywhere gesture that clears focus (dismisses the keyboard).

```kotlin
@Composable
public fun BaseComposeScreen(
    background: ScreenBackground.Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val focusInteractionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .background(background.value)
            .clickable(interactionSource = focusInteractionSource, indication = null,
                onClick = { focusManager.clearFocus(force = true) }),
        content = content,
    )
}

@Stable
public sealed interface ScreenBackground {
    @Immutable public data class Color(public val value: ComposeColor) : ScreenBackground
}
```

Notes: `Column` (not `Box`/`LazyColumn`); **no** `fillMaxSize()` (content drives height); `clickable` with `indication = null` (no ripple); the `content` lambda receives `ColumnScope`.

### Rules (MUST)

- **Every top-level Screen is wrapped in `BaseComposeScreen`** — both stack and dialog screens.
- **Background color from `AppTokens`** — `AppTokens.colors.background.screen` for stack screens; `AppTokens.colors.background.dialog` for dialog screens.
- **Leaf dialog screens reuse `BaseComposeScreen`** (`ScreenBackground.Color(...background.dialog)`), laying out `Spacer(AppTokens.dp.dialog.top)` → body → `Spacer(AppTokens.dp.dialog.bottom)` → `Spacer(Modifier.navigationBarsPadding())`, horizontal padding `AppTokens.dp.dialog.horizontalPadding`. They add no toolbar or duplicate title: the shared host renders `BottomSheetToolbar` from the active config's `title: UiText`, with Back for nested steps and Close for the entire session.

### What it does NOT do

No `Toolbar` (the screen adds its own). No system-insets handling (edge-to-edge is Activity-level; per-screen insets via `Toolbar`/`BottomOverlayContainer`). No scrolling container (use `LazyColumn` inside `content`).

---

## Marker interfaces & identity types (`:ui-core:foundation:models`)

- **`BaseDirection`** — `public interface BaseDirection`. ViewModel nav intents. Per-screen `<Name>Direction` is a `sealed interface : BaseDirection` (compiler-enforced exhaustiveness in `eventListener`); subtypes `data object`/`data class`. **No `@Serializable`** — Directions are consumed immediately; they don't survive process death (the router stack does).
- **`BaseLoader`** — `public interface BaseLoader`. Active async ops. Per-screen `<Name>Loader` is an `@Immutable sealed interface : BaseLoader` (every subtype `@Immutable data object`). A screen with no async ops still declares the interface (empty body).
- **`BaseRouter`** — `public interface BaseRouter`. Nav route marker in `:ui-screen-features:screen-api`. `@Serializable` on the sealed class and every subtype; subtypes carry only serializable payloads.
- **`BaseResult`** + `Result<T>` — `Result<T : Any>(val data: T) : BaseResult` wraps every payload carried through `ResultManager`. Action protocols are plain sealed interfaces nested inside the producer's `*Router.<Screen>` (no `BaseResult` extension); `sendResult` wraps them in `Result<T>`.
- **`ResultKey<T : BaseResult>(val key: String)`** + `ResultKeys.create(key)` — typed channel identifiers; equal by string. **One `ResultKey` per protocol, not per subtype.** The receiver defines the key in its companion object and passes an explicit `resultKey: ResultKey<Result<Payload>>` on the producer route; scope by receiver identity when multiple instances can coexist (see `references/results.md`).
- **`PersistenceKey<T : Any>`** — final invariant draft descriptor: stable local archive address and matching serializer. The owning Component defines `val DRAFT` in its bottom private companion object and calls protected `restoreState(DRAFT)` / `saveState(DRAFT, supplier)`. It does not identify a live instance or result channel; child ComponentContexts isolate equal keys. No separate metadata file or matrix.

### Rules (MUST)

- Don't extend `BaseDirection` with state (Directions are transient; persisted intent belongs in the route).
- Don't extend `BaseLoader` with state (Loaders are tags; communicate progress via a State field).
- Don't extend `BaseRouter` outside `:ui-screen-features:screen-api`.
- Don't extend `BaseResult` yourself — the only implementer is `Result<T>`.
- One `ResultKey` per protocol, not per subtype.

---

## `OperationManager` (internal — VM authors use `safeLaunch`)

`@Factory(binds = [OperationManager::class])`, `@InjectedParam coroutineScope`. `BaseViewModel` gets its own instance bound to its own scope (cancelled in `onDestroy()`):

- **`launch(dispatcher, onError, block)`** — runs `block` on `coroutineScope` with the dispatcher + a `CoroutineExceptionHandler` calling `onError(t)` for any non-`CancellationException` + a `SupervisorJob` child (failures don't cancel siblings), wrapped in `supervisorScope { block() }`.
- **`whileActive(upstream, activation)`** — the 1-second debounced unsubscription for `Processing.WhileActive`:
  ```kotlin
  override fun <T> whileActive(upstream: Flow<T>, activation: Flow<Boolean>): Flow<T> =
      activation
          .flatMapLatest { active -> if (active) flowOf(true) else flow { delay(1.seconds); emit(false) } }
          .distinctUntilChanged()
          .flatMapLatest { active -> if (active) upstream else emptyFlow() }
  ```
  `activation` is RESUMED-true / else-false, provided by `BaseComponent` via `attachActivation(lifecycle.asActiveFlow())`. Returning to RESUMED within 1s cancels the delay (flatMapLatest) and keeps upstream subscribed.

`CancellationException` is **never** treated as an error. `OperationManager` does **not** throttle/debounce/rate-limit, does **not** retry, does **not** log success. `CoreModule` (`@Module @ComponentScan`) discovers `OperationManagerImpl` and is included in `:shared/Koin.kt`.

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

# Reference implementation (drop-in sources)

> Replace `<org>`/`<product>` placeholders inside fences AND in each `Target path` line.

## BaseViewModel.kt

Target path: `src/commonMain/kotlin/com/<org>/<product>/core/foundation/BaseViewModel.kt`

> Error reporting depends on toolkit contracts, independently of the attached platform SDK adapter.

```kotlin
package com.<org>.<product>.core.foundation

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.<org>.<product>.core.error.provider.ErrorProvider
import com.<org>.<product>.core.foundation.analytics.ScreenAnalytics
import com.<org>.<product>.core.foundation.internal.operation.OperationManager
import com.<org>.<product>.core.foundation.models.BaseDirection
import com.<org>.<product>.core.foundation.models.BaseLoader
import com.<org>.<product>.toolkit.analytics.context.NavigationReason
import com.<org>.<product>.toolkit.analytics.lifecycle.ScreenPresentation
import com.<org>.<product>.toolkit.crashreporting.CrashReporting
import com.<org>.<product>.toolkit.logger.AppLogger
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

public abstract class BaseViewModel<STATE, DIRECTION : BaseDirection, LOADER : BaseLoader>(
    state: STATE,
) : InstanceKeeper.Instance, KoinComponent {

    // ------------ STATE API ------------

    private val _state: MutableStateFlow<STATE> = MutableStateFlow(state)
    public val state: StateFlow<STATE> = _state.asStateFlow()

    protected fun update(updateFunc: (STATE) -> STATE) {
        _state.update(updateFunc)
    }

    // ------------ LOADER API ------------

    private val _loaders = MutableStateFlow<ImmutableSet<LOADER>>(persistentSetOf())
    public val loaders: StateFlow<ImmutableSet<LOADER>> = _loaders.asStateFlow()

    protected suspend fun <T> withLoader(
        loader: LOADER?,
        block: suspend () -> T
    ): T {
        addLoader(loader)
        return try {
            block()
        } finally {
            removeLoader(loader)
        }
    }

    private fun addLoader(loader: LOADER?) {
        loader ?: return
        _loaders.update { (it + loader).toPersistentSet() }
    }

    private fun removeLoader(loader: LOADER?) {
        loader ?: return
        _loaders.update { (it - loader).toPersistentSet() }
    }

    // ------------ NAVIGATION API ------------

    private val _navigator = Channel<DIRECTION>(Channel.CONFLATED)
    public val navigator: Flow<DIRECTION> = _navigator.receiveAsFlow()

    protected fun navigateTo(destination: DIRECTION) {
        _navigator.trySend(destination)
    }

    // ------------ COROUTINE API ------------

    protected enum class Processing {
        WhileActive,
        Infinity,
    }

    private val coroutineScope: CoroutineScope = CoroutineScope(
        context = SupervisorJob() + Dispatchers.Main.immediate
    )

    private val operationManager by inject<OperationManager> {
        parametersOf(coroutineScope)
    }

    protected fun <T> Flow<T>.safeLaunch(
        dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
        processing: Processing = Processing.WhileActive,
        onError: (() -> Unit) = {},
        mapError: (Throwable) -> Throwable = { it },
    ): Job {
        val flow = when (processing) {
            Processing.WhileActive -> operationManager.whileActive(this@safeLaunch, activation)
            Processing.Infinity -> this@safeLaunch
        }

        return operationManager.launch(
            dispatcher = dispatcher,
            onError = { exception ->
                sendError(exception, onError, mapError)
            },
            block = {
                flow.collect()
            }
        )
    }

    protected fun safeLaunch(
        dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
        loader: LOADER? = null,
        onError: (() -> Unit) = {},
        mapError: (Throwable) -> Throwable = { it },
        block: suspend CoroutineScope.() -> Unit,
    ): Job {
        addLoader(loader)

        val job = operationManager.launch(
            dispatcher = dispatcher,
            onError = { exception ->
                sendError(exception, onError, mapError)
            },
            block = block,
        )

        job.invokeOnCompletion {
            removeLoader(loader)
        }
        return job
    }

    // ------------ ACTIVATION API ------------

    private val _activation = MutableStateFlow<Flow<Boolean>?>(null)
    internal val activation: Flow<Boolean> = _activation
        .filterNotNull()
        .flatMapLatest { it }
        .distinctUntilChanged()

    internal fun attachActivation(activationFlow: Flow<Boolean>) {
        _activation.value = activationFlow
            .distinctUntilChanged()
            .onCompletion { emit(false) }
    }

    internal fun detachActivation() {
        _activation.value = null
    }

    // ------------ SCREEN ANALYTICS API ------------

    /** Every independent screen overrides this hook; containers keep null. Foundation binds the lifecycle. */
    protected open val screenPresentation: ((NavigationReason) -> ScreenPresentation)? = null

    internal fun bindScreen(lifecycle: Lifecycle) {
        val present = screenPresentation ?: return
        get<ScreenAnalytics>().bind(lifecycle, present)
    }

    // ------------ ERROR API ------------

    private val errorProvider by inject<ErrorProvider>()
    private val crashReporting by inject<CrashReporting>()

    private suspend fun sendError(
        exception: Throwable,
        onError: () -> Unit,
        mapError: (Throwable) -> Throwable,
    ) {
        val log = buildString {
            append("─────── ViewModel error ──────\n")
            append("│ message: ${exception.message}\n")
            append("│ cause: ${exception.cause?.message}\n")
            append("└──────────────────────────")
        }
        AppLogger.General.error(log)
        crashReporting.reportException(exception)
        val displayError = if (exception is Exception) {
            mapError(exception)
        } else {
            exception
        }
        errorProvider.provide(displayError, callback = onError)
    }

    // ------------ RELEASE API ------------

    override fun onDestroy() {
        _navigator.close()
        coroutineScope.cancel()
    }
}
```

## BaseComponent.kt

Target path: `src/commonMain/kotlin/com/<org>/<product>/core/foundation/BaseComponent.kt`

```kotlin
package com.<org>.<product>.core.foundation

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import com.<org>.<product>.core.foundation.internal.result.ResultManager
import com.<org>.<product>.core.foundation.models.BaseDirection
import com.<org>.<product>.core.foundation.models.BaseResult
import com.<org>.<product>.core.foundation.models.PersistenceKey
import com.<org>.<product>.core.foundation.models.Result
import com.<org>.<product>.core.foundation.models.ResultKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

public abstract class BaseComponent<DIRECTION : BaseDirection>(
    componentContext: ComponentContext,
) : ComponentContext by componentContext, KoinComponent {

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val resultManager by inject<ResultManager> { parametersOf(coroutineScope) }

    protected abstract val viewModel: BaseViewModel<*, DIRECTION, *>

    init {
        lifecycle.doOnCreate {
            viewModel.bindScreen(lifecycle)
            viewModel.attachActivation(lifecycle.asActiveFlow())

            viewModel.navigator
                .onEach(::eventListener)
                .launchIn(coroutineScope)
        }

        lifecycle.doOnDestroy {
            viewModel.detachActivation()
            resultManager.clear()
            coroutineScope.cancel()
        }
    }

    protected abstract suspend fun eventListener(direction: DIRECTION)

    /** Consume a local draft only when constructing a fresh retained ViewModel. */
    protected fun <T : Any> restoreState(key: PersistenceKey<T>): T? =
        stateKeeper.consume(key.key, key.serializer)

    /** Register once during construction; the supplier is read when StateKeeper saves. */
    protected fun <T : Any> saveState(key: PersistenceKey<T>, supplier: () -> T) {
        stateKeeper.register(key.key, key.serializer, supplier)
    }

    /**
     * Register once per key on the main thread when constructing the component.
     * Observes until destruction, including while hidden. Keep the handler short.
     */
    protected fun <T : BaseResult> observeResult(
        key: ResultKey<T>,
        onResult: suspend (T) -> Unit
    ) {
        resultManager.observeResult(key, onResult)
    }

    /**
     * Sends to current observers without replay; returns before handlers finish.
     * Destruction cancels pending sends without retracting results already in the buffer.
     */
    protected fun <T : Any> sendResult(key: ResultKey<Result<T>>, data: T) {
        resultManager.sendResult(key, data)
    }

    @Composable
    public abstract fun Render()


    private fun Lifecycle.asActiveFlow(): Flow<Boolean> = callbackFlow {
        trySend(state == Lifecycle.State.RESUMED)

        val cb = object : Lifecycle.Callbacks {
            override fun onResume() {
                trySend(true).isSuccess
            }

            override fun onPause() {
                trySend(false).isSuccess
            }

            override fun onDestroy() {
                close()
            }
        }

        subscribe(cb)
        awaitClose { unsubscribe(cb) }
    }.distinctUntilChanged()
}
```

## BaseScreen.kt

Target path: `src/commonMain/kotlin/com/<org>/<product>/core/foundation/BaseScreen.kt`

```kotlin
package com.<org>.<product>.core.foundation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color as ComposeColor

@Stable
public sealed interface ScreenBackground {
    @Immutable
    public data class Color(val value: ComposeColor) : ScreenBackground
}

@Composable
public fun BaseComposeScreen(
    background: ScreenBackground.Color,
    content: @Composable ColumnScope.() -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val focusInteractionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .background(background.value)
            .clickable(
                interactionSource = focusInteractionSource,
                indication = null,
                onClick = { focusManager.clearFocus(force = true) }
            ),
        content = content
    )
}
```

## Marker interfaces (models package)

Target paths: `src/commonMain/kotlin/com/<org>/<product>/core/foundation/models/<File>.kt`

```kotlin
// BaseDirection.kt
package com.<org>.<product>.core.foundation.models
public interface BaseDirection

// BaseLoader.kt
package com.<org>.<product>.core.foundation.models
public interface BaseLoader

// BaseRouter.kt
package com.<org>.<product>.core.foundation.models
public interface BaseRouter

// PersistenceKey.kt
package com.<org>.<product>.core.foundation.models
import kotlinx.serialization.KSerializer
/** Stable archive address and serializer for one typed, component-owned draft. */
public class PersistenceKey<T : Any>(
    internal val key: String,
    internal val serializer: KSerializer<T>,
)
```

Reuse the existing [BaseResult.kt](../../../../ui-core/foundation/src/commonMain/kotlin/com/grippo/core/foundation/models/BaseResult.kt)
and [ResultKeySerializer.kt](../../../../ui-core/foundation/src/commonMain/kotlin/com/grippo/core/foundation/models/ResultKeySerializer.kt).
ResultKey's custom serializer writes only its address, allowing non-serializable
Action protocols in serializable routes. Do not replace it with a plain data class
or a generated payload serializer; see [results.md](results.md#resultkey-serialization).

## OperationManager.kt + OperationManagerImpl.kt + CoreModule.kt

Target paths: `.../core/foundation/internal/operation/OperationManager.kt`, `.../OperationManagerImpl.kt`, `.../core/foundation/CoreModule.kt`

```kotlin
// OperationManager.kt
package com.<org>.<product>.core.foundation.internal.operation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
internal interface OperationManager {
    fun launch(
        dispatcher: CoroutineDispatcher,
        onError: suspend (Throwable) -> Unit,
        block: suspend CoroutineScope.() -> Unit
    ): Job
    fun <T> whileActive(upstream: Flow<T>, activation: Flow<Boolean>): Flow<T>
}

// OperationManagerImpl.kt
package com.<org>.<product>.core.foundation.internal.operation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.koin.core.annotation.Factory
import org.koin.core.annotation.InjectedParam
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
@Factory(binds = [OperationManager::class])
internal class OperationManagerImpl(
    @InjectedParam val coroutineScope: CoroutineScope
) : OperationManager {
    override fun launch(
        dispatcher: CoroutineDispatcher,
        onError: suspend (Throwable) -> Unit,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        val handler = CoroutineExceptionHandler { _, t ->
            if (t !is CancellationException) {
                coroutineScope.launch {
                    onError(t)
                }
            }
        }
        return coroutineScope.launch(
            dispatcher + handler + SupervisorJob(coroutineScope.coroutineContext[Job])
        ) {
            supervisorScope { block() }
        }
    }
    override fun <T> whileActive(upstream: Flow<T>, activation: Flow<Boolean>): Flow<T> = activation
        .flatMapLatest { active ->
            if (active) flowOf(true) else flow { delay(1.seconds); emit(false) }
        }
        .distinctUntilChanged()
        .flatMapLatest { active -> if (active) upstream else emptyFlow() }
}

// CoreModule.kt
package com.<org>.<product>.core.foundation
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module
@Module
@ComponentScan
public class CoreModule
```

## CollectAsStateMultiplatform.kt (expect/actual — three source sets)

```kotlin
// commonMain — .../core/foundation/platform/CollectAsStateMultiplatform.kt
package com.<org>.<product>.core.foundation.platform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
@Composable
public expect fun <T> StateFlow<T>.collectAsStateMultiplatform(
    context: CoroutineContext = EmptyCoroutineContext,
): State<T>

// androidMain — .../CollectAsStateMultiplatform.android.kt
package com.<org>.<product>.core.foundation.platform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext
@Composable
public actual fun <T> StateFlow<T>.collectAsStateMultiplatform(context: CoroutineContext): State<T> =
    collectAsStateWithLifecycle(context = context)

// iosMain — .../CollectAsStateMultiplatform.ios.kt
package com.<org>.<product>.core.foundation.platform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.CoroutineContext
@Composable
public actual fun <T> StateFlow<T>.collectAsStateMultiplatform(context: CoroutineContext): State<T> =
    collectAsState(context)
```

## PlatformAnimation.kt (expect/actual — three source sets)

```kotlin
// commonMain — .../core/foundation/platform/PlatformAnimation.kt
package com.<org>.<product>.core.foundation.platform
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimator
public expect fun <C : Any, T : Any> platformAnimation(): StackAnimation<C, T>
public expect fun platformStackAnimator(): StackAnimator

// androidMain — .../PlatformAnimation.android.kt
package com.<org>.<product>.core.foundation.platform
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimator
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.stackAnimation
public actual fun <C : Any, T : Any> platformAnimation(): StackAnimation<C, T> =
    stackAnimation(animator = platformStackAnimator())
public actual fun platformStackAnimator(): StackAnimator = (fade() + slide())

// iosMain — .../PlatformAnimation.ios.kt
package com.<org>.<product>.core.foundation.platform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimation
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.StackAnimator
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.experimental.stack.animation.stackAnimator
import com.arkivanov.decompose.extensions.compose.stack.animation.isFront
public actual fun <C : Any, T : Any> platformAnimation(): StackAnimation<C, T> =
    stackAnimation(animator = platformStackAnimator())
public actual fun platformStackAnimator(): StackAnimator = iosLikeSlide()
private fun iosLikeSlide(animationSpec: FiniteAnimationSpec<Float> = tween()): StackAnimator =
    stackAnimator(animationSpec = animationSpec) { factor, direction ->
        Modifier
            .then(if (direction.isFront) Modifier else Modifier.fade(factor + 1F))
            .offsetXFactor(factor = if (direction.isFront) factor else factor * 0.5F)
    }
private fun Modifier.fade(factor: Float) =
    drawWithContent {
        drawContent()
        drawRect(color = Color(red = 0F, green = 0F, blue = 0F, alpha = (1F - factor) / 4F))
    }
private fun Modifier.offsetXFactor(factor: Float): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, placeable.height) {
            placeable.placeRelative(x = (placeable.width.toFloat() * factor).toInt(), y = 0)
        }
    }
```

> `ResultEmitter.kt` / `ResultManager.kt` source of truth and conventions: see `references/results.md`.
