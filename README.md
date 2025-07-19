# Grippo Mobile

<div align="center">
  <img src="https://img.shields.io/badge/Kotlin-1.9-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin 1.9">
  <img src="https://img.shields.io/badge/Jetpack%20Compose-1.5-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/KMM-Multiplatform-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="KMM">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/iOS-000000?style=for-the-badge&logo=ios&logoColor=white" alt="iOS">
</div>

<div align="center">
  <h3>A modern fitness tracking application built with Kotlin Multiplatform Mobile</h3>
</div>

## 📱 Overview

Grippo Mobile is a cutting-edge fitness tracking application that helps users monitor their workouts, track progress, and achieve their fitness goals. Built with Kotlin Multiplatform Mobile (KMM), the app delivers a native experience on both Android and iOS platforms while sharing business logic and UI components.

## 📚 Documentation

Comprehensive documentation is available in the `docs` directory:

| Document | Description |
|----------|-------------|
| [📖 Documentation Index](docs/index.md) | Start here for a complete overview of the documentation |
| [🏗️ Architecture Overview](docs/architecture-overview.md) | Learn about the MVVM architecture and component structure |
| [🚀 Setup and Installation](docs/installation.md) | Step-by-step instructions for setting up the development environment |
| [📦 Module Descriptions](docs/module-descriptions.md) | Detailed explanations of all modules and their relationships |
| [⚙️ Core Components](docs/core-components.md) | In-depth information about the core architectural components |
| [🎨 Design System](docs/design-system.md) | Comprehensive guide to the design system and UI components |

## 🏗️ Project Structure

The project follows a modular architecture organized by feature and layer:

### 📱 Platform-Specific

| Module | Description |
|--------|-------------|
| `androidApp/` | Android launcher application with platform-specific implementations |
| `iosApp/` | iOS application with SwiftUI wrapper around the shared code |

### 🧩 Core Modules

| Module | Description |
|--------|-------------|
| `shared/` | Main KMM module containing core application logic and UI components |
| `design-system/` | Shared design tokens, themes, and Compose UI components |
| `common/` | Utility modules for platform-specific helpers, validation, date utils, etc. |

### 📊 Data Layer

| Module | Description |
|--------|-------------|
| `data-services/` | Network (Ktor) and database (Room) infrastructure |
| `data-features/` | Business features exposing domain interfaces |
| `data-mappers/` | Mapping between network, database, and domain models |

### 🖼️ Presentation Layer

| Module | Description |
|--------|-------------|
| `presentation-features/` | UI screens and components built with Jetpack Compose |
| `dialog-features/` | Reusable dialog components (pickers, alerts, etc.) with stack-based navigation |
| `compose-libs/` | Standalone Compose libraries (segment control, wheel picker, etc.) |

The build is managed using Gradle Kotlin DSL with a version catalog defining consistent dependencies across all modules.

## ⚙️ Build Configuration

The project uses a centralized build configuration approach with shared Gradle scripts:

| Script | Purpose |
|--------|---------|
| **`android.gradle`** | Configures Android SDK version and Java compatibility |
| **`ios.gradle`** | Sets up CocoaPods integration for the shared module |
| **`kotlin.gradle`** | Applies KMM targets and opts-in to experimental APIs |
| **`compose.gradle`** | Enables Jetpack Compose features and optimizations |

Each module applies these shared scripts to ensure consistent configuration across the entire project.

## 🛠️ Technology Stack

The project leverages modern libraries and frameworks:

### UI & Navigation
- **[Jetpack Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform)** - Modern declarative UI framework
- **[Decompose](https://github.com/arkivanov/Decompose)** - Component-based navigation and lifecycle management

### Data & Networking
- **[Ktor](https://ktor.io/)** - Kotlin-first HTTP client with coroutines support
- **[Room](https://developer.android.com/training/data-storage/room)** - Database persistence with compile-time SQL validation

### Architecture & Utilities
- **[Koin](https://insert-koin.io/)** - Lightweight dependency injection framework
- **[Kotlinx Coroutines](https://github.com/Kotlin/kotlinx.coroutines)** - Asynchronous programming with coroutines
- **[Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)** - Type-safe JSON serialization
- **[Kotlinx DateTime](https://github.com/Kotlin/kotlinx-datetime)** - Cross-platform date/time library
- **[Kotlinx Immutable Collections](https://github.com/Kotlin/kotlinx.collections.immutable)** - Immutable collection types

All dependencies are managed through a version catalog in `gradle/libs.versions.toml` to ensure consistency.

## 🏛️ Architecture

The application follows a clean, modular architecture based on MVVM principles:

### 🧩 Core Components

| Component | Description |
|-----------|-------------|
| **BaseComponent** | Decompose component that manages lifecycle and connects to ViewModels |
| **BaseViewModel** | Provides state management, navigation events, and loading indicators |
| **Contracts** | Interfaces defining events that screens/ViewModels must implement |
| **Direction/Loader/State** | Sealed classes for navigation, loading states, and UI state |

### 🔄 Data Flow

<div align="center">
  <pre>
  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
  │    UI       │────▶│  ViewModel  │────▶│   Feature   │────▶│ Repository  │
  │  Components │◀────│             │◀────│  Interface  │◀────│             │
  └─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                                                                     │
                                                                     ▼
                                                              ┌─────────────┐
                                                              │  Network/   │
                                                              │  Database   │
                                                              └─────────────┘
  </pre>
</div>

### 💉 Dependency Injection

Koin is used for dependency injection throughout the application:

- Modules are defined in each feature
- `Koin.init` registers all modules from features and shared services
- Components and ViewModels access dependencies through the KoinComponent interface

### 🌐 Networking

The network layer is built with Ktor:

- `NetworkClient` wraps Ktor's `HttpClient` with JSON serialization and logging
- API endpoints are defined in a centralized API interface
- Repositories use the network client to make API calls and handle responses

### 💾 Database

Room provides local persistence:

- DAOs define database operations with compile-time SQL validation
- Repositories coordinate between network and database data sources
- Entities are mapped to domain models for use in the application

### 🎨 Design System

A comprehensive design system ensures consistent UI across the application:

- Shared themes, typography, and color palettes
- Reusable Compose UI components
- Design tokens for consistent spacing, sizing, and styling
- Preview annotations for rapid UI development

### 📱 Platform Integration

The architecture supports both Android and iOS platforms:

- Android uses Jetpack Compose directly
- iOS wraps Compose UI in SwiftUI components via UIViewControllers
- Business logic is shared across platforms through KMM

## 🔄 Application Flow

The application follows a structured flow of control:

<div align="center">
  <table>
    <tr>
      <th align="center">Stage</th>
      <th align="center">Description</th>
    </tr>
    <tr>
      <td align="center">🚀 <b>Initialization</b></td>
      <td>Android <code>App</code> or iOS <code>iOSApp</code> initializes Koin and launches the root component</td>
    </tr>
    <tr>
      <td align="center">🧭 <b>Navigation</b></td>
      <td>Decompose's <code>StackNavigation</code> manages screen transitions based on <code>Direction</code> events from ViewModels</td>
    </tr>
    <tr>
      <td align="center">📊 <b>Data Flow</b></td>
      <td>ViewModels request data from feature interfaces, which delegate to repositories combining local and remote data sources</td>
    </tr>
    <tr>
      <td align="center">🎨 <b>UI Rendering</b></td>
      <td>Compose functions in <code>Render</code> methods display state from ViewModels, with loading indicators for in-progress operations</td>
    </tr>
  </table>
</div>

## 🚀 Getting Started

### Building the Project

To build the entire project:

```bash
./gradlew assemble
```

### Running on Android

```bash
./gradlew :androidApp:installDebug
```

### Running on iOS

```bash
cd iosApp
pod install
open iosApp.xcworkspace
```

### Full Clean Build

If you encounter build issues, you can perform a full clean:

```bash
./gradlew clean --no-daemon
```

For a more thorough clean, see the detailed instructions in the [Setup and Installation](docs/installation.md) guide.

## 🌟 Key Features

- **Cross-Platform**: Single codebase for both Android and iOS platforms
- **Modern UI**: Fluid, responsive interface built with Jetpack Compose
- **Offline Support**: Local data persistence with Room database
- **Clean Architecture**: Modular design with clear separation of concerns
- **Reactive Design**: State-based UI with unidirectional data flow
- **Scalable**: Feature-based modules for easy expansion
- **Maintainable**: Comprehensive test coverage and documentation

## 👨‍💻 About the Developer

Grippo Mobile showcases my expertise in:

- Kotlin Multiplatform Mobile development
- Modern Android and iOS application architecture
- Clean, maintainable code organization
- UI/UX implementation with Jetpack Compose
- Asynchronous programming with Coroutines
- Dependency injection and modularization

This project demonstrates my ability to build complex, production-ready mobile applications using cutting-edge technologies and best practices.

## 📬 Contact

For questions, collaboration opportunities, or to discuss how I can contribute to your team:

- **Email**: [your.email@example.com](mailto:your.email@example.com)
- **LinkedIn**: [Your Name](https://linkedin.com/in/yourprofile)
- **GitHub**: [Your GitHub Profile](https://github.com/yourusername)

---

<div align="center">
  <p>Built with ❤️ using Kotlin Multiplatform Mobile</p>
</div>
