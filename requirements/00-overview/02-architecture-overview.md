# Architecture Overview

## Layers and dependency direction

```
:androidApp / :iosApp                       (thin shells, no business logic)
        ↓
     :shared                                 (composition root; includes every other module)
        ↓
┌───────────────────────────────────────────────────────────────────┐
│ :ui-screen-features:* → :ui-dialog-features:*                    │
│        ↓                       ↓                                 │
│ :ui-core:foundation, :ui-core:state, :ui-core:error              │
│        ↓                       ↓                                 │
│ :design-system:components → :design-system:core                  │
│                              ↓                                   │
│              :design-system:resources:provider                   │
└───────────────────────────────────────────────────────────────────┘
        ↓
:data-features:feature-api                   (the ONLY data-layer surface UI sees)
        ↓
:data-features:<feature>                     (Repository + FeatureImpl, internal)
        ↓
:data-services:{backend, database, datastore, firebase, *-auth}
        ↓
:toolkit:*                                   (http-client, logger, serialization, ...)
```

**Hard rules** (see `02-module-structure/02-dependency-rules.md` for the full list):

- A UI module **never** depends on `:data-services:*` directly. The boundary is `:data-features:feature-api`.
- `:data-features:feature-api` is pure interfaces + domain models. It does not depend on `:data-services:*`.
- `:toolkit:*` depends on nothing except other `:toolkit:*`. Two narrow, pure-type exceptions are tolerated: `:toolkit:http-client` → `:ui-core:error:error-provider` (so the response validator can throw `AppError` subtypes), and `:toolkit:date-utils` → `:design-system:{core, resources:provider}` (locale-aware formatting). Crash reporting via `:data-services:firebase` is wired through `:ui-core:foundation`, not toolkit. See `02-module-structure/02-dependency-rules.md`.
- `:design-system:*` does not touch the data layer.
- `:shared` is the only module that imports "everything".

## Module groups (high level)

| Group | Purpose | Examples |
|---|---|---|
| **App shells** | Platform entry points | `:androidApp`, `:iosApp` |
| **`:shared`** | Composition root + root navigator | `Koin.kt`, `RootComponent`, `RootViewModel`, `DialogComponent` |
| **Design system** | Tokens + atomic components | `:design-system:{core, components, preview, resources:*}` |
| **UI core** | Base classes + reusable state | `:ui-core:{foundation, state, error:*}` |
| **UI screen features** | Full-screen flows | `:ui-screen-features:{authorization, home, profile, ...}` |
| **UI dialog features** | Bottom-sheet flows | `:ui-dialog-features:{note-picker, confirmation, ...}` |
| **Data services** | Low-level I/O (HTTP, DB, DataStore, auth) | `:data-services:{backend, database, datastore, ...}` |
| **Data features** | Domain layer (interface + impl) | `:data-features:{feature-api, notes, user, ...}` |
| **Data mappers** | One module per direction | `:data-mappers:{dto-to-entity, entity-to-domain, ...}` |
| **Toolkit** | Platform-aware utilities | `:toolkit:{context, http-client, logger, ...}` |
| **Compose libs** | Reusable Compose widgets outside design system | `:compose-libs:{chart, konfetti, wheel-picker, ...}` |
| **Build logic** | Gradle convention plugins | `:build-logic:convention` |

## Cross-cutting patterns

### MVI (every screen and dialog)

Every screen/dialog is a package with **seven files** of the same template:

```
<feature>/
  <Name>Component.kt       // Decompose Component; owns ViewModel; handles Direction
  <Name>Contract.kt        // @Immutable interface with on*-callbacks + companion object Empty
  <Name>State.kt           // @Immutable data class | object | sealed interface
  <Name>Direction.kt       // sealed interface : BaseDirection (navigation intents)
  <Name>Loader.kt          // @Immutable sealed interface : BaseLoader (active operations)
  <Name>ViewModel.kt       // BaseViewModel<State, Direction, Loader>, implements Contract
  <Name>Screen.kt          // @Composable internal fun, takes (state, loaders, contract)
```

See `03-architecture-patterns/01-mvi-contract.md` and `04-base-classes/` for full details.

### Navigation (Decompose)

Type-safe per-feature `*Router` `sealed class` (`@Serializable`) in `:ui-screen-features:screen-api`. The root component holds a `StackNavigation<RootRouter>`; each feature module exposes a single root component with its own internal stack. See `03-architecture-patterns/02-decompose-navigation.md`.

### Dialogs

A separate `SlotNavigation<DialogConfig>` parallel to the screen stack, hosted by `DialogComponent` in `:shared`. Dialogs return values via **typed callbacks inside `DialogConfig`** (`@Transient`), not via `ResultManager`. See `03-architecture-patterns/03-dialog-navigation.md`.

### Cross-component results

For results that cannot be threaded as a callback (different stack branches, lifecycle-split initiator/consumer), `ResultManager` + `ResultKey` provide a typed channel-based mechanism. Action types live nested inside the producer screen's `Router.<Screen>.Action`. See `03-architecture-patterns/04-cross-component-results.md`.

### Data flow (UI → network)

```
ViewModel
  → <X>Feature (interface in :feature-api)
    → <X>FeatureImpl (in :data-features:<x>, @Single(binds=[Feature::class]))
      → <X>Repository (internal interface)
        → <X>RepositoryImpl (@Single(binds=[Repository::class]))
          → <Product>Api.<method>(body): Result<DTO>   // HTTP via :data-services:backend
          → <X>Dao.<query>(): Flow<Pack>            // Room via :data-services:database
```

**Observe** returns `Flow<Domain>` from DAO; **get/set/update/delete** returns `Result<T>`, hits the API, and reconciles the DAO on success. See `03-architecture-patterns/05-data-flow.md` and `03-architecture-patterns/06-repository-pattern.md`.

### Error pipeline

One path for all errors from any `safeLaunch`:

```
throw inside safeLaunch
  → operationManager catches via CoroutineExceptionHandler
  → BaseViewModel.sendError(exception, onError)
  → AppLogger.General.error(...)            // file log
  → FirebaseProvider.recordException(...)   // crashlytics
  → ErrorProvider.provide(exception, onError)
  → ErrorProviderImpl maps AppError → AppErrorState
  → DialogController.show(DialogConfig.ErrorDisplay(state, onClose = onError))
```

Manual `try/catch` inside a ViewModel is **forbidden** (exception: domain logic such as `result.onSuccess { ... }` after `api.call()`). See `03-architecture-patterns/07-error-pipeline.md`.

### Process-death restoration

Decompose `StateKeeper` serializes the router stack into `Bundle` (Android) / iOS state. Therefore every `*Router` sealed class **and** every payload inside it (`StageState`, etc.) **must be `@Serializable`**. See `03-architecture-patterns/08-process-death-restoration.md`.

## What this overview omits

This page is a map. Each chapter linked above contains the rules in detail, with verbatim API signatures, package paths, and code shape requirements.
