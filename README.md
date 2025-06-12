# Grippo Mobile Technical Overview

This document provides a high level walkthrough of the Grippo Mobile project. The repository implements a Kotlin Multiplatform Mobile (KMM) application targeting Android and iOS.

## Project Structure

The root project contains multiple Gradle modules organised by feature and layer:

- `androidApp/` – Android launcher application.
- `iosApp/` – SwiftUI wrapper around the shared code.
- `shared/` – Kotlin Multiplatform module containing the core application logic and UI components.
- `data-services/` – Networking and database layers (Ktor + Room).
- `data-features/` – Business features exposing domain interfaces.
- `data-mappers/` – Mapping between network, database and domain models.
- `presentation-features/` – Presentation layer components built with Jetpack Compose.
- `dialog-features/` – Reusable dialog components (weight picker, error dialog, …).
- `design-system/` – Shared design tokens and Compose UI widgets.
- `common/` – Utility modules (platform specific helpers, validation, date utils, …).
- `compose-libs/` – Standalone Compose libraries (segment control, wheel picker, …).

The build is managed using Gradle Kotlin DSL with a [version catalog](../gradle/libs.versions.toml) defining plugin and library versions.

## Build Configuration

Common Gradle configuration files can be found under [`gradle/common`](../gradle/common):

- **`android.gradle`** configures the Android SDK version and Java compatibility.
- **`ios.gradle`** sets up the CocoaPods integration for the shared module.
- **`kotlin.gradle`** applies the KMM targets and opts‑in to experimental APIs.
- **`compose.gradle`** enables Jetpack Compose features and strong skipping mode.

Each module applies these shared scripts to keep configuration consistent.

## Core Libraries

- **Jetpack Compose Multiplatform** – UI framework used in all modules.
- **Decompose** – Navigation and lifecycle management across Android and iOS.
- **Koin** – Dependency injection framework (see [`Koin.kt`](../shared/src/commonMain/kotlin/com/grippo/shared/Koin.kt)).
- **Ktor** – HTTP client with logging and JSON support ([`NetworkClient`](../data-services/network/src/commonMain/kotlin/com/grippo/network/client/NetworkClient.kt)).
- **Room** – Database layer for both platforms (configured in [`database/build.gradle.kts`](../data-services/database/build.gradle.kts)).
- **Kotlinx Coroutines** – Asynchronous programming throughout the project.
- **Kotlinx Serialization / DateTime / Immutable Collections** – Utility libraries from KotlinX.

The version catalog in `gradle/libs.versions.toml` lists all versions and coordinates.

## Architectural Patterns

The application uses a layered architecture following MVVM principles:

1. **BaseComponent** – Decompose component that wires lifecycle to a ViewModel. See [`BaseComponent`](../common/core/src/commonMain/kotlin/com/grippo/core/BaseComponent.kt).
2. **BaseViewModel** – Provides state, navigation events and loader management. ViewModels extend this class ([`BaseViewModel`](../common/core/src/commonMain/kotlin/com/grippo/core/BaseViewModel.kt)).
3. **Contracts** – Interfaces defining events that a screen/view model must implement. Example: [`RootContract`](../shared/src/commonMain/kotlin/com/grippo/shared/root/RootContract.kt).
4. **Direction / Loader / State** – For each feature there are sealed classes representing navigation directions, loading states and UI state.

Navigation is handled by Decompose’s `StackNavigation` and Compose is used for all UI rendering.

### Dependency Injection

`Koin.init` (in [`shared/Koin.kt`](../shared/src/commonMain/kotlin/com/grippo/shared/Koin.kt)) registers modules from each feature and shared service. Android initialises Koin in [`App.kt`](../androidApp/src/main/java/com/grippo/android/App.kt) while iOS does so in [`iOSApp.swift`](../iosApp/iosApp/iOSApp.swift).

### Networking and Database

- The [`NetworkClient`](../data-services/network/src/commonMain/kotlin/com/grippo/network/client/NetworkClient.kt) wraps a Ktor `HttpClient` configured with JSON content negotiation and logging.
- API calls are defined in [`Api.kt`](../data-services/network/src/commonMain/kotlin/com/grippo/network/Api.kt).
- The Room database module defines DAOs such as [`TokenDao`](../data-services/database/src/commonMain/kotlin/com/grippo/database/dao/TokenDao.kt) which are used by repositories (e.g. [`AuthorizationRepositoryImpl`](../data-features/authorization/src/commonMain/kotlin/com/grippo/data.features.authorization/data/AuthorizationRepositoryImpl.kt)).

### Feature Modules

Each feature resides in its own module combining repository, domain interface and presentation components. For example the **Authorization** feature exposes `AuthorizationFeature` and implements it using a repository connected to the network and database layers.

### Shared UI and Design System

The design system module provides Compose themes, typography and reusable widgets. An example widget is `UserCard` ([file](../design-system/components/src/commonMain/kotlin/com/grippo/design.components/user/UserCard.kt)). UI previews rely on the `AppPreview` annotation within the design system.

### iOS Integration

The iOS application uses SwiftUI as an entry point and hosts the Compose UI inside a `UIViewController` (`RootView` and `AppDelegate`). Koin is initialised in `iOSApp` before presenting the root component.

## Flow of Control

1. **Application start** – Android `App` or iOS `iOSApp` initialises Koin and launches the root component.
2. **Navigation** – Decompose’s `StackNavigation` manages the `RootComponent` stack with screens such as authorization and home. View models send `Direction` events which components react to by pushing or popping navigation stacks.
3. **Data Fetching** – View models call into feature interfaces which delegate work to repositories. Repositories combine local Room storage and Ktor network calls and emit results to the presentation layer.
4. **UI Rendering** – Compose functions inside `Render` methods display state provided by the view models. Loaders emitted by `BaseViewModel` indicate in‑progress operations.

## How to Build

```bash
./gradlew assemble
```

For a full clean

```bash
./gradlew clean
cd iosApp
rm -rf iosApp/Pods iosApp/Podfile.lock build
rm -rf ~/Library/Developer/Xcode/DerivedData
pod deintegrate
pod install
```

## Summary

Grippo Mobile is a Kotlin Multiplatform application applying MVVM, Decompose and Koin to share code across Android and iOS. Networking is powered by Ktor and persistence by Room. The design system exposes Compose components reused throughout the project, while feature and service modules keep business logic isolated and testable.