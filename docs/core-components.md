# Core Components

This document describes the core components of the Grippo Mobile application architecture.

## BaseComponent

The `BaseComponent` class is the foundation for all UI components in the application. It integrates with Decompose for lifecycle management and navigation.

### Purpose

- Provide a consistent lifecycle management for all components
- Handle navigation events from ViewModels
- Manage coroutine scopes tied to component lifecycle
- Integrate with Koin for dependency injection

### Key Features

The BaseComponent class:
- Takes a ComponentContext from Decompose
- Creates a coroutine scope for async operations
- Connects to a ViewModel for state and events
- Handles lifecycle events (create, start, stop, destroy)
- Listens to navigation events from the ViewModel
- Provides an abstract Render method for UI

```kotlin
public abstract class BaseComponent<DIRECTION : BaseDirection>(
    componentContext: ComponentContext,
    private val identifier: ComponentIdentifier = NoneIdentifier,
) : ComponentContext by componentContext, KoinComponent {
    // Lifecycle management and navigation handling
    protected abstract val viewModel: BaseViewModel<*, DIRECTION, *>
    protected abstract suspend fun eventListener(direction: DIRECTION)

    @Composable
    public abstract fun Render()
}
```

### Usage Pattern

To create a new component:

1. Extend `BaseComponent` with your direction type
2. Provide a ViewModel instance
3. Implement the eventListener to handle navigation
4. Implement the Render method to display UI

```kotlin
class FeatureComponent(
    componentContext: ComponentContext,
    // Dependencies
) : BaseComponent<FeatureDirection>(componentContext) {
    // Implementation
}
```

## BaseViewModel

The `BaseViewModel` class provides core functionality for all ViewModels in the application.

### Purpose

- Manage UI state
- Handle navigation events
- Track loading states
- Provide error handling
- Manage coroutine scopes

### Key Features

The BaseViewModel class:
- Manages UI state using StateFlow
- Provides navigation through a Channel
- Tracks loading states for UI feedback
- Handles errors with ErrorProvider
- Manages coroutine scope for async operations
- Provides helper methods for safe coroutine launches

```kotlin
public abstract class BaseViewModel<STATE, DIRECTION : BaseDirection, LOADER : BaseLoader>(
    state: STATE,
) : InstanceKeeper.Instance, KoinComponent {
    // State management
    public val state: StateFlow<STATE>

    // Navigation
    public val navigator: Flow<DIRECTION>

    // Loading state
    public val loaders: StateFlow<ImmutableSet<LOADER>>

    // Helper methods
    protected fun update(updateFunc: (STATE) -> STATE)
    protected fun navigateTo(destination: DIRECTION)
    protected fun safeLaunch(/* parameters */)
}
```

### Usage Pattern

To create a new ViewModel:

1. Extend `BaseViewModel` with your state, direction, and loader types
2. Initialize with a default state
3. Implement methods to handle user events
4. Use safeLaunch for async operations
5. Update state and navigate as needed

```kotlin
class FeatureViewModel(
    // Dependencies
) : BaseViewModel<FeatureState, FeatureDirection, FeatureLoader>(
    state = FeatureState()
) {
    // Implementation
}
```

## Direction, Loader, and State

Each feature typically defines three key types:

### Direction

A sealed class representing navigation events:

```kotlin
sealed class MyDirection : BaseDirection {
    object Back : MyDirection()
    data class Forward(val id: String) : MyDirection()
}
```

### Loader

A sealed class representing loading states:

```kotlin
sealed class MyLoader : BaseLoader {
    object Data : MyLoader()
    object Refresh : MyLoader()
}
```

### State

A data class representing the UI state:

```kotlin
data class MyState(
    val data: List<Item> = emptyList(),
    val selectedItem: Item? = null
)
```

## Component Lifecycle

The component lifecycle follows Decompose's lifecycle:

1. **onCreate**: Component is created, ViewModel is initialized, and navigation subscription is set up
2. **onStart**: Component becomes visible
3. **onStop**: Component becomes invisible
4. **onDestroy**: Component is destroyed, coroutine scope is canceled

## ViewModel Lifecycle

The ViewModel lifecycle is managed by Decompose's InstanceKeeper:

1. **Initialization**: Initial state is set, and any initial data loading is started
2. **Active**: ViewModel processes events and updates state
3. **onDestroy**: ViewModel is destroyed, coroutine scope is canceled

## Error Handling

Errors are handled through the `ErrorProvider` service:

1. Exceptions in coroutines are caught by `CoroutineExceptionHandler`
2. Errors are logged and passed to `ErrorProvider`
3. `ErrorProvider` determines how to display the error to the user