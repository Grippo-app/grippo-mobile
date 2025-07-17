# Architecture Overview

## Introduction

Grippo Mobile is built as a Kotlin Multiplatform Mobile (KMM) application that shares code between Android and iOS platforms. The architecture follows MVVM (Model-View-ViewModel) principles with a component-based approach using Decompose for navigation and lifecycle management.

## Architectural Layers

The application is organized into the following layers:

### Presentation Layer
- **Components**: Decompose components that manage lifecycle and navigation
- **ViewModels**: Handle business logic and state management
- **UI**: Jetpack Compose UI components for rendering

### Domain Layer
- **Features**: Business logic interfaces exposed to the presentation layer
- **Models**: Domain models representing business entities

### Data Layer
- **Repositories**: Implement data access logic
- **Network**: API communication using Ktor
- **Database**: Local storage using Room

## Key Components

### BaseComponent

The `BaseComponent` class is the foundation for all UI components:

```kotlin
public abstract class BaseComponent<DIRECTION : BaseDirection>(
    componentContext: ComponentContext,
    private val identifier: ComponentIdentifier = NoneIdentifier,
) : ComponentContext by componentContext, KoinComponent {
    // Implementation details
}
```

BaseComponent handles:
- Lifecycle management
- Navigation events
- ViewModel integration
- Coroutine scope management

### BaseViewModel

The `BaseViewModel` class provides core functionality for all ViewModels:

```kotlin
public abstract class BaseViewModel<STATE, DIRECTION : BaseDirection, LOADER : BaseLoader>(
    state: STATE,
) : InstanceKeeper.Instance, KoinComponent {
    // Implementation details
}
```

BaseViewModel handles:
- State management
- Navigation events
- Loading state
- Error handling
- Coroutine scope management

## Navigation Flow

Navigation is handled using Decompose's `StackNavigation`:

1. ViewModels emit `Direction` events through the navigator channel
2. Components listen to these events and update the navigation stack
3. New components are created and rendered based on the navigation state

## Dependency Injection

Koin is used for dependency injection throughout the application:

1. Modules are defined in each feature
2. The `Koin.init` function in `shared/Koin.kt` registers all modules
3. Components and ViewModels use the `KoinComponent` interface to access dependencies

## Data Flow

1. User interactions trigger events in the UI
2. Events are handled by ViewModels
3. ViewModels call into feature interfaces
4. Features delegate to repositories
5. Repositories access network or database services
6. Results flow back up through the layers
7. ViewModels update state
8. UI recomposes based on the new state

## Error Handling

Errors are handled through:
1. CoroutineExceptionHandler in ViewModels
2. ErrorProvider service for displaying errors to users
3. Result type for handling expected errors

## Concurrency

Coroutines are used for asynchronous operations:
1. ViewModels create a CoroutineScope with SupervisorJob
2. The scope is canceled when the ViewModel is destroyed
3. safeLaunch extensions provide structured concurrency with error handling