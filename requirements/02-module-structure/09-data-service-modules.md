# `:data-services:*` Modules

The low-level I/O layer. Only `:data-features:<feature>` (and `:shared` for composition) imports these modules. UI is firewalled away.

## Module list

| Module | Purpose | Convention plugins |
|---|---|---|
| `:data-services:backend` | `<Product>Api`, `BackendClient`, `TokenProvider`, `ClientLogger`, all DTOs | KMP + Koin + serialization |
| `:data-services:database` | Room `@Database`, entities, DAOs, `*Pack` models, migrations, `DatabaseBuilder` | KMP + Koin + Room |
| `:data-services:datastore` | AndroidX DataStore (preferences-core) wrapper | KMP + Koin |
| `:data-services:firebase` | `FirebaseProvider` interface + Android impl (Analytics, Crashlytics, Messaging) | KMP + Koin |
| `:data-services:google-auth` | Google Identity wrapper returning `idToken` (optional) | KMP + Koin |
| `:data-services:apple-auth` | Apple sign-in wrapper returning `idToken` (optional) | KMP + Koin |

## `:data-services:backend`

Houses:

- `<Product>Api` — flat `@Single public class` with **one method per endpoint**, grouped by section comments (`/* * * Auth service * * */`, `/* * * Notes service * * */`, ...). All methods return `Result<T>` and use a single private inline `request<T>(...)` helper.
- `BackendClient` — `@Single internal class` wrapping the Ktor `HttpClient`. Configures `defaultRequest` (host, HTTPS, JSON, `Accept-Language`), `HttpTimeout` (10s), `Logging` (via `ClientLogger`), `Auth` (via `TokenProvider`), `ContentNegotiation` (JSON via `kotlinx-serialization`).
- `TokenProvider` — `@Single internal class : AuthProvider`. Adds `Authorization: Bearer <token>` headers; refreshes with `Mutex` + `withTimeout` + `retryWithBackoff`; uses `AuthCircuitBreaker` attribute on the refresh call; deletes tokens on `RefreshUnauthorizedException`.
- `ClientLogger` — `@Single internal class : Logger` (Ktor `Logger`). Routes HTTP logs to `AppLogger.Network`.
- DTOs — `package com.<org>.<product>.services.backend.dto.<area>`. `@Serializable public data class <Name>Response` / `<Name>Body`. **All fields nullable + default `= null`** (defense against partial responses).
- `BackendModule` — `@Module(includes = [HttpModule::class, DatabaseModule::class, SerializationModule::class]) @ComponentScan public class BackendModule`.

See `06-data-layer/01-backend-client.md`, `06-data-layer/02-token-provider.md`, `06-data-layer/03-grippo-api-and-dtos.md` for full code.

### Build

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

## `:data-services:database`

Houses:

- `Database` — `@Database(entities = [...], version = N, exportSchema = true) @TypeConverters(StringListConverter::class) @ConstructedBy(DatabaseConstructor::class) public abstract class Database : RoomDatabase()` with abstract DAO accessor methods.
- `DatabaseBuilder` — `internal expect fun NativeContext.getDatabaseBuilder(): Database`. Android: `Room.databaseBuilder<Database>(context, dbFile.absolutePath) + addMigrations + fallbackToDestructiveMigration(dropAllTables = true) + setQueryCoroutineContext(Dispatchers.IO)`. iOS: same builder using a path in `NSDocumentDirectory` + `setDriver(BundledSQLiteDriver())`.
- `DatabaseConstructor` — `internal expect object DatabaseConstructor : RoomDatabaseConstructor<Database>` (the Room generator creates the actual on each platform).
- `entity/*.kt` — `@Entity public data class <X>Entity` files; non-null fields, indices/foreign keys declared.
- `dao/*.kt` — `@Dao public interface <X>Dao` with `@Query`, `@Insert(onConflict = REPLACE)`, `@Update`, `@Delete`, `@Transaction`. Observes return `Flow<...>`; mutations are `suspend`.
- `models/*Pack.kt` — `data class <X>Pack(@Embedded val <x>: <X>Entity, @Relation(...) val children: List<...>)`.
- `converters/StringListConverter.kt` — pipe-delimited `List<String>` ↔ `String`.
- `migrations/*.kt` — `internal object Migration<N>To<N+1> : Migration(<N>, <N+1>) { override fun migrate(connection: SQLiteConnection) { connection.execSQL(...) } }`. Collected in `DatabaseMigrations.all`.
- `DatabaseModule` — `@Module(includes = [ContextModule::class]) @ComponentScan public class DatabaseModule` with `@Single fun provideDatabase(NativeContext): Database` and one `@Single` per DAO.

See `06-data-layer/04-database.md` through `06-data-layer/06-room-migrations.md` for full details.

### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("room.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.data.services.database" }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.context)
        implementation(projects.toolkit.logger)
        implementation(projects.uiCore.error.errorProvider)   // for AppError on DB faults
    }
}
```

The `room.convention` plugin handles all Room+KSP wiring (Android + all iOS targets).

## `:data-services:datastore`

Wraps AndroidX DataStore (`preferences-core`) into a multiplatform service. Used for small key-value preferences (theme, locale, debug toggles). **Not** for tokens or sensitive data — tokens live in Room.

### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.data.services.datastore" }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.context)
        implementation(libs.androidx.datastore.preferences.core)
    }
}
```

## `:data-services:firebase`

Exposes a platform-neutral `FirebaseProvider` **object** (not interface — see below) plus three provider abstractions (`FirebaseAnalyticsProvider`, `FirebaseCrashlyticsProvider`, `FirebaseMessagingProvider`). Android implementations (`AndroidFirebaseAnalytics`, ...) live in `androidMain` and wrap the actual Firebase SDKs; iOS implementations (`IosFirebaseAnalytics`, ...) live in **Swift** under `iosApp/iosApp/` and are passed back into Kotlin via the same `FirebaseProvider.setup(...)` call.

`FirebaseProvider.setup(analytics, crashlytics, messaging)` takes platform-specific instances:

- Android: called once in `App.onCreate` with `Android*` wrappers built from `FirebaseAnalytics.getInstance(this)` etc.
- iOS: called once in `AppDelegate.application(_:didFinishLaunchingWithOptions:)` with `Ios*` Swift implementations, after `FirebaseApp.configure()`.

This module **does not** use Koin annotations. There is no `FirebaseModule`. `FirebaseProvider` is a singleton `object` that consumers (like `:ui-core:foundation`'s error pipeline) reference statically.

### Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.data.services.firebase" }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(projects.toolkit.notificationManager)

            implementation(project.dependencies.platform(libs.android.firebase.bom))
            implementation(libs.android.firebase.analytics)
            implementation(libs.android.firebase.crashlytics)
            implementation(libs.android.firebase.messaging)
            implementation(libs.kotlinx.coroutines.play.services)
        }
    }
}
```

`libs.koin.core` is in `commonMain` because the provider interfaces reference Koin's `Logger`-style types for crash reporting. `:toolkit:notification-manager` is added on `androidMain` so the Firebase Messaging service can hand incoming pushes to the local notification surface.

This module is `api`-exposed from `:shared` because the iOS XCFramework re-exports it (the convention plugin does `export(project(":data-services:firebase"))`).

## `:data-services:google-auth` / `:data-services:apple-auth`

Optional, per-product. Wrap the platform Auth SDKs and expose a single `suspend fun getIdToken(): String?` method. Implementations live in `androidMain` (`androidx.credentials` + `googleid` library for Google; native Apple sign-in for Apple) and `iosMain` (`ASAuthorization` for Apple; Google handled differently or skipped).

These modules are consumed by `:ui-screen-features:authorization` directly — not by `:data-features:authorization`. The login screen drives the platform credential flow (no domain user exists yet), then hands the resulting ID token to `AuthorizationFeature.login(...)`. This is one of the documented carve-outs to the "UI MUST NOT depend on `:data-services:*`" rule (see `02-dependency-rules.md`).

## `:data-services:*` rules

- **`internal` everywhere** except: the public Koin `<X>Module`, types referenced by `:data-features:<feature>` (most internal types are referenced via interfaces declared inside this module, then implemented in `androidMain`/`iosMain`).
- **No transitive UI access.** The data services do not know about `UiText`, `*FormatState`, Composables, or design tokens.
- **No domain models.** DTOs and entities live here; the conversion to domain happens in mappers + repositories.
- **One service per concern.** Don't put HTTP and DataStore in the same module. Keep boundaries clean.

## Per-platform implementation pattern

Many services have an `expect/actual` split:

```
:data-services:database/
  src/commonMain/kotlin/...     // expect declarations + commonMain Room API
  src/androidMain/kotlin/...    // Room.databaseBuilder<Database>(context, dbFile.absolutePath)
  src/iosMain/kotlin/...        // Room.databaseBuilder<Database>(name = nsDocumentPath) + BundledSQLiteDriver()
```

This is the **idiomatic** way to handle platform-specific I/O. Avoid wrapper interfaces with platform-specific implementations injected via DI — `expect/actual` is simpler, less indirection, and Compose-friendly.
