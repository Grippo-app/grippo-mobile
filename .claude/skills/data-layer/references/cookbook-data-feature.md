# Cookbook — add a data feature

Self-contained reference for the end-to-end data-feature recipe.

> Steps 7 (mappers) and 8 (`:shared` wiring) are summarized; mapper authoring is owned
> by the mapper builder and the `:shared` wiring by the DI skill.

> **Concrete example.** The example identifiers below (`Notifications`, etc.) are illustrative;
> the steps apply to any feature you build with this template. Substitute identifiers from your
> product domain.

How to add a new data feature module — e.g. `:data-features:notifications`. Background rules:
[`module-structure.md`](module-structure.md), [`repositories.md`](repositories.md),
[`persistence-room.md`](persistence-room.md), [`dtos-and-api.md`](dtos-and-api.md).

---

## Step 1. Public interface and domain models (EXAMPLE)

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
    val kind: NotificationKindEnum,
    val createdAt: LocalDateTime,
    val read: Boolean,
)

// :data-features:feature-api/notifications/models/NotificationKindEnum.kt
// A closed-set backend field → a domain enum (never a raw String).
public enum class NotificationKindEnum(public val key: String) {
    SYSTEM(key = "system"),
    MENTION(key = "mention"),
    REMINDER(key = "reminder");

    public companion object {
        // Drop-on-unknown: the entity→domain mapper guards it with `?: return null`.
        public fun of(key: String?): NotificationKindEnum? = entries.firstOrNull { it.key == key }
    }
}
```

---

## Step 1.5. Add UseCase class (only if required) (EXAMPLE)

A `<Verb><Noun>UseCase` lives in `:data-features:feature-api` alongside the Feature interface.
Create one **only** when:

1. The operation **composes two or more Feature interfaces** (e.g. `LoginUseCase` calls both
   `AuthorizationFeature` and `UserFeature`).
2. The logic is **domain computation the ViewModel should not hold** (pure calculation, validation
   — like `ExerciseValidatorUseCase()` with zero dependencies).

```kotlin
// :data-features:feature-api/notifications/BuildNotificationSummaryUseCase.kt
public data class NotificationSummary(
    val unreadCount: Int,
    val hasMentions: Boolean,
)

public class BuildNotificationSummaryUseCase {
    public fun execute(notifications: List<UserNotification>): NotificationSummary =
        NotificationSummary(
            unreadCount = notifications.count { !it.read },
            hasMentions = notifications.any { it.kind == NotificationKindEnum.MENTION },
        )
}
```

The class carries **no Koin annotation** — no `@Factory`, no `@Single`. Register it in the existing
`FeatureApiModule`:

```kotlin
// :data-features:feature-api/FeatureApiModule.kt — add inside module { }:
single { BuildNotificationSummaryUseCase() }
```

`FeatureApiModule` is `@Module` only (no `@ComponentScan`) and uses
`@get:JvmName("module") public val module: ModuleObject = module { single { … }; … }`. This is the
sole hand-DSL exception.

---

## Step 2. Add module to `settings.gradle.kts` (EXAMPLE)

```kotlin
include(":data-features:notifications")
```

---

## Step 3. Create `build.gradle.kts` (EXAMPLE)

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
}
```

`:toolkit:logger` is consumed by mapper modules (for `AppLogger.Mapping`), not by data-feature
modules — no existing `:data-features:*` module declares it.

---

## Step 4. Repository, feature impl, and module (EXAMPLE)

Directory:

```
:data-features:notifications/src/commonMain/kotlin/com/<org>/<product>/data/features/notifications/
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
    private val userActiveDao: UserActiveDao,
    private val userDao: UserDao,
) : NotificationsRepository {

    override fun observeUserNotifications(): Flow<List<UserNotification>> =
        notificationDao.observe().map { entities -> entities.toDomain() }

    override suspend fun getUserNotifications(): Result<Unit> {
        val profileId = userActiveDao.get()
            .firstOrNull()
            ?.let { userDao.getById(it).firstOrNull()?.profileId }
            ?: return Result.success(Unit)
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

---

## Step 5. Add DAO and entity to database (EXAMPLE)

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
    val kind: String,                 // raw backend key — promoted to NotificationKindEnum at entity→domain
    val createdAt: String,
    val read: Boolean,
)
```

This template always spells out `Index(value = […])` — keep that form for consistency with
existing entities.

DAO:

```kotlin
// :data-services:database/dao/NotificationDao.kt
@Dao
public interface NotificationDao {

    @Query("SELECT * FROM notification ORDER BY createdAt DESC")
    public fun get(): Flow<List<NotificationEntity>>

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
    version = 6,            // bumped from 5
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

If this task is explicitly authorized to include a Room migration, add a separate
`Migration5To6.kt` for the new table — use your project's current → next version
(see [`cookbook-room-migration.md`](cookbook-room-migration.md)). Otherwise stop and
split/defer the migration to the room-migration flow:

```kotlin
internal object Migration5To6 : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA defer_foreign_keys = ON")

        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `notification` (
                `id` TEXT NOT NULL,
                `profileId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `body` TEXT NOT NULL,
                `kind` TEXT NOT NULL,
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
    Migration5To6,
)
```

---

## Step 6. Add DTO and API endpoints to backend (EXAMPLE)

DTOs:

```kotlin
// :data-services:backend/dto/notifications/UserNotificationResponse.kt
@Serializable
public data class UserNotificationResponse(
    @SerialName("id")        val id: String? = null,
    @SerialName("title")     val title: String? = null,
    @SerialName("body")      val body: String? = null,
    @SerialName("kind")      val kind: String? = null,
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

---

## Step 7. Add the mappers (summary)

> Full mapper authoring is owned by the mapper builder
> (`:data-mappers:*`); the key rules a data-feature builder must know:

- `:data-mappers:dto-to-entity` — `UserNotificationResponse.toEntityOrNull(profileId): NotificationEntity?`
  log-guards every required non-null field via `AppLogger.Mapping.log(field) { "…is null" } ?: return null`,
  then builds the entity. The dto→entity step passes the raw `kind` key straight through (the
  entity is the replica) — **no enum parse here**. The plural
  `List<UserNotificationResponse>.toEntities(profileId)` is `mapNotNull { it.toEntityOrNull(profileId) }`.
- `:data-mappers:entity-to-domain` — `NotificationEntity.toDomain(): UserNotification?` is the
  **only** enum-parse site: `AppLogger.Mapping.log(NotificationKindEnum.of(kind)) { … } ?: return null`,
  then promotes `createdAt` via `DateTimeUtils.toLocalDateTime(createdAt)`. Drop-on-unknown makes
  the per-row mapper nullable (named `toDomain`, **not** `toDomainOrNull` — `OrNull` is DTO-source
  only), which cascades to the plural via `mapNotNull`.

---

## Step 8. Wire up in `:shared` (summary)

> DI wiring owned by the DI skill.

- `:shared/build.gradle.kts` — add `implementation(projects.dataFeatures.notifications)`.
- `:shared/Koin.kt` — add `NotificationsFeatureModule().module` to the `modules(...)` call.

---

## Step 9. Verify (MUST)

```bash
./gradlew :shared:assembleSharedDebugXCFramework
./gradlew :androidApp:assembleDebug
```

Both must build green. Then test the feature manually. (Android-only when `iosEnabled: false`.)

---

## Common mistakes (MUST)

- **Forgetting `@Single(binds = [...])`** on the impl. Consumers can't `inject<Notifications*>()`.
- **Forgetting to bump `version = N+1`** in `@Database`. Schema export shows old data.
- **Adding the entity to `@Database`'s entities list** but skipping the migration. Destructive
  fallback kicks in; user data lost.
- **Including the new module in `:shared/build.gradle.kts`** but skipping `:shared/Koin.kt`.
  Runtime "no definition found".
- **Cross-module imports between data features.** Each `:data-features:<x>` only sees
  `:data-features:feature-api`.
- **Returning DTOs from the Feature.** Only domain models cross the API.
- **Forgetting to schedule range/list reconciliation.** Stale rows linger.
