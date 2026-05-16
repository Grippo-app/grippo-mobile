# `BaseComponent`

> **Illustrative domain.** Code blocks below use `Note` / `Tag` / `User` as the generic `<Entity>` / `<RelatedEntity>` for examples. Substitute identifiers from your product domain.

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

The conventional shape (real example — `LoginComponent` in `:ui-screen-features:authorization`):

```kotlin
override suspend fun eventListener(direction: LoginDirection) {
    when (direction) {
        is LoginDirection.Registration -> toRegistration(direction.email)
        LoginDirection.Home -> toHome()
        LoginDirection.CreateProfile -> toCreateProfile()
        LoginDirection.Back -> back()
    }
}
```

- `when (direction) { ... }` over every `Direction` subtype — the compiler enforces exhaustiveness because `sealed interface`.
- Each branch maps to a constructor-injected lambda (`back`, `toRegistration`, `toHome`, `toCreateProfile`) **or** to a nav operation on this Component's own `StackNavigation` (if it's a stack-owning feature root like `NotesComponent`, where `eventListener` calls `navigation.push(...)` / `navigation.replaceAll(...)` / `navigation.pop()`).
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
    NoteDetailScreen(state.value, loaders.value, viewModel)
}
```

- Use `collectAsStateMultiplatform()` (not `collectAsState`): on Android it uses `collectAsStateWithLifecycle` (lifecycle-aware); on iOS it falls back to `collectAsState`. Defined in `:ui-core:foundation/platform`.
- The Screen function receives `state.value`, `loaders.value`, and `viewModel` (since the VM implements the Contract).

For Components that own a `ChildStack`/`ChildSlot` (e.g. `RootComponent`, a bare-name feature root like `ProfileComponent`, or a `Root`-suffixed root like `NotesRootComponent` when the first sub-screen would collide with the feature name), `Render()` also collects the stack/slot and delegates rendering to its feature-root Screen. The Screen receives `this` (the component) so it can read `component.childStack` and pass it to `ChildStack(...)`:

```kotlin
@Composable
override fun Render() {
    val state = viewModel.state.collectAsStateMultiplatform()
    val loaders = viewModel.loaders.collectAsStateMultiplatform()
    ProfileScreen(this, state.value, loaders.value, viewModel)
}
```

## ViewModel creation

```kotlin
override val viewModel = componentContext.retainedInstance {
    NoteDetailViewModel(
        dialogController = getKoin().get(),
        noteFeature = getKoin().get(),
        userFeature = getKoin().get(),
        updateNoteUseCase = getKoin().get(),
        stringProvider = getKoin().get(),
        notificationManager = getKoin().get(),
    )
}
```

- `componentContext.retainedInstance { ... }` is a Decompose helper that creates the VM the first time and keeps it across configuration changes (rotation/etc.) until the Component is destroyed.
- Dependencies are pulled via `getKoin().get()` — **not** threaded through the Component constructor. The Component's constructor takes only `componentContext` + navigation lambdas (`back`, `toX`) + initial route params.
- The `override val viewModel` declaration usually omits the type annotation; the type is inferred from `retainedInstance { ... }`. `BaseComponent.viewModel` is `protected abstract val viewModel: BaseViewModel<*, DIRECTION, *>`, and the inferred subtype refines it.
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
internal class NoteEditorComponent(...) : BaseComponent<NoteEditorDirection>(...) {

    init {
        observeResult<Result<NotesRouter.Tag.Action>>(
            key = ResultKeys.create("tag"),
            onResult = {
                when (val action = it.data) {
                    is NotesRouter.Tag.Action.Sync -> viewModel.updateTag(action.tag)
                    is NotesRouter.Tag.Action.Remove -> viewModel.onDeleteTag(action.id)
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

A feature's stack-owning Component owns a private `StackNavigation` and a `Value<ChildStack<<X>Router, Child>>`. Naming follows the bare-feature-name convention by default (e.g. `ProfileComponent` for `:profile`). The `<Feature>RootComponent` suffix is reserved for features whose first sub-screen would collide with the feature name — e.g. `NotesRootComponent` for `:notes`, whose first sub-screen is `NotesComponent`. A single-screen feature (no inner stack) has no Root suffix. Stack-owning feature-root Components are **`public class`** (they're constructed from `:shared`, across module boundaries):

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
        when (direction) {
            ProfileDirection.Back -> close.invoke()
        }
    }

    private val navigation = StackNavigation<ProfileRouter>()

    internal val childStack: Value<ChildStack<ProfileRouter, Child>> = childStack(
        source = navigation,
        serializer = ProfileRouter.serializer(),
        initialStack = { listOf(initial) },
        key = "ProfileComponent",
        handleBackButton = true,
        childFactory = ::createChild,
    )

    private fun createChild(router: ProfileRouter, context: ComponentContext): Child = when (router) {
        ProfileRouter.Overview -> Child.Overview(
            ProfileOverviewComponent(componentContext = context, back = viewModel::onBack)
        )
        ProfileRouter.Settings -> Child.Settings(
            ProfileSettingsComponent(componentContext = context, back = viewModel::onBack)
        )
        // ... other routes
    }

    @Composable
    override fun Render() {
        val state = viewModel.state.collectAsStateMultiplatform()
        val loaders = viewModel.loaders.collectAsStateMultiplatform()
        ProfileScreen(this, state.value, loaders.value, viewModel)
    }

    internal sealed class Child(open val component: BaseComponent<*>) {
        data class Overview(override val component: ProfileOverviewComponent) : Child(component)
        data class Settings(override val component: ProfileSettingsComponent) : Child(component)
        // ... one entry per route
    }
}
```

Notes:

- The Component is `public class` because it is instantiated from `:shared`.
- Pop / back is wired through `BackCallback(onBack = viewModel::onBack)` + `backHandler.register(...)`, not by inspecting `navigation.popOrNull()` inside `eventListener`. The VM owns the back semantics.
- `Render()` typically delegates to a `<Feature>Screen(this, state, loaders, contract)` that calls `ChildStack(stack = component.childStack, ...)` — the stack rendering lives in the Screen, not in `Render()` directly.
- This stack-owning Component is the only component in a feature that has a stack; sub-components do not.
