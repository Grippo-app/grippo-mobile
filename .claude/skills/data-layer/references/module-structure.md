# Module structure — `:data-features:*` and `:data-services:*`

Self-contained reference for the data-layer module rules.

> **Illustrative domain.** Code uses `Note` / `Tag` / `User` as the generic
> `<Entity>` / `<RelatedEntity>`. Substitute identifiers from your product domain.

---

## Data flow (UI → network)

```
ViewModel
  → <X>Feature (interface in :feature-api)
    → <X>FeatureImpl (in :data-features:<x>, @Single(binds = [<X>Feature::class]))
      → <X>Repository (internal interface)
        → <X>RepositoryImpl (@Single(binds = [<X>Repository::class]))
          → <Product>Api.<method>(body): Result<DTO>   // HTTP via :data-services:backend
          → <X>Dao.<query>(): Flow<Pack>            // Room via :data-services:database
```

**Observe** returns `Flow<Domain>` from the DAO; **get/set/update/delete** returns
`Result<T>`, hits the API, and reconciles the DAO on success.

---

## `:data-features:*` — the domain layer

UI sees `:data-features:feature-api` (interfaces + domain models); implementations
live in `:data-features:<feature>` (Repository + FeatureImpl, `internal`).

### Module list

| Module | Purpose | Convention plugins |
|---|---|---|
| `:data-features:feature-api` | All `<X>Feature` interfaces, all domain models, all `<X>UseCase` classes | KMP + Koin |
| `:data-features:authorization` | Authorization feature impl | KMP + Koin |
| `:data-features:<area>` | One per business area | KMP + Koin |

Per-project. Each module is `:data-features:<feature>` and follows the pattern below.

### `:data-features:feature-api` contents

The single point of contact between UI and data. Holds:

- `<X>Feature` interfaces — e.g. `public interface NoteFeature { fun observeNotes(...): Flow<List<Note>>; suspend fun getNotes(...): Result<Unit> }`.
- Domain models — `public data class Note(val id: String, val createdAt: LocalDateTime, ...)`.
- `<X>UseCase` classes — for use cases that **combine two or more Feature
  interfaces** (e.g. `LoginUseCase` composing `AuthorizationFeature` + `UserFeature`)
  **or encapsulate domain computation the ViewModel should not hold** (e.g.
  `ExerciseValidatorUseCase` with zero Feature deps, `TrainingDigestUseCase`). A
  `<X>UseCase` exposes a single public entry point in one of two shapes: a suspend
  `execute(...): Result<T>` command, or an `observe<Noun>(...): Flow<T>` aggregation
  that combines the features' flows.
- `FeatureApiModule` — `@Module public class FeatureApiModule` (**no `@ComponentScan`**).
  UseCase classes carry **no Koin annotation** — no `@Factory`, no `@Single`. They are
  plain Kotlin classes registered explicitly via
  `@get:JvmName("module") public val module: ModuleObject = module { single { LoginUseCase(get(), get()) }; single { ExerciseValidatorUseCase() }; … }`.
  This is the **sole exception** to the no-hand-DSL rule (every other module uses
  `@ComponentScan`).

### Rules (feature-api) — MUST

- **No** `:data-services:*` imports. The api module is pure contracts and domain types.
- **No** UI types (no `UiText`, no `*FormatState`, no `@Immutable`). Domain models are
  pure Kotlin data classes.
- **No** `@Serializable` on domain models, domain enums, or domain sealed types — ever.
  Domain types never enter `*Router` / `DialogConfig` payloads; route/dialog payloads
  carry the parallel `*State` / `*EnumState` mirror from `:ui-core:state` (which is
  `@Serializable`), simple primitives, or route sealed types defined in
  `:ui-screen-features:screen-api`.
- Mutations return `Result<T>`; observations return `Flow<Domain>`.

### Strict domain typing — MUST

Domain models are the **typed** layer. Every field carries the strongest type the
value actually has — the raw `String` / `Long` shapes from the wire and the DB stop at
the mapper boundary. The DTO (`:data-services:backend`, all-nullable `String?`) and the
Entity (`:data-services:database`, raw `String` / `Long` columns) keep the backend's
replica untouched; the **entity→domain** / **dto→domain** mapper is where each value is
promoted to its domain type:

| Backend / DB shape | Domain type | Promoted via |
|---|---|---|
| ISO-8601 timestamp `String` | `LocalDateTime` / `LocalDate` | `DateTimeUtils.toLocalDateTime(...)` |
| minutes / seconds `Long` / `Int` | `Duration` | `value.minutes` / `value.seconds` |
| closed-set / dictionary `String` (type, status, kind, category, …) | a domain `<X>Enum` (pure labels) or `sealed interface <X>` (variants carry payload) | `<X>Enum.of(value)` / guarded `when (type)` |
| open free text (name, id, url, hex color, note body) | `String` | pass-through |

A raw `String` survives into a domain model **only** when the value is genuinely open
text. If the backend draws the value from a fixed vocabulary, it is a domain enum —
never a bare `String`.

### Domain enums and sealed types — MUST

When the backend exposes a field with a **fixed set of variants**, model it strictly —
never leave it a `String`. Pick the shape by whether the variants carry data:

- **Pure labels** (variants differ only by identity) → an `enum class`.
- **Variants carry payload** (a discriminator selects which other fields apply) → a
  `sealed interface` / `sealed class`.

Either way, **the wire token is preserved** as `key`, so the value round-trips back to
the backend unchanged.

#### Enum — pure labels (EXAMPLE)

A closed-set backend field becomes a `<X>Enum`, declared next to the domain models it
serves in `:data-features:feature-api/.../<area>/models/<X>Enum.kt`:

```kotlin
// Illustrative — substitute identifiers from your product domain.
public enum class NoteStatusEnum(public val key: String) {
    DRAFT(key = "draft"),
    IN_REVIEW(key = "in_review"),
    PUBLISHED(key = "published");

    public companion object {
        public fun of(key: String?): NoteStatusEnum? =
            entries.firstOrNull { it.key == key }
    }
}
```

Enum rules (MUST):

1. **`key` carries the wire value.** The enum member is the typed domain value; `key`
   is the exact token the backend sends and the body re-sends. Member names are
   idiomatic Kotlin (`IN_REVIEW`); `key` is the verbatim contract string (`"in_review"`).
2. **`companion object fun of(key: String?)` is the only parser.** Two shapes, chosen by
   whether an unknown value should drop the row:
   - **Drop-on-unknown** (the default for most dictionaries) —
     `fun of(key: String?): <X>Enum? = entries.firstOrNull { it.key == key }`.
     The mapper guards it: `AppLogger.Mapping.log(<X>Enum.of(v)) { ... } ?: return null`.
   - **Default-on-unknown** —
     `fun of(key: String?): <X>Enum = entries.firstOrNull { it.key == key } ?: <DEFAULT>`.
     Use only when a sensible fallback exists and the row must never drop (e.g. a
     user-role field defaulting to a base tier). The mapper calls it bare — no
     `?: return null`.
3. **Pure Kotlin.** No `@Serializable`, no `@Immutable`, no `UiText`, no `@Composable`,
   no design-system imports on a domain enum. UI concerns (a localized `title()`, a
   themed `color()`) live on the parallel `*EnumState` in `:ui-core:state`, bridged by
   the `:data-mappers:domain-to-state` / `state-to-domain` exhaustive-`when` mappers.
4. **Back to the wire**: the `:data-mappers:domain-to-dto` body mapper sends `enum.key`
   (`<field>?.key` for a nullable enum field, `<list>.map { it.key }` for a list of enums).

#### Sealed type — variants carry payload (EXAMPLE)

When each variant has a **different shape** (a backend `type` discriminator picks which
fields are present), an enum can't hold the per-variant data — use a `sealed interface` /
`sealed class`. Keep the wire token as a base `val key: String` that every variant overrides:

```kotlin
// Illustrative — `<X>` is the domain model, `<VariantA>`/`<VariantB>` its wire variants.
public sealed interface <X> {
    public val key: String   // wire discriminator token, preserved

    public data class <VariantA>(
        override val key: String,
        val <fieldA>: Boolean,
    ) : <X>

    public data class <VariantB>(
        override val key: String,
        val <fieldB>: Float,
        val <fieldC>: Boolean,
    ) : <X>
}
```

Sealed type rules (MUST):

1. **No `companion object of(key)`.** A discriminator alone can't supply each variant's
   payload, so parsing lives in the mapper as a guarded `when (type)` that builds the
   right variant and drops the row on an unknown discriminator.
2. **Preserve the wire token** as the base `val key: String`, overridden per variant —
   the value round-trips back unchanged.
3. **Same purity rules as enums.** No `@Serializable` / `@Immutable` / `UiText` /
   `@Composable` on the domain sealed type; the UI mirror is a `*State` sealed type in
   `:ui-core:state`.
4. **Back to the wire**: the body mapper reads `value.key` plus the variant's payload
   inside a `when (value)`.

### Build (feature-api) — EXAMPLE

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.data.features.feature.api" }

    sourceSets.commonMain.dependencies {
        implementation(projects.toolkit.dateUtils)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.datetime)
    }
}
```

No design-system, no UI core, no other toolkit module besides `:toolkit:date-utils`
(needed because several domain models carry `LocalDateTime`/`DateRange` values).

### `:data-features:<feature>` (implementation)

Each implementation module holds:

- `data/<X>RepositoryImpl.kt` — `internal class <X>RepositoryImpl(private val api: <Product>Api, private val dao: <X>Dao, ...) : <X>Repository`. Annotated `@Single(binds = [<X>Repository::class])`.
- `domain/<X>Repository.kt` — `internal interface <X>Repository { ... }`. **Internal** to this module.
- `domain/<X>FeatureImpl.kt` — `internal class <X>FeatureImpl(private val repository: <X>Repository) : <X>Feature`. Annotated `@Single(binds = [<X>Feature::class])`.
- `<X>FeatureModule.kt` — `@Module(includes = [BackendModule::class, DatabaseModule::class]) @ComponentScan public class <X>FeatureModule`. **Public** so `:shared/Koin.kt` can reference it.

#### Why an extra `<X>Repository` layer (REFERENCE)

- `<X>Feature` is the **UI-visible** contract. It uses domain models only, returns
  `Result<T>` for mutations, exposes `Flow<Domain>` for observations.
- `<X>Repository` is the **internal** contract that combines `<X>Dao` (Room) +
  `<Product>Api` (Ktor). Sometimes it adds caching policies, range reconciliation,
  draft handling, or `Mapper` calls.
- `<X>FeatureImpl` is a **thin** wrapper that mostly delegates to `<X>Repository` but
  may compose multiple repositories or add lightweight transformations.

Splitting them lets the Repository test against fakes for `<X>Dao` and `<Product>Api`,
while the Feature stays a stable public surface. For trivial features (no caching, no
composition), `<X>FeatureImpl` may delegate one-to-one to `<X>Repository` — the layer is
consistent across the codebase regardless of complexity.

#### Standard patterns (MUST)

> Full pattern in [`repositories.md`](repositories.md).

- **Observe** returns `Flow<Domain>` from the DAO — never from the API.
- **Get/Set/Update/Delete** returns `Result<Unit>` or `Result<T>`. Hits the API, then
  updates the DAO on `onSuccess`.
- **Range reconciliation**: after `getNotes(start, end)`, delete all rows in
  `[start, end]` except those returned by the server
  (`dao.deleteByCreatedAtRangeExceptIds(...)`). This removes "deleted on another device"
  drift.
- **Drafts** live only in the DB (`draftNoteDao`); they never round-trip through the server.

#### Build (feature impl) — EXAMPLE

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.data.features.<feature>" }

    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.database)
        implementation(projects.dataServices.backend)
        implementation(projects.dataMappers.domainToEntity)
        implementation(projects.dataMappers.entityToDomain)
        implementation(projects.dataMappers.dtoToEntity)
        implementation(projects.dataMappers.domainToDto)
        implementation(projects.toolkit.dateUtils)

        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.datetime)
    }
}
```

A feature module always depends on `:data-features:feature-api` and the data services +
mappers it actually uses. Pull only what the feature touches — don't pre-import.

### Adding a new feature (REFERENCE)

> Full recipe in [`cookbook-data-feature.md`](cookbook-data-feature.md).

1. Add `<X>Feature` interface + domain models in `:data-features:feature-api`.
2. Create `:data-features:<x>` module (add to `settings.gradle.kts`).
3. Implement `<X>Repository` + `<X>RepositoryImpl` + `<X>FeatureImpl`.
4. Declare `<X>FeatureModule` (public Koin module).
5. Add `implementation(projects.dataFeatures.<x>)` to `:shared/build.gradle.kts`.
6. Add `<X>FeatureModule().module` to `:shared/Koin.kt`'s `modules(...)`.

### `:data-features:feature-api` exposes — checklist (MUST)

For a new feature `<X>`:

- [ ] `<X>Feature` interface with `Flow`-returning observers and `Result`-returning mutators.
- [ ] `<X>` (and any sub-types) domain model — non-null fields, no platform types, no `@Serializable`.
- [ ] Strictly typed fields: closed-set strings as a `<X>Enum` (pure labels — `key` +
      `companion object of(key)`) or a `sealed interface <X>` (variants carry payload —
      base `val key`, per-variant `data class`, no `of()`); timestamps as `LocalDateTime`;
      durations as `Duration` — never a raw `String` where a closed type fits.
- [ ] `<X>UseCase` only when: **(a)** the operation **composes two or more Feature
      interfaces**, OR **(b)** the logic is **domain computation the ViewModel should not
      hold** (pure calculation, validation, aggregation with zero or few Feature deps).

For a new use case:

- [ ] `<X>UseCase` class with a single public entry point — two allowed shapes: a command
      `public suspend fun execute(...): Result<T>` or a stream
      `public fun observe<Noun>(...): Flow<T>` that combines the features' flows.
      Domain-named variants of that single verb (e.g. `executeEmail/executeGoogle/executeApple`)
      are allowed — still one logical operation.
- [ ] **No Koin annotation** on the class — no `@Factory`, no `@Single`. Plain Kotlin class.
- [ ] Registered in `FeatureApiModule` via `single { <X>UseCase(get(), …) }` in its
      `module { }` body (the sole allowed hand-DSL exception).

---

## `:data-services:*` — the low-level I/O layer

Only `:data-features:<feature>` (and `:shared` for composition) imports these modules.
UI is firewalled away.

### Module list

| Module | Purpose | Convention plugins |
|---|---|---|
| `:data-services:backend` | `<Product>Api`, `BackendClient`, `TokenProvider`, `ClientLogger`, all DTOs | KMP + Koin + serialization |
| `:data-services:database` | Room `@Database`, entities, DAOs, `*Pack` models, migrations, `DatabaseBuilder` | KMP + Koin + Room |
| `:data-services:datastore` | AndroidX DataStore (preferences-core) wrapper | KMP + Koin |
| `:data-services:firebase` | `FirebaseProvider` interface + Android impl (Analytics, Crashlytics, Messaging) | KMP + Koin |
| `:data-services:google-auth` | Google Identity wrapper returning `idToken` (optional) | KMP + Koin + Compose |
| `:data-services:apple-auth` | Apple sign-in wrapper returning `idToken` (optional) | KMP + Koin + Compose |

### `:data-services:backend`

> Full code in [`backend-client.md`](backend-client.md), [`auth-session.md`](auth-session.md), [`dtos-and-api.md`](dtos-and-api.md).

Houses `<Product>Api` (flat `@Single public class`, one method per endpoint, all returning
`Result<T>` via a private inline `request<T>(...)` helper), `BackendClient`
(`@Single internal class` wrapping the Ktor `HttpClient`), `TokenProvider`
(`@Single internal class : AuthProvider`), `ClientLogger` (`@Single internal class : Logger`),
all DTOs (`package com.<org>.<product>.services.backend.dto.<area>`, scalar fields nullable
with default `= null`, collections default `emptyList()`), and `BackendModule`
(`@Module(includes = [HttpModule::class, DatabaseModule::class, SerializationModule::class]) @ComponentScan public class BackendModule`).

Build: KMP + Koin + `alias(libs.plugins.kotlin.serialization)`; namespace
`com.<org>.<product>.data.services.backend`; depends on `:toolkit:serialization`,
`:toolkit:http-client`, `:toolkit:logger`, `:toolkit:localization`,
`:data-services:database` (for `TokenDao`/`UserActiveDao`), plus the Ktor libs
(`ktor.client.core`, `ktor.serialization.kotlinx.json`, `ktor.client.logging`, `ktor.auth`,
`ktor.client.content.negotiation`, `kotlinx.serialization.json`).

### `:data-services:database`

> Full code in [`persistence-room.md`](persistence-room.md), [`cookbook-room-migration.md`](cookbook-room-migration.md).

Houses the single `@Database` class, entities, DAOs, `*Pack` models, `StringListConverter`,
migrations (collected in `DatabaseMigrations.all`), the `DatabaseBuilder` (expect/actual),
and `DatabaseModule`. Build: KMP + `room.convention` + Koin; namespace
`com.<org>.<product>.data.services.database`; depends on `:toolkit:context`,
`:toolkit:logger`, `:ui-core:error:error-provider` (for `AppError` on DB faults). The
`room.convention` plugin handles all Room+KSP wiring (Android + all iOS targets).

### `:data-services:datastore`

> Full code in [`datastore.md`](datastore.md).

Wraps AndroidX DataStore (`preferences-core`) into a multiplatform service. Used for small
key-value preferences (theme, locale, debug toggles). **Not** for tokens or sensitive data
— tokens live in Room. Build: KMP + Koin; namespace
`com.<org>.<product>.data.services.datastore`; depends on `:toolkit:context` and
`libs.androidx.datastore.preferences.core`.

### `:data-services:firebase`

Exposes a platform-neutral `FirebaseProvider` **object** (not interface) plus three provider
abstractions (`FirebaseAnalyticsProvider`, `FirebaseCrashlyticsProvider`,
`FirebaseMessagingProvider`). Android implementations (`AndroidFirebaseAnalytics`, …) live in
`androidMain` and wrap the actual Firebase SDKs; iOS implementations (`IosFirebaseAnalytics`, …)
live in **Swift** under `iosApp/iosApp/` and are passed back into Kotlin via the same
`FirebaseProvider.setup(...)` call.

`FirebaseProvider.setup(analytics, crashlytics, messaging)` takes platform-specific instances:

- Android: called once in `App.onCreate` with `Android*` wrappers built from
  `FirebaseAnalytics.getInstance(this)` etc.
- iOS: called once in `AppDelegate.application(_:didFinishLaunchingWithOptions:)` with `Ios*`
  Swift implementations, after `FirebaseApp.configure()`.

This module **does not** use Koin annotations. There is no `FirebaseModule`. `FirebaseProvider`
is a singleton `object` that consumers (like `:ui-core:foundation`'s error pipeline) reference
statically. `libs.koin.core` is in `commonMain` because the provider interfaces reference
Koin's `Logger`-style types for crash reporting. `:toolkit:notification-manager` is added on
`androidMain` so the Firebase Messaging service can hand incoming pushes to the local
notification surface. This module is `api`-exposed from `:shared` because the iOS XCFramework
re-exports it (the convention plugin does `export(project(":data-services:firebase"))`).

### `:data-services:google-auth` / `:data-services:apple-auth`

Optional, per-product. Wrap the platform Auth SDKs and expose a single
`suspend fun getIdToken(): String?` method. Implementations live in `androidMain`
(`androidx.credentials` + `googleid` library for Google; native Apple sign-in for Apple) and
`iosMain` (`ASAuthorization` for Apple; Google handled differently or skipped).

These modules are consumed by `:ui-screen-features:authorization` directly — not by
`:data-features:authorization`. The login screen drives the platform credential flow (no
domain user exists yet), then hands the resulting ID token to `AuthorizationFeature.login(...)`.
This is one of the documented carve-outs to the "UI MUST NOT depend on `:data-services:*`" rule.

Both modules apply `compose.multiplatform.convention` (the Google flow uses
`androidx.credentials` UI APIs that Compose hosts, and both thread a Composable callback into
the platform-side credential prompt). The Apple module's `-framework AuthenticationServices`
linker opt lets `ASAuthorizationController` resolve at link time on iOS; Google additionally
needs `-framework Security`. Skipping either linker opt produces a "framework not found"
failure at iOS link.

### `:data-services:*` rules — MUST

- **`internal` everywhere** except: the public Koin `<X>Module`, types referenced by
  `:data-features:<feature>` (most internal types are referenced via interfaces declared inside
  this module, then implemented in `androidMain`/`iosMain`).
- **No transitive UI access.** The data services do not know about `UiText`, `*FormatState`,
  Composables, or design tokens.
- **No domain models.** DTOs and entities live here; the conversion to domain happens in
  mappers + repositories.
- **One service per concern.** Don't put HTTP and DataStore in the same module. Keep
  boundaries clean.

### Per-platform implementation pattern

Many services have an `expect/actual` split:

```
:data-services:database/
  src/commonMain/kotlin/...     // expect declarations + commonMain Room API
  src/androidMain/kotlin/...    // Room.databaseBuilder<Database>(context, dbFile.absolutePath)
  src/iosMain/kotlin/...        // Room.databaseBuilder<Database>(name = nsDocumentPath) + BundledSQLiteDriver()
```

This is the **idiomatic** way to handle platform-specific I/O. Avoid wrapper interfaces with
platform-specific implementations injected via DI — `expect/actual` is simpler, less
indirection, and Compose-friendly.
