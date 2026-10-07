# Cookbook — add an API endpoint

Self-contained reference for the add-endpoint recipe.

> **Concrete example.** The example identifiers below (`Notifications`, `Item`, etc.) are
> illustrative; the steps apply to any endpoint. Substitute identifiers from your product domain.

How to add a new endpoint to `<Product>Api`. Background rules:
[`backend-client.md`](backend-client.md), [`dtos-and-api.md`](dtos-and-api.md).

---

## Step 1. Confirm the contract (MUST)

The endpoint must already be live on the backend. Check `backendContractEnabled` in the project
config for which contract source to use:

- **Gate `false`** — check the Swagger / OpenAPI docs at `https://<product-domain>/docs` (or your
  equivalent) by hand.
- **Gate `auto` / `true`, snapshot present** — look the endpoint up in
  the `inventory` path returned by `cd orchestrator/api-contract && npm run
  --silent contract:paths` (method + path, or `operationId`) for auth and
  errors; then read `<areasDir>/<area>.json` from the same resolver output for the
  field-level contract: types, `required`, `nullable_declared` / `nullable_observed`, enums.
- **Endpoint missing from the inventory** — with gate `auto` or `true`, stop
  (`BLOCKED`) and refresh the snapshot (Backend Test + Refresh, or `npm run
  contract:probe` followed by the matching `contract:refresh-*` in
  `orchestrator/api-contract/`). Do not infer the shape from task text.
- **No snapshot at all** — gate `auto` may continue unrelated greenfield work,
  but endpoint/DTO work remains `BLOCKED`; gate `true` also fails validation.

The backend owns the contract. Mobile doesn't invent endpoints.

---

## Step 2. Define the DTO(s) (NORMATIVE)

Identify the area: `auth`, `user`, `notifications`, etc.

```
:data-services:backend/src/commonMain/kotlin/com/<org>/<product>/services/backend/dto/<area>/
  <Name>Response.kt          // for GET responses
  <Name>Body.kt              // for POST/PUT bodies
```

`<Name>Response.kt`:

```kotlin
package com.<org>.<product>.services.backend.dto.notifications

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class UserNotificationResponse(
    @SerialName("id")        val id: String? = null,
    @SerialName("title")     val title: String? = null,
    @SerialName("body")      val body: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("read")      val read: Boolean? = null,
)
```

`<Name>Body.kt`:

```kotlin
package com.<org>.<product>.services.backend.dto.notifications

@Serializable
public data class NotificationReadBody(
    @SerialName("ids") val ids: List<String>,
)
```

DTO rules: see [`dtos-and-api.md`](dtos-and-api.md).

---

## Step 3. Add method to `<Product>Api` (NORMATIVE)

In the right section comment (or add a new section):

```kotlin
/* * * * * * * * * * * * * * * * *
 *  Notifications service
 * * * * * * * * * * * * * * * * */

public suspend fun getNotifications(): Result<List<UserNotificationResponse>> =
    request(method = HttpMethod.Get, path = "/notifications")

public suspend fun markNotificationRead(id: String): Result<Unit> =
    request(method = HttpMethod.Put, path = "/notifications/$id/read")

public suspend fun markAllNotificationsRead(): Result<Unit> =
    request(method = HttpMethod.Put, path = "/notifications/read-all")
```

For methods with query params:

```kotlin
public suspend fun searchNotifications(query: String, page: Int): Result<List<UserNotificationResponse>> =
    request(
        method = HttpMethod.Get,
        path = "/notifications/search",
        queryParams = mapOf("q" to query, "page" to page.toString()),
    )
```

For methods with a body:

```kotlin
public suspend fun markNotificationsRead(body: NotificationReadBody): Result<Unit> =
    request(method = HttpMethod.Put, path = "/notifications/read", body = body)
```

---

## Step 4. Add the mappers (NORMATIVE)

`:data-mappers:dto-to-entity/notifications/NotificationMapper.kt`:

```kotlin
public fun UserNotificationResponse.toEntityOrNull(profileId: String): NotificationEntity? {
    val id = AppLogger.Mapping.log(id) { "UserNotificationResponse.id is null" } ?: return null
    // … log-guard the remaining non-null fields the same way, then build the entity:
    return NotificationEntity(id = id, profileId = profileId /* , … */)
}

public fun List<UserNotificationResponse>.toEntities(profileId: String): List<NotificationEntity> =
    mapNotNull { it.toEntityOrNull(profileId) }
```

`:data-mappers:domain-to-dto/notifications/NotificationMapper.kt`:

```kotlin
public fun NotificationRead.toBody(): NotificationReadBody = NotificationReadBody(
    ids = ids,
)
```

---

## Step 5. Use in Repository / Feature (NORMATIVE)

```kotlin
override suspend fun getUserNotifications(): Result<Unit> {
    val profileId = userActiveDao.get()
        .firstOrNull()
        ?.let { userDao.getById(it).firstOrNull()?.profileId }
        ?: return Result.success(Unit) // no active user yet — no-op
    val response = api.getNotifications()
    response.onSuccess { dtos ->
        val entities = dtos.toEntities(profileId)
        if (entities.isEmpty()) {
            notificationDao.deleteAll()
        } else {
            notificationDao.deleteAllExceptIds(entities.map { it.id })
            notificationDao.insertAll(entities)
        }
    }
    return response.map { }
}

override suspend fun markNotificationRead(id: String): Result<Unit> {
    val response = api.markNotificationRead(id)
    response.onSuccess { notificationDao.markRead(id) }
    return response.map { }
}
```

`userActiveDao.get()` returns the active **userId**, not the profile id — perform the two-step
`userId → profileId` lookup as shown.

---

## Step 6. Verify (MUST)

```bash
./gradlew :shared:assembleSharedDebugXCFramework
./gradlew :androidApp:assembleDebug
```

Both platforms must compile. Then run the app and trigger the endpoint manually.

---

## Endpoint patterns (EXAMPLE)

### GET with optional query params

```kotlin
public suspend fun getItems(filter: String?, since: String?): Result<List<ItemResponse>> {
    val params = buildMap {
        filter?.let { put("filter", it) }
        since?.let { put("since", it) }
    }
    return request(
        method = HttpMethod.Get,
        path = "/items",
        queryParams = params.takeIf { it.isNotEmpty() },
    )
}
```

### POST returning the created resource ID

```kotlin
public suspend fun createItem(body: ItemBody): Result<IdResponse> =
    request(method = HttpMethod.Post, path = "/items", body = body)
```

`IdResponse` is a common shape:

```kotlin
@Serializable
public data class IdResponse(
    @SerialName("id") val id: String? = null,
)
```

### PUT with id in path and body

```kotlin
public suspend fun updateItem(id: String, body: ItemBody): Result<Unit> =
    request(method = HttpMethod.Put, path = "/items/$id", body = body)
```

### DELETE

```kotlin
public suspend fun deleteItem(id: String): Result<Unit> =
    request(method = HttpMethod.Delete, path = "/items/$id")
```

### Multipart upload (e.g. image)

Rarely used. This template doesn't include multipart endpoints. If needed:

```kotlin
public suspend fun uploadAvatar(bytes: ByteArray, filename: String): Result<UrlResponse> = runCatching {
    client.invoke(
        method = HttpMethod.Post,
        path = "/users/avatar",
        body = MultiPartFormDataContent(
            formData {
                append("file", bytes, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$filename\"")
                })
            }
        ),
    ).body<UrlResponse>()
}
```

Wrapping a custom multipart in `runCatching` directly (not via the `request<T>` helper) —
Multipart needs special body handling.

---

## Common mistakes (MUST)

- **Non-nullable DTO response fields.** Backend evolution breaks the client.
- **Forgetting `@SerialName`.** Kotlin's field name matches the JSON field by default, but explicit
  `@SerialName` survives renames.
- **`Boolean` response field without a default.** If the backend omits it, deserialization fails.
  Always nullable + default `= null`.
- **Inline `runCatching` instead of the `request<T>` helper.** Diverges from the convention (except
  multipart uploads — see the upload example above).
- **Adding the method outside any section comment.** File grows past 200 endpoints; section
  comments are mandatory.
- **Calling `<Product>Api` from a ViewModel.** The VM only sees `<X>Feature` from
  `:data-features:feature-api`.
- **Skipping the DAO update** after a successful mutation. The local cache lags.
- **Skipping the mapper.** Inline DTO → Entity conversion in Repository is forbidden.
