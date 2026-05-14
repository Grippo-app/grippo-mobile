# `:toolkit:serialization` — `Json` provider

A single `kotlinx.serialization.json.Json` instance, configured for **defensive deserialization**. Shared by `BackendClient`, `DataStore` (if it persists JSON), and Decompose `StateKeeper` (indirectly via `kotlinx-serialization`).

## Module

```kotlin
@Module
@ComponentScan
public class SerializationModule {
    @Single
    internal fun provideJson(): Json = Json {
        useAlternativeNames = false
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }
}
```

## Configuration rationale

| Option | Value | Why |
|---|---|---|
| `useAlternativeNames` | `false` | Disables `@JsonNames` alternative lookups — DTOs are matched by `@SerialName` only, which keeps the wire contract one-to-one with the backend |
| `ignoreUnknownKeys` | `true` | Backend may add fields without coordinating a mobile release; old clients ignore unknowns |
| `isLenient` | `true` | Accepts trailing commas, unquoted keys — defensive against malformed responses |
| `prettyPrint` | `true` | Outgoing bodies are pretty-printed; makes the network log readable. Trivial wire-size cost — payloads are small |

These four flags together are the **defensive defaults**. Don't change without a strong reason.

## Build

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.serialization" }

    sourceSets.commonMain.dependencies {
        implementation(libs.kotlinx.serialization.json)
    }
}
```

## Usage

The `Json` instance is injected wherever JSON serialization/deserialization happens:

```kotlin
// :data-services:backend/BackendClient.kt
install(ContentNegotiation) {
    json(json = json, contentType = ContentType.Application.Json)
}
```

```kotlin
// elsewhere (rare — most serialization goes through Ktor)
class MyService(private val json: Json) {
    fun serialize(value: MyData): String = json.encodeToString(value)
    fun deserialize(text: String): MyData = json.decodeFromString(text)
}
```

## Rules

- **One `Json` instance app-wide.** Different configs lead to subtle bugs.
- **Don't `Json { ... }` ad-hoc.** Inject the shared instance via Koin.
- **`@Serializable` on every DTO** that goes through this `Json`. Compile-time-checked via the `kotlin-serialization` plugin.

## When to add a different config

Almost never. If you genuinely need a second `Json` (e.g. one for backend with `ignoreUnknownKeys = true`, one for an internal protocol with `ignoreUnknownKeys = false`), add a named provider:

```kotlin
@Module
@ComponentScan
public class SerializationModule {
    @Single
    internal fun provideJson(): Json = Json { ... defensive defaults ... }

    @Single
    @Named("strict")
    internal fun provideStrictJson(): Json = Json {
        ignoreUnknownKeys = false
        // ...
    }
}
```

Consumers `inject<Json>(named("strict"))`. But weigh the cost — two configs mean two compile-time validators.

## Anti-patterns

- **`Json.Default`** in production code — has `encodeDefaults = false` and other quirks.
- **`Json { isLenient = false; ignoreUnknownKeys = false }`** — strict modes break on backend evolution.
- **Multiple `Json` instances created inline** in different services. Single source of truth.
- **`@Transient` on serialized fields you actually want to send.** `@Transient` skips serialization; for non-serialized callback lambdas it's correct (see `03-architecture-patterns/03-dialog-navigation.md`), but for data fields it's a bug.
