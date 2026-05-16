# Add an API Endpoint

How to add a new endpoint to `<Product>Api`.

## Steps

### 1. Confirm the contract

The endpoint must already be live on the backend. Check the Swagger / OpenAPI docs at `https://<host>/docs` (or your equivalent).

The backend owns the contract. Mobile doesn't invent endpoints.

### 2. Define the DTO(s)

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

Rules: see `06-data-layer/03-product-api-and-dtos.md`.

### 3. Add the method to `<Product>Api`

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

### 4. Add the mappers

`:data-mappers:dto-to-entity/notifications/NotificationMapper.kt`:

```kotlin
public fun UserNotificationResponse.toEntityOrNull(profileId: String): NotificationEntity? {
    val id = AppLogger.Mapping.log(id) { "UserNotificationResponse.id is null" } ?: return null
    // ... see 14-cookbook/04-add-mapper.md
}

public fun List<UserNotificationResponse>.toEntities(profileId: String): List<NotificationEntity> =
    mapNotNull { it.toEntityOrNull(profileId) }
```

`:data-mappers:domain-to-dto/notifications/NotificationMapper.kt`:

```kotlin
public fun NotificationReadRequest.toBody(): NotificationReadBody = NotificationReadBody(
    ids = ids,
)
```

### 5. Use in the corresponding Repository / Feature

```kotlin
override suspend fun getUserNotifications(): Result<Unit> {
    val profileId = userActiveDao.get()
        .firstOrNull()
        ?.let { userDao.getById(it).firstOrNull()?.profileId }
        ?: return Result.failure(IllegalStateException("no active user"))
    val response = api.getNotifications()
    response.onSuccess { dtos -> notificationDao.insertAll(dtos.toEntities(profileId)) }
    return response.map { }
}

override suspend fun markRead(id: String): Result<Unit> {
    val response = api.markNotificationRead(id)
    response.onSuccess { notificationDao.markRead(id) }
    return response.map { }
}
```

`userActiveDao.get()` returns the active **userId**, not the profile id — see `14-cookbook/04-add-mapper.md` step 5 for the two-step `userId → profileId` lookup.

### 6. Verify

```bash
./gradlew :shared:assembleSharedDebugXCFramework
```

Both platforms must compile. Then run the app and trigger the endpoint manually.

## Endpoint patterns

### GET with optional query params

```kotlin
public suspend fun getThings(filter: String?, since: String?): Result<List<ThingResponse>> {
    val params = buildMap {
        filter?.let { put("filter", it) }
        since?.let { put("since", it) }
    }
    return request(
        method = HttpMethod.Get,
        path = "/things",
        queryParams = params.takeIf { it.isNotEmpty() },
    )
}
```

### POST returning the created resource ID

```kotlin
public suspend fun createThing(body: ThingBody): Result<IdResponse> =
    request(method = HttpMethod.Post, path = "/things", body = body)
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
public suspend fun updateThing(id: String, body: ThingBody): Result<Unit> =
    request(method = HttpMethod.Put, path = "/things/$id", body = body)
```

### DELETE

```kotlin
public suspend fun deleteThing(id: String): Result<Unit> =
    request(method = HttpMethod.Delete, path = "/things/$id")
```

### Multipart upload (e.g. image)

Rarely used. The reference repo doesn't include multipart endpoints. If needed:

```kotlin
public suspend fun uploadAvatar(bytes: ByteArray, filename: String): Result<UrlResponse> = runCatching {
    client.invoke(
        method = HttpMethod.Post,
        path = "/users/avatar",
        body = MultiPartFormDataContent(
            formData {
                append("file", bytes, Headers.build {
                    append(HttpHeaders.ContentType, "image/jpeg")
                    append(HttpHeaders.ContentDisposition, "filename=$filename")
                })
            }
        ),
    ).body()
}
```

Wrapping a custom multipart in `runCatching` directly (not via the `request<T>` helper) — Multipart needs special body handling.

## Common mistakes

- **Non-nullable DTO response fields.** Backend evolution breaks the client.
- **Forgetting `@SerialName`.** Kotlin's field name matches the JSON field by default, but explicit `@SerialName` survives renames.
- **`Boolean` response field without a default.** If the backend omits it, deserialization fails. Always nullable + default `= null`.
- **Inline `runCatching` instead of the `request<T>` helper.** Diverges from the convention.
- **Adding the method outside any section comment.** File grows past 200 endpoints; section comments are mandatory.
- **Calling `<Product>Api` from a ViewModel.** The VM only sees `<X>Feature` from `:data-features:feature-api`.
- **Skipping the DAO update** after a successful mutation. The local cache lags.
- **Skipping the mapper.** Inline DTO → Entity conversion in Repository is forbidden.
