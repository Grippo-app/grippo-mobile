# `:toolkit:image-loader` — Coil 3 + Ktor 3

Coil 3 is used as the image loader on both platforms. Its `coil-network-ktor3` adapter routes image requests through the same Ktor `HttpClient` the rest of the app uses (so auth headers and `Accept-Language` apply if needed).

## Module shape

```kotlin
@Module(includes = [HttpModule::class])
@ComponentScan
public class ImageLoaderModule {

    @Single
    internal fun imageLoaderFactory(
        httpClient: HttpClient,
    ): SingletonImageLoader.Factory = SingletonImageLoader.Factory { context ->
        ImageLoader.Builder(context)
            .crossfade(true)
            .maxBitmapSize(Size(400, 400))
            .components { add(KtorNetworkFetcherFactory(httpClient)) }
            .build()
    }

    @Single(createdAtStart = true)
    internal fun imageLoaderInitializer(
        factory: SingletonImageLoader.Factory,
    ): ImageLoaderInitializer = ImageLoaderInitializer(factory)
}

// internal/ImageLoaderInitializer.kt
internal class ImageLoaderInitializer(factory: SingletonImageLoader.Factory) {
    init { SingletonImageLoader.setSafe(factory) }
}
```

- `@Module(includes = [HttpModule::class])` pulls in the shared `HttpClient`.
- The provider produces a `SingletonImageLoader.Factory` — Coil 3's hook for lazy, platform-aware `ImageLoader` construction. The factory's lambda receives a `PlatformContext` from Coil itself, so no `NativeContext`/`expect/actual` plumbing is needed in this module.
- `.maxBitmapSize(Size(400, 400))` caps decoded bitmaps; thumbnails are rendered at avatar/card sizes anyway.
- `ImageLoaderInitializer` is `@Single(createdAtStart = true)` — Koin instantiates it eagerly during `startKoin`, which calls `SingletonImageLoader.setSafe(factory)` before any Composable runs.

## Installation

Installation is entirely DI-driven on both platforms — there is no manual `SingletonImageLoader.setSafe { ... }` call in the app shells. As long as `ImageLoaderModule` is in `Koin.init { ... modules(...) }` (see `:shared/Koin.kt`), the `createdAtStart = true` initializer runs once, idempotently, on startup.

## Usage

```kotlin
AsyncImage(
    model = userAvatarUrl,
    contentDescription = null,
    modifier = Modifier.size(AppTokens.dp.icon.xxLarge).clip(CircleShape),
    contentScale = ContentScale.Crop,
    placeholder = AppTokens.drawables.res(Res.drawable.img_avatar_placeholder),
    error = AppTokens.drawables.res(Res.drawable.img_avatar_placeholder),
)
```

The `AsyncImage` Composable lives in `coil-compose`. Placeholders and errors are passed explicitly.

## Why Ktor adapter

Coil 3 ships with `coil-network-okhttp` (Android-only), `coil-network-ktor3` (multiplatform), and others. The Ktor adapter:

- **Reuses the existing `HttpClient`** — caching, auth, locale headers, all consistent.
- **Multiplatform** — works on both Android and iOS.
- **No second engine** — Ktor's engine handles both API calls and image fetches.

If image requests should **not** carry the Authorization header (typical for public CDN images), build a separate `HttpClient` without `Auth` and pass it to the Coil builder. The reference repo uses the same client; most product CDNs are public.

## Caching

Coil 3 handles disk + memory cache automatically. Disk cache lives in the platform's cache directory; size cap is the default (~250 MB). Tune via `.diskCache { ... }` if needed.

## Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.image.loader" }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.httpClient)

        implementation(libs.coil.compose)
        implementation(libs.coil.network.ktor)
    }
}
```

No `compose.multiplatform.convention` and no `:toolkit:context` dep — Coil 3's `SingletonImageLoader.Factory` handles platform context internally, and `AsyncImage` is consumed by feature modules (which already have the Compose plugin), not by this module.

## Rules

- **One `ImageLoader` app-wide.** Singleton via Koin + `SingletonImageLoader.setSafe`.
- **Compose Composables use `AsyncImage` / `SubcomposeAsyncImage` from `coil-compose`.** Don't import Coil's lower-level types in feature code.
- **Placeholders/errors** come from `:design-system:resources:provider` via `AppTokens.drawables.res(...)`.
- **`crossfade(true)`** for natural transitions; can be turned off per request.
- **`@Composable` callers don't read from network directly.** Pass URLs to `AsyncImage`.

## Anti-patterns

- **`Glide`, `Picasso`, `Kamel`** — different libraries, fragmented behavior. Coil 3 only.
- **Multiple `ImageLoader` instances** with different configs. Centralize.
- **Loading bitmaps manually via `BitmapFactory.decodeStream(...)`** — bypasses caching, leaks.
- **Hardcoded `MemoryCache.maxSizeBytes`** in feature code — set centrally if at all.
- **Network image inside a `LazyColumn` without `placeholder`** — first frame is empty; jarring.
