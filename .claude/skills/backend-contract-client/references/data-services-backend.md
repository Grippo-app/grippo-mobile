# `:data-services:backend` module

The low-level I/O layer. Only `:data-features:<feature>` (and `:shared` for composition) imports
the `:data-services:*` modules. UI is firewalled away.

`:data-services:backend` houses `<Product>Api`, `BackendClient`, `TokenProvider`, `ClientLogger`, and
all DTOs. Convention plugins: KMP + Koin + serialization.

## What it houses

- **`<Product>Api`** — flat `@Single public class` with **one method per endpoint**, grouped by
  section comments (`/* * * Auth service * * */`, `/* * * Notes service * * */`, ...). All methods
  return `Result<T>` and use a single private inline `request<T>(...)` helper.
- **`BackendClient`** — `@Single internal class` wrapping the Ktor `HttpClient`. Configures
  `defaultRequest` (host, HTTPS, JSON, `Accept-Language`), `HttpTimeout` (10s), `Logging` (via
  `ClientLogger`), `Auth` (via `TokenProvider`), `ContentNegotiation` (JSON via
  `kotlinx-serialization`).
- **`TokenProvider`** — `@Single internal class : AuthProvider`. Adds `Authorization: Bearer <token>`
  headers; refreshes with `Mutex` + `withTimeout` + `retryWithBackoff`; uses `AuthCircuitBreaker`
  attribute on the refresh call; deletes tokens on `RefreshUnauthorizedException`.
- **`ClientLogger`** — `@Single internal class : Logger` (Ktor `Logger`). Routes HTTP logs to
  `AppLogger.Network`.
- **DTOs** — `package com.<org>.<product>.services.backend.dto.<area>`. `@Serializable public data
  class <Name>Response` / `<Name>Body`. Scalar fields are nullable with default `= null`;
  collection fields default to `emptyList()` (defense against partial responses).
- **`BackendModule`** — `@Module(includes = [HttpModule::class, DatabaseModule::class,
  SerializationModule::class]) @ComponentScan public class BackendModule`.

See the data-layer skill (`../../data-layer/references/backend-client.md`,
`../../data-layer/references/auth-session.md`,
`../../data-layer/references/dtos-and-api.md`) for full code.

## Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android { namespace = "com.<org>.<product>.data.services.backend" }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.serialization)
        implementation(projects.toolkit.httpClient)
        implementation(projects.toolkit.logger)
        implementation(projects.toolkit.localization)
        implementation(projects.dataServices.database)         // for TokenDao/UserActiveDao

        implementation(libs.ktor.client.core)
        implementation(libs.ktor.serialization.kotlinx.json)
        implementation(libs.ktor.client.logging)
        implementation(libs.ktor.auth)
        implementation(libs.ktor.client.content.negotiation)
        implementation(libs.kotlinx.serialization.json)
    }
}
```
