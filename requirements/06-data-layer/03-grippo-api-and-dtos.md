# `GrippoApi` and DTOs

`GrippoApi` is a **flat, one-method-per-endpoint** class. No subgrouping, no sub-services, no resource-style classes. Just methods grouped by **comments** (`/* * * Auth service * * */`). This is intentional — it keeps the entire HTTP contract visible in one file.

Rename to `<Product>Api` per product.

## Class shape

```kotlin
@Single
public class GrippoApi internal constructor(private val client: BackendClient) {

    /* * * * * * * * * * * * * * * * *
     *  Auth service
     * * * * * * * * * * * * * * * * */

    public suspend fun login(body: EmailAuthBody): Result<TokenResponse> =
        request(method = HttpMethod.Post, path = "/auth/login", body = body)

    public suspend fun google(body: GoogleBody): Result<TokenResponse> =
        request(method = HttpMethod.Post, path = "/auth/google", body = body)

    public suspend fun apple(body: AppleBody): Result<TokenResponse> =
        request(method = HttpMethod.Post, path = "/auth/apple", body = body)

    public suspend fun register(body: RegisterBody): Result<TokenResponse> =
        request(method = HttpMethod.Post, path = "/auth/register", body = body)

    public suspend fun refresh(body: RefreshBody): Result<TokenResponse> =
        request(method = HttpMethod.Post, path = "/auth/refresh", body = body)

    /* * * * * * * * * * * * * * * * *
     *  Push token service
     * * * * * * * * * * * * * * * * */

    public suspend fun sendPushToken(body: PushTokenBody): Result<Unit> =
        request(method = HttpMethod.Post, path = "/push-tokens", body = body)

    public suspend fun deletePushToken(): Result<Unit> =
        request(method = HttpMethod.Delete, path = "/push-tokens")

    /* * * * * * * * * * * * * * * * *
     *  User service
     * * * * * * * * * * * * * * * * */

    public suspend fun getUser(): Result<UserResponse> =
        request(method = HttpMethod.Get, path = "/users")

    public suspend fun deleteUser(): Result<Unit> =
        request(method = HttpMethod.Delete, path = "/users")

    public suspend fun createProfile(body: CreateProfileBody): Result<UserResponse> =
        request(method = HttpMethod.Post, path = "/users", body = body)

    /* * * * * * * * * * * * * * * * *
     *  Trainings service
     * * * * * * * * * * * * * * * * */

    public suspend fun getTrainings(start: String, end: String): Result<List<TrainingResponse>> =
        request(
            method = HttpMethod.Get,
            path = "/trainings",
            queryParams = mapOf("start" to start, "end" to end),
        )

    public suspend fun setTraining(body: TrainingBody): Result<IdResponse> =
        request(method = HttpMethod.Post, path = "/trainings", body = body)

    public suspend fun updateTraining(id: String, body: TrainingBody): Result<Unit> =
        request(
            method = HttpMethod.Put,
            path = "/trainings",
            body = body,
            queryParams = mapOf("id" to id),
        )

    public suspend fun deleteTraining(id: String): Result<Unit> =
        request(method = HttpMethod.Delete, path = "/trainings/$id")

    /* * * * * * * * * * * * * * * * *
     *  Utilities
     * * * * * * * * * * * * * * * * */

    private suspend inline fun <reified T> request(
        method: HttpMethod,
        path: String,
        body: Any? = null,
        queryParams: Map<String, String>? = null,
    ): Result<T> = runCatching {
        client.invoke(method, path, body, queryParams).body()
    }
}
```

## Conventions

### One method per endpoint

`GET /trainings?start&end` → `suspend fun getTrainings(start: String, end: String): Result<List<TrainingResponse>>`.

`POST /trainings` → `suspend fun setTraining(body: TrainingBody): Result<IdResponse>`.

Method name reflects the action verb (`get`, `set`, `update`, `delete`, `register`, `login`, `refresh`) — not the HTTP verb. `set` is used instead of `post` to read more naturally at the call site (`api.setTraining(...)`).

### Section comments

Endpoints are grouped by domain (`Auth`, `Push token`, `User`, `Trainings`, ...). The block-comment delimiter is consistent:

```kotlin
/* * * * * * * * * * * * * * * * *
 *  Trainings service
 * * * * * * * * * * * * * * * * */
```

Useful when the file grows to 50+ endpoints. Keep sections roughly ordered to match the backend OpenAPI sections.

### `request<T>` helper

```kotlin
private suspend inline fun <reified T> request(
    method: HttpMethod,
    path: String,
    body: Any? = null,
    queryParams: Map<String, String>? = null,
): Result<T> = runCatching {
    client.invoke(method, path, body, queryParams).body()
}
```

- **`inline` + `reified T`** — Ktor's `body<T>()` needs a reified type for deserialization.
- **`runCatching { ... }`** — converts thrown `AppError` (from `HttpResponseValidator`) into `Result.Failure(AppError.*)`. Callers handle via `.getOrThrow()` inside `safeLaunch`.

### Return types

- `Result<T>` for **all** endpoints. `T` is `Unit`, a single DTO, or a `List<DTO>`.
- **Never** `T` directly (errors are values, not exceptions to skip the type system).
- **Never** `Flow<DTO>` — `GrippoApi` is request-response only. Observation is a DAO concern.

## DTOs

### Package layout

```
:data-services:backend/src/commonMain/kotlin/com/<org>/<product>/services/backend/dto/
  <area>/
    <Name>Response.kt           // server → client
    <Name>Body.kt               // client → server
```

`<area>` matches the API section (`auth`, `training`, `user`, `goal`, `push-token`, ...).

### Shape

```kotlin
@Serializable
public data class TrainingResponse(
    @SerialName("repetitions")     val repetitions: Int? = null,
    @SerialName("createdAt")       val createdAt: String? = null,
    @SerialName("duration")        val duration: Long? = null,
    @SerialName("exercises")       val exercises: List<ExerciseResponse> = emptyList(),
    @SerialName("id")              val id: String? = null,
    @SerialName("intensity")       val intensity: Float? = null,
    @SerialName("volume")          val volume: Float? = null,
    @SerialName("updatedAt")       val updatedAt: String? = null,
    @SerialName("profileId")       val profileId: String? = null,
    @SerialName("userId")          val userId: String? = null,
)

@Serializable
public data class ExerciseResponse(
    @SerialName("repetitions")        val repetitions: Int? = null,
    @SerialName("createdAt")          val createdAt: String? = null,
    @SerialName("id")                 val id: String? = null,
    @SerialName("intensity")          val intensity: Float? = null,
    @SerialName("iterations")         val iterations: List<IterationResponse> = emptyList(),
    @SerialName("name")               val name: String? = null,
    @SerialName("volume")             val volume: Float? = null,
    @SerialName("trainingId")         val trainingId: String? = null,
    @SerialName("exerciseExampleId")  val exerciseExampleId: String? = null,
    @SerialName("exerciseExample")    val exerciseExample: ExerciseExampleResponse? = null,
    @SerialName("updatedAt")          val updatedAt: String? = null,
)
```

### Rules

1. **`@Serializable public data class`**. Always `data class`.
2. **All scalar fields nullable + default `= null`.** This is the **canonical defense** against partial backend responses, optional fields, and breaking schema changes.
3. **Collection fields default to `emptyList()`.** Backend may omit empty arrays; default to empty so the field is never `null`.
4. **`@SerialName("...")` on every field.** Even when the Kotlin name matches — explicit serial names are unambiguous and survive Kotlin renames.
5. **Names mirror backend** (camelCase). Don't transform `created_at` → `createdAt` via serial-name magic; if backend uses snake_case, use `@SerialName("created_at") val createdAt: String? = null`.
6. **No business logic, no computed properties.** DTOs are pure transport.
7. **One DTO per file** for top-level types. Tightly-related DTOs (e.g. `TrainingResponse` + `ExerciseResponse` + `IterationResponse` all returned by the same endpoint) can share a file.

### Why everything nullable

Backend evolution moves faster than the mobile release cycle. If a field is removed, mobile crashes without nullable defaults. If a field is added, `ignoreUnknownKeys = true` saves us. The combination of both is what keeps the client resilient.

The downside: nullable fields propagate through mappers. The `:data-mappers:dto-to-entity` and `:data-mappers:dto-to-domain` modules handle this — see `07-mappers/03-null-safety.md`.

## Bodies (request payloads)

```kotlin
@Serializable
public data class TrainingBody(
    @SerialName("duration")     val duration: Long,
    @SerialName("exercises")    val exercises: List<ExerciseBody>,
    @SerialName("intensity")    val intensity: Float,
    @SerialName("volume")       val volume: Float,
    @SerialName("repetitions")  val repetitions: Int,
)

@Serializable
public data class ExerciseBody(
    @SerialName("repetitions")        val repetitions: Int,
    @SerialName("intensity")          val intensity: Float,
    @SerialName("iterations")         val iterations: List<IterationBody>,
    @SerialName("name")               val name: String,
    @SerialName("volume")             val volume: Float,
    @SerialName("exerciseExampleId")  val exerciseExampleId: String?,
)

@Serializable
public data class IterationBody(
    @SerialName("repetitions")     val repetitions: Int,
    @SerialName("extraWeight")     val extraWeight: Float?,
    @SerialName("bodyWeight")      val bodyWeight: Float?,
    @SerialName("bodyMultiplier")  val bodyMultiplier: Float?,
    @SerialName("assistWeight")    val assistWeight: Float?,
    @SerialName("externalWeight")  val externalWeight: Float?,
)
```

Bodies are usually **non-nullable** — the client knows what it's sending, and `null` would mean "unset" rather than "unknown". Exception: optional fields the user may have left blank (`externalWeight: Float?`, `exerciseExampleId: String?` when a draft exercise has no example yet). Bodies typically **omit** the `= null` default — the call site is required to make the decision explicitly.

## `ClientLogger`

```kotlin
@Single
internal class ClientLogger : Logger {

    override fun log(message: String) {
        val emojiLine = if ((message.contains("RESPONSE: 200") || message.contains("RESPONSE: 201"))
            && message.contains("REQUEST").not()
        ) {
            "🟩🟩🟩🟩🟩🟩"
        } else if (message.contains("REQUEST").not()) {
            "🟥🟥🟥🟥🟥🟥"
        } else {
            "🟨🟨🟨🟨🟨🟨"
        }

        val formattedMessage = message
            .split("\n")
            .joinToString("\n")

        AppLogger.Network.log("$emojiLine HTTP LOG $emojiLine\n$formattedMessage")
    }
}
```

Routes Ktor's `Logging` plugin output to `AppLogger.Network` with color emojis (🟩 success, 🟥 error, 🟨 request). The colors aid visual scanning in the rolling log buffer.

## Anti-patterns

- **Subgrouping `GrippoApi`** into `AuthApi`, `TrainingsApi`, `UserApi`. Flatness is intentional — discovery is easier.
- **Non-nullable DTO scalar fields.** Breaks on backend schema drift.
- **`@Serializable` on a `sealed class` DTO without `@SerialName`** — kotlinx-serialization needs discriminator names.
- **Inline path strings duplicated across methods.** Two endpoints sharing `/users/X` is fine; if a third appears, consider a `companion object Paths { const val USERS = "/users" }` block.
- **Returning `T` instead of `Result<T>`** — every endpoint can fail.
- **Logging request/response bodies in release** without redaction. PII risk.
- **Endpoints that mutate state but return `Result<T>` of a specific shape** — when the server response is opaque, return `Result<Unit>`.
