# `BaseComponent`

`BaseComponent` is the Decompose component base class. Every screen/dialog Component extends it. It:

- Delegates `ComponentContext` (Decompose's lifecycle + state-keeper + back-handler aggregate).
- Owns the `ViewModel` via `componentContext.retainedInstance { ... }` (survives configuration changes).
- Subscribes to `viewModel.navigator` and dispatches `Direction`s to `eventListener` on the component's lifecycle.
- Bridges the Decompose `Lifecycle` into the VM's `Flow<Boolean>` activation for `WhileActive` flows.
- Exposes `observeResult` / `sendResult` for cross-component results (delegates to `ResultManager`).

## Class signature

```kotlin
public abstract class BaseComponent<DIRECTION : BaseDirection>(
    componentContext: ComponentContext,
    private val identifier: ComponentIdentifier = NoneIdentifier,
) : ComponentContext by componentContext, KoinComponent
```

- Generic on the ViewModel's `DIRECTION` type (Component dispatches Directions; State/Loader live entirely inside the VM).
- `componentContext` is delegated, so the subclass can call `childContext`, `backHandler`, `stateKeeper`, etc. without prefixing.
- `identifier` (optional) — for unique identification across instances; rarely set explicitly.

## Required overrides

```kotlin
protected abstract val viewModel: BaseViewModel<*, DIRECTION, *>

protected abstract suspend fun eventListener(direction: DIRECTION)

@Composable
public abstract fun Render()
```

## Lifecycle wiring

```kotlin
init {
    lifecycle.doOnCreate {
        viewModel.navigator
            .onEach(::eventListener)
            .launchIn(coroutineScope)

        viewModel.attachActivation(lifecycle.asActiveFlow())
    }

    lifecycle.doOnDestroy {
        viewModel.detachActivation()
        resultManager.clear()
        coroutineScope.cancel()
    }
}
```

- On **create**, the Component starts collecting `viewModel.navigator` and pipes each `Direction` into `eventListener`. Also attaches the lifecycle's active-flow to the VM for `WhileActive` flows.
- On **destroy**, it detaches the activation flow, clears result subscriptions, and cancels its own scope. The VM's `onDestroy()` is called separately by Decompose's `InstanceKeeper`.

## `coroutineScope` (internal)

The Component holds its own `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`. This scope is **not** the VM's scope; the Component scope is used only for navigator collection and result subscriptions.

## `eventListener`

```kotlin
protected abstract suspend fun eventListener(direction: DIRECTION)
```

The conventional shape:

```kotlin
override suspend fun eventListener(direction: ProfileBodyDirection) {
    when (direction) {
        ProfileBodyDirection.Back -> back()
        ProfileBodyDirection.OpenSettings -> toSettings()
        is ProfileBodyDirection.OpenWorkoutHistory -> toWorkoutHistory(direction.initialRange)
    }
}
```

- `when (direction) { ... }` over every `Direction` subtype — the compiler enforces exhaustiveness because `sealed interface`.
- Each branch maps to a constructor-injected lambda (`back`, `toSettings`, `toWorkoutHistory`) **or** to a nav operation on this Component's own `StackNavigation` (if it's a root-of-feature component owning a stack).
- `eventListener` is `suspend` — you can call `dialogController.show(...)` or any suspend function inside, though most cases just call a lambda.

## `Render`

```kotlin
@Composable
public abstract fun Render()
```

Idiomatic body:

```kotlin
@Composable
override fun Render() {
    val state = viewModel.state.collectAsStateMultiplatform()
    val loaders = viewModel.loaders.collectAsStateMultiplatform()
    ProfileBodyScreen(state.value, loaders.value, viewModel)
}
```

- Use `collectAsStateMultiplatform()` (not `collectAsState`): on Android it uses `collectAsStateWithLifecycle` (lifecycle-aware); on iOS it falls back to `collectAsState`. Defined in `:ui-core:foundation/platform`.
- The Screen function receives `state.value`, `loaders.value`, and `viewModel` (since the VM implements the Contract).

For Components that own a `ChildStack`/`ChildSlot` (e.g. `RootComponent`, `*RootComponent`), `Render()` also collects the stack/slot and renders children:

```kotlin
@Composable
override fun Render() {
    val state = viewModel.state.collectAsStateMultiplatform()
    ProfileRootScreen(state.value, stack = childStack, /* ... */)
}
```

## ViewModel creation

```kotlin
override val viewModel: ProfileBodyViewModel = componentContext.retainedInstance {
    ProfileBodyViewModel(
        userFeature = getKoin().get(),
        weightHistoryFeature = getKoin().get(),
        dialogController = getKoin().get(),
    )
}
```

- `componentContext.retainedInstance { ... }` is a Decompose helper that creates the VM the first time and keeps it across configuration changes (rotation/etc.) until the Component is destroyed.
- Dependencies are pulled via `getKoin().get()` — **not** threaded through the Component constructor. The Component's constructor takes only `componentContext` + navigation lambdas (`back`, `toX`).
- This is the **idiomatic** DI pattern for the project. Constructor-injecting Features into the Component would require every parent to know every grandchild's deps, which is unwieldy at the root-component scale.

## Constructor parameters

A Component's constructor takes **only**:

1. `componentContext: ComponentContext` — Decompose handle.
2. **Navigation callbacks** — one lambda per direction this Component cannot resolve itself: `back: () -> Unit`, `close: () -> Unit`, `toX: (params) -> Unit`.
3. **Initial route parameters** if the parent route carries them: `initialRange: DateRange`, `userId: String`.

Forbidden in constructor params: Feature interfaces, UseCases, DialogControllers, Stub repositories. Those come from Koin via `retainedInstance { getKoin().get() }`.

## Result observation

```kotlin
public abstract class BaseComponent<DIRECTION : BaseDirection>(...) {

    protected fun <T : BaseResult> observeResult(
        key: ResultKey<T>,
        onResult: suspend (T) -> Unit,
    ) { resultManager.observeResult(key, onResult) }

    protected fun <T : Any> sendResult(key: ResultKey<Result<T>>, data: T) {
        resultManager.sendResult(key, data)
    }
}
```

Usage:

```kotlin
internal class TrainingRootComponent(...) : BaseComponent<TrainingDirection>(...) {

    init {
        observeResult<Result<TrainingRouter.Exercise.Action>>(
            key = ResultKeys.create("exercise"),
            onResult = { result ->
                when (val action = result.data) {
                    is TrainingRouter.Exercise.Action.Sync -> viewModel.updateExercise(action.exercise)
                    is TrainingRouter.Exercise.Action.Remove -> viewModel.removeExercise(action.id)
                }
            }
        )
    }
}
```

See `03-architecture-patterns/04-cross-component-results.md`.

## Back handling

Default back goes to `BackHandler`. To register a custom callback:

```kotlin
init {
    backHandler.register(BackCallback(onBack = viewModel::onBack))
}
```

`viewModel.onBack()` (a Contract method) can update state, dispatch analytics, then call `navigateTo(Direction.Back)`.

## What a Component MUST NOT do

- Render UI directly. `Render()` calls a `<X>Screen` function; the function lives in `<X>Screen.kt`.
- Hold state. The VM holds state.
- Call `<X>Feature` / `<X>UseCase`. The VM does.
- Cache `getKoin().get()` results in fields. Pull only inside `retainedInstance { ... }`.
- Subscribe to long-lived flows. The VM does (via `safeLaunch`).
- Mutate the router stack of a parent. Use a constructor-injected lambda.

## Minimal Component template

```kotlin
internal class FooComponent(
    componentContext: ComponentContext,
    private val back: () -> Unit,
    private val toBar: (String) -> Unit,
) : BaseComponent<FooDirection>(componentContext) {

    override val viewModel: FooViewModel = componentContext.retainedInstance {
        FooViewModel(
            barFeature = getKoin().get(),
        )
    }

    override suspend fun eventListener(direction: FooDirection) {
        when (direction) {
            FooDirection.Back -> back()
            is FooDirection.OpenBar -> toBar(direction.id)
        }
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        FooScreen(state.value, loaders.value, viewModel)
    }
}
```

## Root components with their own stack

A `*RootComponent` for a feature also owns a private `StackNavigation`:

```kotlin
internal class ProfileRootComponent(
    componentContext: ComponentContext,
    initialRoute: ProfileRouter,
    private val back: () -> Unit,
) : BaseComponent<ProfileRootDirection>(componentContext) {

    private val navigation = StackNavigation<ProfileRouter>()

    private val stack: Value<ChildStack<ProfileRouter, Child>> = childStack(
        source = navigation,
        serializer = ProfileRouter.serializer(),
        initialConfiguration = initialRoute,
        key = "ProfileRootComponent",
        childFactory = ::createChild,
    )

    override val viewModel = componentContext.retainedInstance { ProfileRootViewModel() }

    private fun createChild(config: ProfileRouter, ctx: ComponentContext): Child = when (config) {
        ProfileRouter.Body -> Child.Body(
            ProfileBodyComponent(ctx, back = { navigation.pop() }, toSettings = { navigation.push(ProfileRouter.Settings) })
        )
        ProfileRouter.Settings -> Child.Settings(ProfileSettingsComponent(ctx, back = { navigation.pop() }))
        is ProfileRouter.WorkoutHistory -> Child.WorkoutHistory(
            WorkoutHistoryComponent(ctx, initialRange = config.initialRange, back = { navigation.pop() })
        )
    }

    sealed class Child(open val component: BaseComponent<*>) {
        data class Body(override val component: ProfileBodyComponent) : Child(component)
        data class Settings(override val component: ProfileSettingsComponent) : Child(component)
        data class WorkoutHistory(override val component: WorkoutHistoryComponent) : Child(component)
    }

    override suspend fun eventListener(direction: ProfileRootDirection) {
        when (direction) {
            ProfileRootDirection.Back -> {
                if (!navigation.popOrNull()) back()
            }
        }
    }

    @Composable
    override fun Render() {
        ChildStack(stack = stack, animation = stackAnimation(platformStackAnimator())) { child ->
            child.instance.component.Render()
        }
    }
}
```

The `*RootComponent` is the only component in a feature that has a stack; sub-components do not.
