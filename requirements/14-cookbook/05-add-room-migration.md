# Add a Room Migration

> **Concrete example.** The example task and identifiers below (`Note archive`, `tag-picker`, etc.) are illustrative; the recipe steps apply to any feature you build with this template.

Migrations are a **deliberate**, separate task. They are reviewed against the destructive fallback policy and require **both-platform** verification.

## When you need a migration

- You bump `@Database(version = N+1)` because an `@Entity` schema changed (new column, removed column, new index, changed type, new table, dropped table).
- The app is **in production** — users have existing data you can't afford to lose.

If the app is pre-launch, you can skip migrations and rely on `fallbackToDestructiveMigration(dropAllTables = true)` — the DB is wiped and rebuilt from server.

## Steps

### 1. Update the entity / database

In `Database.kt`:

```kotlin
@Database(
    entities = [
        // ... existing
        NotificationEntity::class,
    ],
    version = 6,       // bumped from 5
    exportSchema = true,
)
public abstract class Database : RoomDatabase() {
    // ... existing accessors
    public abstract fun notificationDao(): NotificationDao
}
```

Build the module to regenerate the schema JSON:

```bash
./gradlew :data-services:database:assemble
```

Inspect the new schema file at `data-services/database/schemas/com.<org>.<product>.services.database.Database/6.json` — verify the diff matches your intent.

### 2. Write the migration

Create `Migration5To6.kt` in `data-services/database/src/commonMain/kotlin/com/<org>/<product>/services/database/migrations/`:

```kotlin
internal object Migration5To6 : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA defer_foreign_keys = ON")

        // ─── notification ──────────────────────────────────────────────────
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

### 3. Register the migration

In `DatabaseMigrations.kt`:

```kotlin
internal object DatabaseMigrations {
    val all: Array<Migration> = arrayOf(
        Migration2To3,
        Migration3To4,
        Migration4To5,
        Migration5To6,
    )
}
```

### 4. Verify the schema JSON matches

Diff the new `6.json` against your migration's effect:

- New tables → corresponding `CREATE TABLE` in the migration.
- Renamed/dropped columns → recreate-table pattern.
- New indices → `CREATE INDEX` in the migration.
- Changed column types → recreate-table pattern.

Room's runtime compares the live database after migration to the expected schema (from `6.json`). Mismatches trigger the destructive fallback. Don't ship until they match.

### 5. Test on Android

Build a debug APK with **version 5** of the database (revert the version bump locally), install on a device, create some data, then install the new APK (with version 6 + the migration). Verify:

- The app opens without crashing.
- Existing data is preserved.
- The new feature works.

### 6. Test on iOS

```bash
./gradlew :shared:assembleSharedDebugXCFramework
```

Same flow: install version 5 build via Xcode, create data, then install version 6 build.

Room Multiplatform uses the same migration code on iOS — but the iOS `BundledSQLiteDriver` is a different SQLite implementation. Behavior should match Android; verify just in case.

## Migration patterns

### Add column

```sql
ALTER TABLE note ADD COLUMN priority INTEGER NOT NULL DEFAULT 0
```

The simplest case. Choose a sensible non-null default.

### Drop column / rename / change type — recreate-table pattern

```kotlin
connection.execSQL("PRAGMA defer_foreign_keys = ON")

// 1. Create the new table with the new schema
connection.execSQL("""
    CREATE TABLE IF NOT EXISTS `note_new` (
        `id` TEXT NOT NULL,
        `profileId` TEXT NOT NULL,
        `title` TEXT NOT NULL,
        `createdAt` TEXT NOT NULL,
        `body` TEXT NOT NULL,
        `priority` INTEGER NOT NULL,
        PRIMARY KEY(`id`),
        FOREIGN KEY(`profileId`) REFERENCES `user`(`profileId`) ON UPDATE NO ACTION ON DELETE CASCADE
    )
""".trimIndent())

// 2. Copy data (apply transformations here)
connection.execSQL("""
    INSERT INTO `note_new` (id, profileId, title, createdAt, body, priority)
    SELECT id, profileId, title, createdAt, body, priority FROM `note`
""".trimIndent())

// 3. Drop the old table
connection.execSQL("DROP TABLE `note`")

// 4. Rename
connection.execSQL("ALTER TABLE `note_new` RENAME TO `note`")

// 5. Recreate indices with Room's expected names
connection.execSQL("CREATE INDEX IF NOT EXISTS `index_note_profileId` ON `note` (`profileId`)")
```

`PRAGMA defer_foreign_keys = ON` disables FK enforcement during the migration so the temporary `note_new` table doesn't trigger constraint errors.

### Add index

```kotlin
connection.execSQL("CREATE INDEX IF NOT EXISTS `index_note_createdAt` ON `note` (`createdAt`)")
```

Room expects index names of the form `index_<table>_<col>`. Match exactly or Room considers it missing.

### Add table

```kotlin
connection.execSQL("""
    CREATE TABLE IF NOT EXISTS `notification` (
        `id` TEXT NOT NULL,
        ...
        PRIMARY KEY(`id`)
    )
""".trimIndent())
connection.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_profileId` ON `notification` (`profileId`)")
```

### Drop table

```kotlin
connection.execSQL("DROP TABLE IF EXISTS `obsolete_table`")
```

Verify no foreign keys reference the table first.

## Anti-patterns

- **Editing an existing migration after it's shipped.** Migrations are immutable. Add `MigrationNToN+1` to fix.
- **Skipping the `6.json` commit.** PR review loses the schema audit.
- **Using `ALTER TABLE ADD COLUMN` for a column without a default value** — Room rejects it.
- **Forgetting to recreate indices** after the recreate-table pattern. Room sees missing indices and destructive-fallbacks.
- **Inconsistent table comments in multi-table migrations.** Use `// ─── <table> ─────` delimiters.
- **Manually testing on Android only.** iOS uses the same migration code; verify both.
- **Increasing `version` without writing a migration.** Destructive fallback kicks in — silent data loss for users.
- **Mixing platform-specific SQL.** Room's `SQLiteConnection` is the same on both platforms; SQL must work universally.
