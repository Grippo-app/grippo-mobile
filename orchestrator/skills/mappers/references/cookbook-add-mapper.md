# Cookbook — Add a Mapper

> Conventions live in [`mapping-conventions.md`](mapping-conventions.md);
> null policy in [`null-safety-and-logging.md`](null-safety-and-logging.md).

> **Concrete example.** The example task and identifiers below
> (`UserNotificationResponse → NotificationEntity`, etc.) are illustrative; the
> recipe steps apply to any feature you build with this template.

How to add a new mapping (e.g. `UserNotificationResponse → NotificationEntity`).

## Step 1. Identify the direction (NORMATIVE)

Pick the right `:data-mappers:*` module:

| From | To | Module |
|---|---|---|
| DTO `*Response` | `*Entity` | `:data-mappers:dto-to-entity` |
| `*Entity` / `*Pack` | Domain `<X>` | `:data-mappers:entity-to-domain` |
| DTO | Domain | `:data-mappers:dto-to-domain` |
| Domain | State (`*State`, `*FormatState`) | `:data-mappers:domain-to-state` |
| State | Domain | `:data-mappers:state-to-domain` |
| Domain | `*Entity` (for drafts) | `:data-mappers:domain-to-entity` |
| Domain | `*Body` | `:data-mappers:domain-to-dto` |

## Step 2. Create the file (MUST)

```
data-mappers/dto-to-entity/src/commonMain/kotlin/com/<org>/<product>/dto/entity/notifications/
  NotificationMapper.kt
```

Mapper module directories use slashes (`com/<org>/<product>/dto/entity/<area>/`),
matching the `package` declaration `package com.<org>.<product>.dto.entity.<area>`.
This is the opposite convention of `:data-features:*` modules (which intentionally
use dotted directories like `com/<org>/<product>/data.features.<feature>/`). Match
the `<area>` subpackage to the data domain.

## Step 3. Write the mapper (EXAMPLE)

`NotificationMapper.kt`:

```kotlin
package com.<org>.<product>.dto.entity.notifications

import com.<org>.<product>.services.backend.dto.notifications.UserNotificationResponse
import com.<org>.<product>.services.database.entity.NotificationEntity
import com.<org>.<product>.toolkit.logger.AppLogger

public fun UserNotificationResponse.toEntityOrNull(profileId: String): NotificationEntity? {
    val entityId = AppLogger.Mapping.log(id) { "UserNotificationResponse.id is null" } ?: return null
    val entityTitle = AppLogger.Mapping.log(title) { "UserNotificationResponse.title is null" } ?: return null
    val entityBody = AppLogger.Mapping.log(body) { "UserNotificationResponse.body is null" } ?: return null
    // dto→entity passes the raw key straight through (the entity is the replica); the entity
    // column is non-null, so guard it like any other required field — no enum parse here.
    val entityKind = AppLogger.Mapping.log(kind) { "UserNotificationResponse.kind is null" } ?: return null
    val entityCreatedAt = AppLogger.Mapping.log(createdAt) { "UserNotificationResponse.createdAt is null" } ?: return null

    return NotificationEntity(
        id = entityId,
        profileId = profileId,
        title = entityTitle,
        body = entityBody,
        kind = entityKind,
        createdAt = entityCreatedAt,
        read = read ?: false,
    )
}

public fun List<UserNotificationResponse>.toEntities(profileId: String): List<NotificationEntity> =
    mapNotNull { it.toEntityOrNull(profileId) }
```

## Step 4. Confirm dependencies (MUST)

The mapper module's `build.gradle.kts` must depend on:

- The **source** module (here: `:data-services:backend` for DTOs).
- The **target** module (here: `:data-services:database` for entities).
- `:toolkit:logger` for `AppLogger.Mapping`.

Existing mapper modules already have these — usually no edit needed. If a mapper
module's `build.gradle.kts` is missing a source/target/`:toolkit:logger` dep, that
is a wider task: stop and report
`BLOCKED: mapper module dependencies missing — <list>`.

## Step 5. Use in a Repository (EXAMPLE)

```kotlin
@Single(binds = [NotificationsRepository::class])
internal class NotificationsRepositoryImpl(
    private val api: <Product>Api,
    private val notificationDao: NotificationDao,
    private val userActiveDao: UserActiveDao,
    private val userDao: UserDao,
) : NotificationsRepository {

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
}
```

`UserActiveDao.get()` returns the active **`userId`** (a `Flow<String?>`), not the
profile id. Translate to `profileId` via
`userDao.getById(userId).firstOrNull()?.profileId` — every repository whose entity
has a `profileId` foreign key follows this two-step lookup. Skip the translation
only when the foreign key is `userId` itself.

## Patterns by direction (EXAMPLE)

### DTO → Entity / Domain

Use `AppLogger.Mapping.log(value) { msg } ?: return null` for required fields. See
[`null-safety-and-logging.md`](null-safety-and-logging.md).

### Entity → Domain

No null checks for scalar pass-throughs — entities are non-null by contract. Type
promotion still happens here (timestamp `String` → `LocalDateTime`, minutes `Long` →
`Duration`):

```kotlin
public fun NotificationEntity.toDomain(): UserNotification? {
    val mappedKind = AppLogger.Mapping.log(NotificationKindEnum.of(kind)) {
        "NotificationEntity $id has unrecognized kind: $kind"
    } ?: return null
    return UserNotification(
        id = id,
        title = title,
        body = body,
        kind = mappedKind,
        createdAt = DateTimeUtils.toLocalDateTime(createdAt),
        read = read,
    )
}

public fun List<NotificationEntity>.toDomain(): List<UserNotification> = mapNotNull { it.toDomain() }
```

A dictionary `String` → `<X>Enum` parse (or a missing `@Relation`) still guards with
`AppLogger.Mapping.log(...) ?: return null` — the per-row mapper then returns `<X>?`
(still named `toDomain`, **not** `toDomainOrNull` — the `OrNull` suffix is
DTO-source only) and the plural variant uses `mapNotNull`. A drop-on-unknown
`<X>Enum.of(...)` is guarded; a defaulted `of(...) ?: <DEFAULT>` is read bare:

```kotlin
public fun <X>Entity.toDomain(): <X>? {
    val mappedStatus = AppLogger.Mapping.log(<X>StatusEnum.of(status)) {
        "<X>Entity $id has unrecognized status: $status"
    } ?: return null

    return <X>(
        id = id,
        status = mappedStatus,                  // String → <X>StatusEnum (drop-on-unknown)
        duration = duration.minutes,            // Long (minutes) → Duration
        createdAt = DateTimeUtils.toLocalDateTime(createdAt),
    )
}

public fun List<<X>Entity>.toDomain(): List<<X>> = mapNotNull { it.toDomain() }
```

See [`mapping-conventions.md`](mapping-conventions.md) § "Enum dictionaries" for the
full round-trip.

### Domain → State

Translate types into their `*FormatState` carriers — numerics become
`AmountFormatState.of(value)`, dates become
`DateTimeFormatState.of(value, range, format)` (never a raw `LocalDateTime` and never
an eagerly-formatted `String`) — and wrap collections in `PersistentList`. No
`UiText` here — mappers don't construct localized strings; the ViewModel/screen wraps
for display:

```kotlin
public fun UserNotification.toState(): UserNotificationRowState = UserNotificationRowState(
    id = id,
    title = title,
    body = body,
    createdAt = DateTimeFormatState.of(
        value = createdAt,
        range = DateRangePresets.infinity(),
        format = DateFormat.DateOnly.DateMmmDdYyyy,
    ),
    read = read,
)

public fun List<UserNotification>.toState(): PersistentList<UserNotificationRowState> =
    map { it.toState() }.toPersistentList()
```

### Domain → DTO Body

Reverse type translation:

```kotlin
public fun NotificationPreferences.toBody(): NotificationPreferencesBody = NotificationPreferencesBody(
    pushEnabled = pushEnabled,
    emailEnabled = emailEnabled,
)
```

## Within a new data feature — the dto→entity + entity→domain pair (EXAMPLE)

When adding a new data feature, the minimum mapper pair is `dto-to-entity` +
`entity-to-domain`. The dto→entity step passes the raw enum key through (no parse);
the entity→domain step is the **only** enum-parse site.

`:data-mappers:dto-to-entity/notifications/NotificationMapper.kt`:

```kotlin
package com.<org>.<product>.dto.entity.notifications

public fun UserNotificationResponse.toEntityOrNull(profileId: String): NotificationEntity? {
    val id = AppLogger.Mapping.log(id) { "UserNotificationResponse.id is null" } ?: return null
    val title = AppLogger.Mapping.log(title) { "UserNotificationResponse.title is null" } ?: return null
    val body = AppLogger.Mapping.log(body) { "UserNotificationResponse.body is null" } ?: return null
    // dto→entity passes the raw key straight through (the entity is the replica); the entity
    // column is non-null, so guard it like any other required field — no enum parse here.
    val kind = AppLogger.Mapping.log(kind) { "UserNotificationResponse.kind is null" } ?: return null
    val createdAt = AppLogger.Mapping.log(createdAt) { "UserNotificationResponse.createdAt is null" } ?: return null
    return NotificationEntity(
        id = id,
        profileId = profileId,
        title = title,
        body = body,
        kind = kind,
        createdAt = createdAt,
        read = read ?: false,
    )
}

public fun List<UserNotificationResponse>.toEntities(profileId: String): List<NotificationEntity> =
    mapNotNull { it.toEntityOrNull(profileId) }
```

`:data-mappers:entity-to-domain/notifications/NotificationMapper.kt`:

```kotlin
// The entity→domain step is the ONLY enum parse site. Drop-on-unknown makes the per-row
// mapper nullable (named `toDomain`, not `toDomainOrNull` — `OrNull` is DTO-source only),
// which cascades to the plural via `mapNotNull`.
public fun NotificationEntity.toDomain(): UserNotification? {
    val mappedKind = AppLogger.Mapping.log(NotificationKindEnum.of(kind)) {
        "NotificationEntity $id has unrecognized kind: $kind"
    } ?: return null
    return UserNotification(
        id = id,
        title = title,
        body = body,
        kind = mappedKind,
        createdAt = DateTimeUtils.toLocalDateTime(createdAt),
        read = read,
    )
}

public fun List<NotificationEntity>.toDomain(): List<UserNotification> = mapNotNull { it.toDomain() }
```

## Verify (REFERENCE)

```bash
./gradlew :shared:assembleSharedDebugXCFramework
./gradlew :androidApp:assembleDebug
```

The mapper module compiles in parallel with others. A build failure here is local —
fix the mapper without touching consumers.

## Common mistakes (MUST)

- **`!!` on a nullable DTO field.** Forbidden. Use `?: return null` (with
  `AppLogger.Mapping.log`).
- **Defaulting a required field to a placeholder** (`val id = dto.id ?: "missing"`).
  The row is invalid; drop it.
- **Logging the entire DTO** instead of per-field. Loses diagnostic value when a
  single field goes missing.
- **Adding business logic** in the mapper (e.g. computing volume from iterations).
  Mappers are pure translation.
- **`@Composable` annotation** on a mapper. Mappers don't know Compose.
- **Cross-mapper imports** (`:dto-to-entity` importing from `:entity-to-domain`).
  Forbidden. Compose at the call site instead.
- **Sharing a mapper across `<area>` subpackages.** One area per file.
- **`@Single` / `@Factory` on mapper.** Mappers are top-level functions; no DI.
- **Mapping with a class instead of an extension function.** Top-level
  `fun X.toY()` is the convention.
