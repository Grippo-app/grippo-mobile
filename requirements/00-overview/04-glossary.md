# Glossary

## Architecture terms

- **App shell** — the platform-specific entry point (`:androidApp`, `:iosApp`). Holds no business logic; only sets up DI, launches the root `Component`, and wires Firebase (Android: in `Application.onCreate`; iOS: in `AppDelegate.application(_:didFinishLaunchingWithOptions:)`). Deeplinks from the launch intent and the splash screen are wired Android-only.
- **Composition root** — `:shared`. The only module that depends on every other module. Hosts `Koin.init()`, `RootComponent`, `DialogComponent`.
- **Component** — a Decompose `BaseComponent<DIRECTION>` subclass. Owns its `ViewModel` (via `retainedInstance`), subscribes to `navigator`, handles `Direction` events. Created by the parent component, which threads `componentContext` and callback lambdas through the constructor.
- **ViewModel** — a `BaseViewModel<STATE, DIRECTION, LOADER>` subclass. Implements the `Contract`. The only place that mutates state (`update { ... }`) and emits `Direction`s (`navigateTo(...)`).
- **Screen** — a `@Composable internal fun <Name>Screen(state, loaders, contract)`. Stateless: takes the rendered state and a contract of callbacks. Lives at the bottom of the seven-file pattern.
- **Contract** — `@Immutable internal interface <Name>Contract` listing all UI callbacks (`onApplyClick()`, `onBack()`, ...). Has a `companion object Empty : <Name>Contract { ... }` no-op for previews.
- **State** — `@Immutable internal data class <Name>State(...)` (or `data object`, or `sealed interface`) holding the current rendered state. Uses `UiText`, immutable collections, `*FormatState`.
- **Direction** — `internal sealed interface <Name>Direction : BaseDirection`. Navigation intents emitted by the ViewModel (`Back`, `OpenProfile`, ...). Translated to actual navigation in `Component.eventListener`.
- **Loader** — `@Immutable internal sealed interface <Name>Loader : BaseLoader`. Tags for active async operations (button spinners, skeletons). Multiple loaders can be active concurrently (`loaders: StateFlow<ImmutableSet<LOADER>>`).
- **Router** — a `@Serializable public sealed class <Feature>Router : BaseRouter` in `:ui-screen-features:screen-api`. Defines all screens **within** a feature plus inter-feature entry points.
- **Feature** — `:data-features:<x>`'s `<X>Feature` interface (in `:feature-api`). The UI-visible facet of a domain area. Implementations are `internal`; consumers see only the interface + domain models.
- **Repository** — `internal interface <X>Repository` inside `:data-features:<x>`. The Feature's data source: combines `GrippoApi` calls with DAO reads. Returns `Flow<Domain>` for observe, `Result<T>` for mutate.
- **Pack** — a `data class <X>Pack` with `@Embedded` entity + one or more `@Relation` fields. Used as the read-side DAO return type for tree-shaped queries.
- **DialogConfig** — a `@Serializable sealed class DialogConfig` subtype identifying a bottom sheet, its inputs, and its result callback (`@Transient`). Configs are shown via `DialogController.show(config)`.
- **DialogController** — the singleton that bridges any ViewModel to the `DialogComponent`'s slot navigator.
- **ResultKey<T : BaseResult>** — a typed key for cross-component results. Producer calls `sendResult(key, data)`; consumer subscribes via `observeResult(key) { ... }`.
- **Deeplink** — `enum class Deeplink(val key: String)`. Parsed from a launch intent (Android) / push payload, translated into a `Direction` by `RootViewModel`.

## Layering vocabulary

- **DTO** — Data Transfer Object. `@Serializable data class <Name>Response` or `<Name>Body` in `:data-services:backend/dto/<area>`. **All fields nullable + default `= null`.**
- **Entity** — Room `@Entity data class <Name>Entity` in `:data-services:database/entity`. Non-null fields, snake-case table names, indices and foreign keys declared explicitly.
- **Domain model** — `data class <Name>` in `:data-features:feature-api`. Non-null fields, no platform types, no `@Serializable`.
- **State** — UI state, lives in the screen package (`:ui-screen-features:<feature>` or `:ui-dialog-features:<feature>`). `@Immutable`, uses `UiText` instead of `String` where the value depends on resources.
- **Body** — `@Serializable data class <Name>Body` — POST/PUT request bodies. Same DTO conventions.
- **Domain → DTO direction**: `Domain.toBody(): <Name>Body` in `:data-mappers:domain-to-dto`.

## Cross-cutting types

- **`AppLogger`** — singleton with `General`, `Navigation`, `Network`, `Mapping` log categories; writes to a single append-only file sink. Path resolution: Android — `${user.home}/<product>/logs/app.log` (falls back to `java.io.tmpdir`, then `/tmp`); iOS — `NSTemporaryDirectory()/<product>/logs/app.log`. No rotation; cleared via `AppLogger.clearLogFile()`.
- **`AppTokens`** — facade `@Stable public object AppTokens` exposing `colors`, `icons`, `typography`, `strings`, `drawables`, `dp` as `@Composable @ReadOnlyComposable` properties backed by `CompositionLocal`.
- **`AppTheme`** — `@Composable public fun AppTheme(darkTheme, localeTag, content)` providing every `Local*` `CompositionLocal`. The `Local*` definitions are `internal` to `:design-system:core`; the types they expose (`AppColor`, `AppDp`, ...) live in `:design-system:resources:provider`.
- **`StringProvider`** — interface with `suspend fun get(StringResource, vararg Any): String`. Used in ViewModels (non-Composable) where `AppTokens.strings` is unavailable.
- **`UiText`** — `sealed interface UiText { Res(StringResource, formatArgs), Str(String) }` with both `@Composable fun text()` and `suspend fun text(StringProvider)`.
- **`*FormatState`** — sealed classes `Empty`/`Invalid`/`Valid` for form-field state. Form-style validators: `EmailFormatState`, `PasswordFormatState`, `NameFormatState`. Numeric units: `WeightFormatState`, `HeightFormatState`, `DurationFormatState`, `VolumeFormatState`, `PercentageFormatState`, `IntensityFormatState`, `DensityFormatState`, `MultiplierFormatState`, `RepetitionsFormatState`. Date/time: `DateFormatState`, `DateTimeFormatState`, `DateRangeFormatState`. See `11-state-and-formatters/`.
- **`AppError`** — sealed hierarchy in `:ui-core:error:error-provider`. Subtypes: `Network.{NoInternet, Timeout, Expected, Unexpected}`, `Expected`, `Unknown`. Mapped by `ErrorProviderImpl` to `AppErrorState`, then surfaced as `DialogConfig.ErrorDisplay`.
- **`DateTimeUtils`** / **`DateRange`** / **`DateRangeKind`** / **`DateRangePresets`** / **`DateFormat`** / **`DateFormatting`** — toolkit for dates. `DateFormatting.install(localeTag)` switches every formatter to a new locale.
- **`Connectivity`** — `SharedFlow<Status>` of online/offline status. Status is `Connected(metered: Boolean) | Disconnected`.
- **`NativeContext`** — platform handle. Android: wraps `Context`. iOS: empty object. Use it instead of touching `Context` directly from `commonMain`.

## Reserved names

These names are infrastructure-stable. **Do not rename** when applying these requirements to a new project:

| Category | Names |
|---|---|
| Base classes | `BaseViewModel`, `BaseComponent`, `BaseScreen`, `BaseComposeScreen`, `BaseDirection`, `BaseLoader`, `BaseRouter`, `BaseResult`, `ComponentIdentifier` |
| Infrastructure | `OperationManager`, `ResultManager`, `ResultEmitter`, `ResultKey`, `ResultKeys`, `Processing` enum |
| Design tokens | `AppTokens`, `AppTheme`, `AppColor`, `AppDp`, `AppTypography`, `AppString`, `AppDrawable`, `AppIcon`, `AppPreview`, `PreviewContainer`, `AppLocale` |
| Resources | `StringProvider`, `UiText`, `Format*State` |
| Errors | `AppError`, `ErrorProvider`, `AppErrorState` |
| Dialogs | `DialogConfig`, `DialogController`, `DialogComponent`, `DialogProvider` |
| Toolkit | `AppLogger`, `DateTimeUtils`, `DateRange`, `DateRangeKind`, `DateRangePresets`, `DateFormat`, `DateFormatting`, `NativeContext`, `Connectivity`, `NotificationManager`, `PermissionManager`, `LinkOpener` |

Product-specific names (e.g. `GrippoApi`, `RootComponent`, `Training*`) **should** be renamed.
