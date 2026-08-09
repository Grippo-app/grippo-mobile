# `<Product>Api` and DTOs

Self-contained reference for the API surface and DTO rules.

> **Illustrative domain.** Code uses `Note` / `Tag` / `User` as the generic `<Entity>` /
> `<RelatedEntity>`. Substitute identifiers from your product domain.

`<Product>Api` is a **flat, one-method-per-endpoint** class. No subgrouping, no sub-services, no
resource-style classes. Just methods grouped by **comments** (`/* * * Auth service * * */`).
This is intentional — it keeps the entire HTTP contract visible in one file. The concrete class
name is `<Product>Api` (PascalCase product slot from project config).

---

## Class shape (EXAMPLE)

```kotlin
@Single
public class <Product>Api internal constructor(private val client: BackendClient) {

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

    public suspend fun updateUser(body: UserBody): Result<UserResponse> =
        request(method = HttpMethod.Put, path = "/users", body = body)

    /* * * * * * * * * * * * * * * * *
     *  <Entity> service
     * * * * * * * * * * * * * * * * */

    public suspend fun get<Entities>(start: String, end: String): Result<List<<Entity>Response>> =
        request(
            method = HttpMethod.Get,
            path = "/<entity_table>s",
            queryParams = mapOf("start" to start, "end" to end),
        )

    public suspend fun set<Entity>(body: <Entity>Body): Result<IdResponse> =
        request(method = HttpMethod.Post, path = "/<entity_table>s", body = body)

    public suspend fun update<Entity>(id: String, body: <Entity>Body): Result<Unit> =
        request(
            method = HttpMethod.Put,
            path = "/<entity_table>s",
            body = body,
            queryParams = mapOf("id" to id),
        )

    public suspend fun delete<Entity>(id: String): Result<Unit> =
        request(method = HttpMethod.Delete, path = "/<entity_table>s/$id")

    /* * * * * * * * * * * * * * * * *
     *  Utilities
     * * * * * * * * * * * * * * * * */

    private suspend inline fun <reified T> request(
        method: HttpMethod,
        path: String,
        body: Any? = null,
        queryParams: Map<String, String>? = null,
    ): Result<T> = runCatching {
        client.invoke(method, path, body, queryParams).body<T>()
    }
}
```

---

## Conventions

### One method per endpoint (MUST)

`GET /<entity_table>s?start&end` → `suspend fun get<Entities>(start: String, end: String): Result<List<<Entity>Response>>`.

`POST /<entity_table>s` → `suspend fun set<Entity>(body: <Entity>Body): Result<IdResponse>`.

Method name reflects the action verb (`get`, `set`, `update`, `delete`, `register`, `login`,
`refresh`) — not the HTTP verb. `set` is used instead of `post` to read more naturally at the
call site (`api.set<Entity>(...)`).

### Section comments (SHOULD)

Endpoints are grouped by domain (`Auth`, `Push token`, `User`, `<Entity>`, …). The block-comment
delimiter is consistent:

```kotlin
/* * * * * * * * * * * * * * * * *
 *  <Entity> service
 * * * * * * * * * * * * * * * * */
```

Useful when the file grows to 50+ endpoints. Keep sections roughly ordered to match the backend
OpenAPI sections.

### `request<T>` helper (EXAMPLE)

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
- **`runCatching { ... }`** — converts thrown `AppError` (from `HttpResponseValidator`) into
  `Result.Failure(AppError.*)`. Callers handle via `.getOrThrow()` inside `safeLaunch`.

### Return types (MUST)

- `Result<T>` for **all** endpoints. `T` is `Unit`, a single DTO, or a `List<DTO>`.
- **Never** `T` directly (errors are values, not exceptions to skip the type system).
- **Never** `Flow<DTO>` — `<Product>Api` is request-response only. Observation is a DAO concern.

---

## DTOs

### Package layout (NORMATIVE)

```
:data-services:backend/src/commonMain/kotlin/com/<org>/<product>/services/backend/dto/
  <area>/
    <Name>Response.kt           // server → client
    <Name>Body.kt               // client → server
```

`<area>` matches the API section (`auth`, `<entity>`, `user`, `<related>`, `push-token`, …).

### Shape (EXAMPLE)

```kotlin
@Serializable
public data class <Entity>Response(
    @SerialName("id")              val id: String? = null,
    @SerialName("title")           val title: String? = null,
    @SerialName("body")            val body: String? = null,
    @SerialName("createdAt")       val createdAt: String? = null,
    @SerialName("updatedAt")       val updatedAt: String? = null,
    @SerialName("profileId")       val profileId: String? = null,
    @SerialName("userId")          val userId: String? = null,
    @SerialName("<relateds>")      val <relateds>: List<<RelatedEntity>Response> = emptyList(),
)

@Serializable
public data class <RelatedEntity>Response(
    @SerialName("id")        val id: String? = null,
    @SerialName("<entity>Id")    val <entity>Id: String? = null,
    @SerialName("name")      val name: String? = null,
    @SerialName("color")     val color: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("items")     val items: List<ItemResponse> = emptyList(),
)
```

### DTO rules (MUST)

1. **`@Serializable public data class`**. Always `data class`.
2. **All scalar fields nullable + default `= null`.** This is the **canonical defense** against
   partial backend responses, optional fields, and breaking schema changes.
3. **Collection fields default to `emptyList()`.** Backend may omit empty arrays; default to
   empty so the field is never `null`.
4. **`@SerialName("...")` on every field.** Even when the Kotlin name matches — explicit serial
   names are unambiguous and survive Kotlin renames.
5. **Names mirror backend** (camelCase). Don't transform `created_at` → `createdAt` via
   serial-name magic; if backend uses snake_case, use `@SerialName("created_at") val createdAt: String? = null`.
6. **No business logic, no computed properties.** DTOs are pure transport.
7. **One DTO per file** for top-level types. Tightly-related DTOs (e.g. `<Entity>Response` +
   `<RelatedEntity>Response` + `ItemResponse` all returned by the same endpoint) can share a file.
8. **Closed-set fields stay raw `String?` — never a Kotlin enum.** A field drawn from a backend
   dictionary (type, status, kind, category, …) is a `String?` here; the DTO is the wire replica.
   The typed `<X>Enum` is produced downstream by the `…→domain` mapper. Likewise timestamps stay
   `String?` (promoted to `LocalDateTime` downstream) and durations stay numeric (promoted to
   `Duration` downstream). Strict typing is a **domain** concern, not a DTO one.

### Why everything nullable (REFERENCE)

Backend evolution moves faster than the mobile release cycle. If a field is removed, mobile
crashes without nullable defaults. If a field is added, `ignoreUnknownKeys = true` saves us. The
combination of both is what keeps the client resilient. The downside: nullable fields propagate
through mappers — the `:data-mappers:dto-to-entity` and `:data-mappers:dto-to-domain` modules
handle this.

---

## Bodies (request payloads) (EXAMPLE)

```kotlin
@Serializable
public data class <Entity>Body(
    @SerialName("title")  val title: String,
    @SerialName("body")   val body: String,
    @SerialName("<relateds>") val <relateds>: List<<RelatedEntity>Body>,
)

@Serializable
public data class <RelatedEntity>Body(
    @SerialName("name")   val name: String,
    @SerialName("color")  val color: String?,
    @SerialName("items")  val items: List<ItemBody>,
)

@Serializable
public data class ItemBody(
    @SerialName("label")     val label: String,
    @SerialName("value")     val value: String?,
    @SerialName("metadata")  val metadata: String?,
)
```

Bodies are usually **non-nullable** — the client knows what it's sending, and `null` would mean
"unset" rather than "unknown". Exception: optional fields the user may have left blank
(`value: String?`, `color: String?` when the user did not choose one). Bodies typically **omit**
the `= null` default — the call site is required to make the decision explicitly.

---

## `ClientLogger` (EXAMPLE)

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

Routes Ktor's `Logging` plugin output to `AppLogger.Network` with color emojis (🟩 success,
🟥 error, 🟨 request). The colors aid visual scanning in the single append-only `app.log` file
under `${user.home}/<product>/logs/` (no rotation; cleared via `AppLogger.clearLogFile()`).

---

## Anti-patterns (MUST)

- **Subgrouping `<Product>Api`** into `AuthApi`, `<Entity>Api`, `UserApi`. Flatness is
  intentional — discovery is easier.
- **Non-nullable DTO scalar fields.** Breaks on backend schema drift.
- **`@Serializable` on a `sealed class` DTO without `@SerialName`** — kotlinx-serialization needs
  discriminator names.
- **Inline path strings duplicated across methods.** Two endpoints sharing `/users/X` is fine;
  if a third appears, consider a `companion object Paths { const val USERS = "/users" }` block.
- **Returning `T` instead of `Result<T>`** — every endpoint can fail.
- **Logging request/response bodies in release** without redaction. PII risk.
- **Endpoints that mutate state but return `Result<T>` of a specific shape** — when the server
  response is opaque, return `Result<Unit>`.
