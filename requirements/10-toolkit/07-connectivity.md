# `:toolkit:connectivity`

`Connectivity` exposes a `SharedFlow<Status>` of online/offline state and lets callers query the current status.

## API

```kotlin
public interface Connectivity {
    public val statusUpdates: SharedFlow<Status>
    public val monitoring: StateFlow<Boolean>

    public val isMonitoring: Boolean
        get() = monitoring.value

    public suspend fun status(): Status
    public fun start()
    public fun stop()

    public sealed interface Status {
        public val isConnected: Boolean
            get() = this is Connected
        public val isMetered: Boolean
            get() = this is Connected && metered
        public val isDisconnected: Boolean
            get() = this is Disconnected

        public data class Connected(public val metered: Boolean) : Status
        public data object Disconnected : Status
    }
}
```

## Options

```kotlin
public open class ConnectivityOptions(
    public val autoStart: Boolean = DEFAULT_AUTO_START,
) {
    public class Builder internal constructor() {
        public var autoStart: Boolean = DEFAULT_AUTO_START
        public fun autoStart(autoStart: Boolean): Builder
        public fun build(): ConnectivityOptions
    }

    public companion object {
        private const val DEFAULT_AUTO_START: Boolean = false
        public fun build(block: Builder.() -> Unit): ConnectivityOptions
    }
}
```

`autoStart = true` means `Connectivity` begins monitoring as soon as it's created (typically at Koin start). With `autoStart = false`, the consumer must call `start()` explicitly.

The reference repo configures `autoStart = true` in `ConnectivityModule`.

## Status

| Status | Meaning |
|---|---|
| `Connected(metered = false)` | Online, Wi-Fi |
| `Connected(metered = true)` | Online, cellular (paid bandwidth) |
| `Disconnected` | No network |

`metered` matters for product decisions: don't auto-sync large blobs on cellular without user consent.

## `statusUpdates` semantics

- **`SharedFlow<Status>`** with `replay = 1` and `BufferOverflow.DROP_OLDEST`. The most recent status is replayed to new subscribers; older statuses are dropped if no consumer is ready.
- **First emission** is the current status at subscription time.
- **Distinct values** only — duplicate consecutive `Connected(metered = false)` emissions are filtered.

## Usage

### Observing in `RootViewModel`

```kotlin
init {
    connectivity.statusUpdates
        .onEach(::provideConnectionStatus)
        .safeLaunch()
}

private fun provideConnectionStatus(status: Connectivity.Status) {
    update { it.copy(isOnline = status.isConnected) }
}
```

`RootState` exposes `isOnline: Boolean`; a global "offline" banner can read this.

### Conditional fetching

```kotlin
internal class FooViewModel(
    private val connectivity: Connectivity,
    private val feature: BarFeature,
) : BaseViewModel<...>(...) {

    override fun onRefreshClick() {
        if (connectivity.statusUpdates.replayCache.firstOrNull()?.isDisconnected == true) {
            // skip the fetch; show stale-banner
            return
        }
        safeLaunch(loader = FooLoader.Fetch) {
            feature.fetch().getOrThrow()
        }
    }
}
```

For most cases, **don't gate calls on connectivity** — let them fail and surface `AppError.Network.NoInternet` via the error pipeline. The error dialog already explains "No internet". Gating before the call adds complexity without benefit.

Connectivity-based gating is justified for **large** operations (uploads, video, downloads) where the user shouldn't be surprised by a partial result.

## Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.connectivity" }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.toolkit.context)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            // Android ConnectivityManager APIs
        }
        iosMain.dependencies {
            // SCNetworkReachability / NWPathMonitor APIs
        }
    }
}
```

## Koin module

```kotlin
@Module(includes = [ContextModule::class])
@ComponentScan
public class ConnectivityModule {
    @Single
    internal fun provideConnectivity(nativeContext: NativeContext): Connectivity =
        nativeContext.createConnectivity()
}

// commonMain — internal/NativeConnectivity.kt
internal fun NativeContext.createConnectivity(
    provider: ConnectivityProvider = getConnectivityProvider(),
    options: ConnectivityOptions = ConnectivityOptions(autoStart = true),
    scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
): Connectivity = DefaultConnectivity(scope, provider, options)

// commonMain — internal/ConnectivityProviderFactory.kt
internal expect fun NativeContext.getConnectivityProvider(): ConnectivityProvider
```

`createConnectivity` is a regular extension with defaults — `autoStart = true` is baked into the default `ConnectivityOptions`, so the module body stays one-line. The actual `expect/actual` boundary is `getConnectivityProvider()` (Android: `AndroidConnectivityProvider(context)`; iOS: `AppleConnectivityProvider()`).

## Anti-patterns

- **Gating every API call on connectivity.** The error pipeline already handles offline; double-gating is duplicate logic.
- **Storing connection status in multiple ViewModels.** Centralize in `RootViewModel.state.isOnline`; other VMs read from it via the user-facing banner.
- **Manually polling the network.** Use `statusUpdates`.
- **Subscribing to `statusUpdates` outside a `safeLaunch`.** Lifecycle management bug.
