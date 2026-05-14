# Add a Mapper

How to add a new mapping (e.g. `UserNotificationResponse → NotificationEntity`).

## Steps

### 1. Identify the direction

Pick the right `:data-mappers:*` module:

| From | To | Module |
|---|---|---|
| DTO `*Response` | `*Entity` | `:data-mappers:dto-to-entity` |
| `*Entity` / `*Pack` | Domain `<X>` | `:data-mappers:entity-to-domain` |
| DTO | Domain | `:data-mappers:dto-to-domain` |
| Domain | State (`*State`, `UiText`, `*FormatState`) | `:data-mappers:domain-to-state` |
| State | Domain | `:data-mappers:state-to-domain` |
| Domain | `*Entity` (for drafts) | `:data-mappers:domain-to-entity` |
| Domain | `*Body` | `:data-mappers:domain-to-dto` |

### 2. Create the file

```
data-mappers/dto-to-entity/src/commonMain/kotlin/com/<org>/<product>/dto/entity/notifications/
  NotificationMapper.kt
```

Mapper module directories use slashes (`com/<org>/<product>/dto/entity/<area>/`), matching the `package` declaration `package com.<org>.<product>.dto.entity.<area>`. This is the opposite convention of `:data-features:*` modules (which intentionally use dotted directories like `com/grippo/data.features.trainings/`). Match the `<area>` subpackage to the data domain (`notifications`, `training`, `goal`, ...).

### 3. Write the mapper

`NotificationMapper.kt`:

```kotlin
package com.<org>.<product>.dto.entity.notifications

import com.<org>.<product>.services.backend.dto.notifications.UserNotificationResponse
import com.<org>.<product>.services.database.entity.NotificationEntity
import com.<org>.<product>.toolkit.logger.AppLogger

public fun UserNotificationResponse.toEntityOrNull(profileId: String): NotificationEntity? {
    val id = AppLogger.Mapping.log(id) { "UserNotificationResponse.id is null" } ?: return null
    val title = AppLogger.Mapping.log(title) { "UserNotificationResponse.title is null" } ?: return null
    val body = AppLogger.Mapping.log(body) { "UserNotificationResponse.body is null" } ?: return null
    val createdAt = AppLogger.Mapping.log(createdAt) { "UserNotificationResponse.createdAt is null" } ?: return null

    return NotificationEntity(
        id = id,
        profileId = profileId,
        title = title,
        body = body,
        createdAt = createdAt,
        read = read ?: false,
    )
}

public fun List<UserNotificationResponse>.toEntities(profileId: String): List<NotificationEntity> =
    mapNotNull { it.toEntityOrNull(profileId) }
```

### 4. Confirm dependencies

The mapper module's `build.gradle.kts` must depend on:

- The **source** module (here: `:data-services:backend` for DTOs).
- The **target** module (here: `:data-services:database` for entities).
- `:toolkit:logger` for `AppLogger.Mapping`.

Existing mapper modules already have these — usually no edit needed.

### 5. Use in a Repository

```kotlin
@Single(binds = [NotificationsRepository::class])
internal class NotificationsRepositoryImpl(
    private val api: GrippoApi,
    private val notificationDao: NotificationDao,
    private val userActiveDao: UserActiveDao,
    private val userDao: UserDao,
) : NotificationsRepository {

    override suspend fun getUserNotifications(): Result<Unit> {
        val profileId = userActiveDao.get()
            .firstOrNull()
            ?.let { userDao.getById(it).firstOrNull()?.profileId }
            ?: return Result.failure(IllegalStateException("no active user"))
        val response = api.getNotifications()
        response.onSuccess { dtos ->
            val entities = dtos.toEntities(profileId)
            notificationDao.insertAll(entities)
        }
        return response.map { }
    }
}
```

`UserActiveDao.get()` returns the active **`userId`** (a `Flow<String?>`), not the profile id. Translate to `profileId` via `userDao.getById(userId).firstOrNull()?.profileId` — the reference repo uses this two-step lookup in `TrainingRepositoryImpl.setDraftTraining`, `ExcludedMusclesRepositoryImpl`, and `ExcludedEquipmentsRepositoryImpl`. Skip the translation only when the foreign key is `userId` itself.

## Patterns by direction

### DTO → Entity / Domain

Use `AppLogger.Mapping.log(value) { msg } ?: return null` for required fields. See `07-mappers/03-null-safety.md`.

### Entity → Domain

No null checks — entities are non-null by contract:

```kotlin
public fun NotificationEntity.toDomain(): UserNotification = UserNotification(
    id = id,
    title = title,
    body = body,
    createdAt = DateTimeUtils.toLocalDateTime(createdAt),
    read = read,
)

public fun List<NotificationEntity>.toDomain(): List<UserNotification> = map { it.toDomain() }
```

### Domain → State

Translate types (e.g. `LocalDateTime` → `String` via `DateTimeUtils.format(...)`), wrap strings in `UiText`, wrap collections in `ImmutableList`:

```kotlin
public fun UserNotification.toState(): UserNotificationRowState = UserNotificationRowState(
    id = id,
    title = UiText.Str(title),
    body = UiText.Str(body),
    createdAt = DateTimeUtils.format(createdAt, DateFormat.DateOnly.DateMmmDdYyyy),
    read = read,
)

public fun List<UserNotification>.toState(): ImmutableList<UserNotificationRowState> =
    map { it.toState() }.toImmutableList()
```

### Domain → DTO Body

Reverse type translation:

```kotlin
public fun NotificationPreferences.toBody(): NotificationPreferencesBody = NotificationPreferencesBody(
    pushEnabled = pushEnabled,
    emailEnabled = emailEnabled,
)
```

## Verify

```bash
./gradlew :shared:assembleSharedDebugXCFramework
```

The mapper module compiles in parallel with others. A build failure here is local — fix the mapper without touching consumers.

## Common mistakes

- **`!!` on a nullable DTO field.** Forbidden. Use `?: return null` (with `AppLogger.Mapping.log`).
- **Defaulting a required field to a placeholder** (`val id = dto.id ?: "missing"`). The row is invalid; drop it.
- **Logging the entire DTO** instead of per-field. Loses diagnostic value when a single field goes missing.
- **Adding business logic** in the mapper (e.g. computing volume from iterations). Mappers are pure translation.
- **`@Composable` annotation** on a mapper. Mappers don't know Compose.
- **Cross-mapper imports** (`:dto-to-entity` importing from `:entity-to-domain`). Forbidden. Compose at the call site instead.
- **Sharing a mapper across `<area>` subpackages.** One area per file.
- **`@Single` / `@Factory` on mapper.** Mappers are top-level functions; no DI.
- **Mapping with a class instead of an extension function.** Top-level `fun X.toY()` is the convention.
