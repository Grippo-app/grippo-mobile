# Add a Data Feature

> **Concrete example.** The example task and identifiers below (`Note archive`, `tag-picker`, etc.) are illustrative; the recipe steps apply to any feature you build with this template.

How to add a new data feature module — e.g. `:data-features:notifications`.

## Steps

### 1. Add the public interface and domain models

In `:data-features:feature-api`:

```kotlin
// :data-features:feature-api/notifications/NotificationsFeature.kt
public interface NotificationsFeature {
    public fun observeUserNotifications(): Flow<List<UserNotification>>
    public suspend fun getUserNotifications(): Result<Unit>
    public suspend fun markNotificationRead(id: String): Result<Unit>
    public suspend fun markAllRead(): Result<Unit>
}

// :data-features:feature-api/notifications/models/UserNotification.kt
public data class UserNotification(
    val id: String,
    val title: String,
    val body: String,
    val createdAt: LocalDateTime,
    val read: Boolean,
)
```

### 2. Add the module to `settings.gradle.kts`

```kotlin
include(":data-features:notifications")
```

### 3. Create `:data-features:notifications/build.gradle.kts`

```kotlin
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("koin.annotation.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.data.features.notifications" }

    sourceSets.commonMain.dependencies {
        implementation(projects.dataFeatures.featureApi)
        implementation(projects.dataServices.backend)
        implementation(projects.dataServices.database)
        implementation(projects.dataMappers.dtoToEntity)
        implementation(projects.dataMappers.entityToDomain)
        implementation(projects.toolkit.dateUtils)

        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.datetime)
    }

    // Note: `:toolkit:logger` is consumed by mapper modules (for `AppLogger.Mapping`),
    // not by data-feature modules. No existing `:data-features:*` module declares it.
}
```

### 4. Create the repository, feature impl, and module

Directory:

```
:data-features:notifications/src/commonMain/kotlin/com/<org>/<product>/data.features.notifications/
  NotificationsFeatureModule.kt
  data/
    NotificationsRepositoryImpl.kt
  domain/
    NotificationsRepository.kt
    NotificationsFeatureImpl.kt
```

`NotificationsFeatureModule.kt`:

```kotlin
@Module(includes = [BackendModule::class, DatabaseModule::class])
@ComponentScan
public class NotificationsFeatureModule
```

`domain/NotificationsRepository.kt`:

```kotlin
internal interface NotificationsRepository {
    fun observeUserNotifications(): Flow<List<UserNotification>>
    suspend fun getUserNotifications(): Result<Unit>
    suspend fun markNotificationRead(id: String): Result<Unit>
    suspend fun markAllRead(): Result<Unit>
}
```

`data/NotificationsRepositoryImpl.kt`:

```kotlin
@Single(binds = [NotificationsRepository::class])
internal class NotificationsRepositoryImpl(
    private val api: <Product>Api,
    private val notificationDao: NotificationDao,
) : NotificationsRepository {

    override fun observeUserNotifications(): Flow<List<UserNotification>> =
        notificationDao.observe().map { entities -> entities.toDomain() }

    override suspend fun getUserNotifications(): Result<Unit> {
        val response = api.getNotifications()
        response.onSuccess { dtos ->
            val entities = dtos.toEntities()
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

    override suspend fun markAllRead(): Result<Unit> {
        val response = api.markAllNotificationsRead()
        response.onSuccess { notificationDao.markAllRead() }
        return response.map { }
    }
}
```

`domain/NotificationsFeatureImpl.kt`:

```kotlin
@Single(binds = [NotificationsFeature::class])
internal class NotificationsFeatureImpl(
    private val repository: NotificationsRepository,
) : NotificationsFeature {

    override fun observeUserNotifications(): Flow<List<UserNotification>> =
        repository.observeUserNotifications()

    override suspend fun getUserNotifications(): Result<Unit> = repository.getUserNotifications()

    override suspend fun markNotificationRead(id: String): Result<Unit> =
        repository.markNotificationRead(id)

    override suspend fun markAllRead(): Result<Unit> = repository.markAllRead()
}
```

### 5. Add the DAO and entity to `:data-services:database`

Entity:

```kotlin
// :data-services:database/entity/NotificationEntity.kt
@Entity(
    tableName = "notification",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["profileId"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["profileId"])],
)
public data class NotificationEntity(
    @PrimaryKey val id: String,
    val profileId: String,
    val title: String,
    val body: String,
    val createdAt: String,
    val read: Boolean,
)
```

The reference repo always spells out `Index(value = […])` — keep that form for consistency with existing entities.

DAO:

```kotlin
// :data-services:database/dao/NotificationDao.kt
@Dao
public interface NotificationDao {

    @Query("SELECT * FROM notification ORDER BY createdAt DESC")
    public fun observe(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public suspend fun insertAll(entities: List<NotificationEntity>)

    @Query("DELETE FROM notification WHERE id NOT IN (:keepIds)")
    public suspend fun deleteAllExceptIds(keepIds: List<String>)

    @Query("DELETE FROM notification")
    public suspend fun deleteAll()

    @Query("UPDATE notification SET read = 1 WHERE id = :id")
    public suspend fun markRead(id: String)

    @Query("UPDATE notification SET read = 1")
    public suspend fun markAllRead()
}
```

Register in `Database.kt`:

```kotlin
@Database(
    entities = [
        // ... existing entities
        NotificationEntity::class,
    ],
    version = N + 1,        // bump
    exportSchema = true,
)
public abstract class Database : RoomDatabase() {
    // ... existing accessors
    public abstract fun notificationDao(): NotificationDao
}
```

Add the DAO provider in `DatabaseModule.kt`:

```kotlin
@Single
internal fun provideNotificationDao(db: Database): NotificationDao = db.notificationDao()
```

Add a migration `Migration<N>To<N+1>.kt` for the new table (see `06-data-layer/06-room-migrations.md`):

```kotlin
internal object MigrationNToNPlusOne : Migration(N, N + 1) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `notification` (
                `id` TEXT NOT NULL,
                `profileId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `body` TEXT NOT NULL,
                `createdAt` TEXT NOT NULL,
                `read` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`profileId`) REFERENCES `user`(`profileId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_notification_profileId` ON `notification` (`profileId`)"
        )
    }
}
```

Add it to `DatabaseMigrations.all`:

```kotlin
val all: Array<Migration> = arrayOf(
    Migration2To3,
    Migration3To4,
    Migration4To5,
    MigrationNToNPlusOne,
)
```

### 6. Add the DTO and API endpoints to `:data-services:backend`

DTOs:

```kotlin
// :data-services:backend/dto/notifications/UserNotificationResponse.kt
@Serializable
public data class UserNotificationResponse(
    @SerialName("id")        val id: String? = null,
    @SerialName("title")     val title: String? = null,
    @SerialName("body")      val body: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("read")      val read: Boolean? = null,
)
```

Add to `<Product>Api`:

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

### 7. Add the mappers

`:data-mappers:dto-to-entity/notifications/NotificationMapper.kt`:

```kotlin
package com.<org>.<product>.dto.entity.notifications

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

`:data-mappers:entity-to-domain/notifications/NotificationMapper.kt`:

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

### 8. Wire up in `:shared`

`:shared/build.gradle.kts`:

```kotlin
sourceSets.commonMain.dependencies {
    // ...
    implementation(projects.dataFeatures.notifications)
}
```

`:shared/Koin.kt`:

```kotlin
modules(
    // ... existing
    NotificationsFeatureModule().module,
)
```

### 9. Verify

```bash
./gradlew :shared:assembleSharedDebugXCFramework
./gradlew :androidApp:assembleDebug
```

Both must build green. Then test the feature manually.

## Common mistakes

- **Forgetting `@Single(binds = [...])`** on the impl. Consumers can't `inject<Notifications*>()`.
- **Forgetting to bump `version = N+1`** in `@Database`. Schema export shows old data.
- **Adding the entity to `@Database`'s entities list** but skipping the migration. Destructive fallback kicks in; user data lost.
- **Including the new module in `:shared/build.gradle.kts`** but skipping `:shared/Koin.kt`. Runtime "no definition found".
- **Cross-module imports between data features.** Each `:data-features:<x>` only sees `:data-features:feature-api`.
- **Returning DTOs from the Feature.** Only domain models cross the API.
- **Forgetting to schedule range/list reconciliation.** Stale rows linger.
