# Cookbook — add a Room migration

Self-contained reference for the migration rules and the add-migration recipe.

> **Illustrative domain.** Code uses `Note` / `Tag` / `User` / `Notification` as the generic
> `<Entity>`. Substitute identifiers from your product domain.

Migrations are first-class **infrastructure**. They live in `:data-services:database/migrations`,
are collected in `DatabaseMigrations.all`, and are tested per platform (Android + iOS). Adding or
modifying a migration is a deliberate, separate task — never bundled into a feature PR. It is
reviewed against the destructive-fallback policy and requires both-platform verification.

> **Authorization gate.** Authorization to add/modify a migration is enforced by `task-intake`
> before the migration builder runs. `prelaunch: true` skips the migration (destructive fallback);
> `prelaunch: false` requires one. Stop and ask on a risky delta against a **shipped**
> (`prelaunch: false`) DB.

---

## When you need a migration (NORMATIVE)

- You bump `@Database(version = N+1)` because an `@Entity` schema changed (new column, removed
  column, new index, changed type, new table, dropped table).
- The app is **in production** — users have existing data you can't afford to lose.

If the app is pre-launch, you can skip migrations and rely on
`fallbackToDestructiveMigration(dropAllTables = true)` — the DB is wiped and rebuilt from server.

---

## File structure (REFERENCE)

```
data-services/database/src/commonMain/kotlin/com/<org>/<product>/services/database/migrations/
  DatabaseMigrations.kt
  Migration2To3.kt
  Migration3To4.kt
  Migration4To5.kt
  ... one per version step
```

---

## `DatabaseMigrations` (EXAMPLE)

```kotlin
internal object DatabaseMigrations {
    val all: Array<Migration> = arrayOf(
        Migration2To3,
        Migration3To4,
        Migration4To5,
    )
}
```

`DatabaseBuilder.addMigrations(*DatabaseMigrations.all)` registers them all. Room's runtime picks
the right chain (e.g. v3 → v5 runs `Migration3To4` then `Migration4To5`).

---

## Individual migration (EXAMPLE)

```kotlin
internal object Migration4To5 : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA defer_foreign_keys = ON")

        // ─── draft_<entity> ──────────────────────────────────────────────────
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `draft_<entity>_new` (
                `id` TEXT NOT NULL,
                `<entity>Id` TEXT,
                `profileId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`profileId`) REFERENCES `user`(`profileId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            """
            INSERT INTO `draft_<entity>_new` (`id`, `<entity>Id`, `profileId`, `title`)
            SELECT `id`, `<entity>Id`, `profileId`, `title` FROM `draft_<entity>`
            """.trimIndent()
        )
        connection.execSQL("DROP TABLE `draft_<entity>`")
        connection.execSQL("ALTER TABLE `draft_<entity>_new` RENAME TO `draft_<entity>`")

        // ─── draft_<related> ─────────────────────────────────────────────────
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `draft_<related>_new` (
                `id` TEXT NOT NULL,
                `<entity>Id` TEXT NOT NULL,
                `categoryId` TEXT NOT NULL,
                `createdAt` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`<entity>Id`) REFERENCES `draft_<entity>`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            """
            INSERT INTO `draft_<related>_new` (`id`, `<entity>Id`, `categoryId`, `createdAt`)
            SELECT `id`, `<entity>Id`, `categoryId`, `createdAt` FROM `draft_<related>`
            """.trimIndent()
        )
        connection.execSQL("DROP TABLE `draft_<related>`")
        connection.execSQL("ALTER TABLE `draft_<related>_new` RENAME TO `draft_<related>`")

        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_draft_<related>_<entity>Id` ON `draft_<related>` (`<entity>Id`)"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_draft_<related>_categoryId` ON `draft_<related>` (`categoryId`)"
        )
    }
}
```

### Migration rules (MUST)

1. **`internal object Migration<N>To<N+1> : Migration(N, N+1)`** — one object per version step.
2. **`override fun migrate(connection: SQLiteConnection)`** — Room 2.8+ uses `SQLiteConnection`,
   not `SupportSQLiteDatabase`.
3. **Multi-table mods use the recreate-table pattern**:
   - `PRAGMA defer_foreign_keys = ON` — disables FK checks during the migration.
   - `CREATE TABLE IF NOT EXISTS <table>_new (...)` with the new schema.
   - `INSERT INTO <table>_new ... SELECT ... FROM <table>` — copy data, applying any transformations.
   - `DROP TABLE <table>` + `ALTER TABLE <table>_new RENAME TO <table>` — swap.
   - Recreate indices explicitly.
4. **Indices are recreated** after the rename — Room expects them with the exact names it generates
   (e.g. `index_<table>_<col>`).
5. **Comments delimit each table** (`// ─── <table> ─────`) for multi-table recreate-table
   migrations (e.g. `Migration4To5`). Single-table or add-only migrations don't need them; a short
   prose comment is enough when context helps.
6. **Raw SQL only.** No DSL.

---

## Adding a migration — the steps

### Step 1. Update the entity / database (EXAMPLE)

In `Database.kt`, bump `@Database(version = N+1)` and update affected `@Entity` classes:

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

Build the module to regenerate the schema JSON (Room's KSP writes the new JSON):

```bash
./gradlew :data-services:database:assemble
```

Inspect the new schema file at
`data-services/database/schemas/com.<org>.<product>.services.database.Database/6.json` — verify
the diff matches your intent (added column, dropped column, changed type, renamed column, new index).

### Step 2. Write the migration (EXAMPLE)

Create `Migration5To6.kt` in
`data-services/database/src/commonMain/kotlin/com/<org>/<product>/services/database/migrations/`.
For:

- **Add column:** `ALTER TABLE <t> ADD COLUMN <c> <TYPE> [NOT NULL DEFAULT <value>]`.
- **Drop / rename column / change type:** use the recreate-table pattern.
- **Add index:** `CREATE INDEX IF NOT EXISTS <name> ON <t> (<col>)`.
- **Add table:** `CREATE TABLE IF NOT EXISTS <t> (...)`.

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

### Step 3. Register the migration (EXAMPLE)

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

### Step 4. Verify the schema JSON matches (MUST)

Diff the new `6.json` against your migration's effect:

- New tables → corresponding `CREATE TABLE` in the migration.
- Renamed/dropped columns → recreate-table pattern.
- New indices → `CREATE INDEX` in the migration.
- Changed column types → recreate-table pattern.

Room's runtime compares the live database after migration to the expected schema (from `6.json`).
Mismatches trigger the destructive fallback. Don't ship until they match.

### Step 5. Test on Android (MUST)

Build a debug APK with **version 5** of the database (revert the version bump locally), install on
a device, create some data, then install the new APK (with version 6 + the migration). Verify: the
app opens without crashing, existing data is preserved, the new feature works.

### Step 6. Test on iOS (MUST)

```bash
./gradlew :shared:assembleSharedDebugXCFramework
```

Same flow: install version 5 build via Xcode, create data, then install version 6 build. Room
Multiplatform uses the same migration code on iOS — but the iOS `BundledSQLiteDriver` is a
different SQLite implementation. Behavior should match Android; verify just in case.

(Build for both platforms:
`./gradlew :data-services:database:build :shared:assembleSharedDebugXCFramework :androidApp:assembleDebug`.)

---

## Migration patterns (EXAMPLE)

### Add column

```kotlin
connection.execSQL("ALTER TABLE note ADD COLUMN priority INTEGER NOT NULL DEFAULT 0")
```

The simplest case. Choose a sensible non-null default.

### Drop column / rename / change type — recreate-table pattern

Five steps, in order: create the new table with the new schema, copy data (apply transformations
here), drop the old table, rename, recreate indices with Room's expected names.

```kotlin
connection.execSQL("PRAGMA defer_foreign_keys = ON")

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

connection.execSQL("""
    INSERT INTO `note_new` (id, profileId, title, createdAt, body, priority)
    SELECT id, profileId, title, createdAt, body, priority FROM `note`
""".trimIndent())

connection.execSQL("DROP TABLE `note`")
connection.execSQL("ALTER TABLE `note_new` RENAME TO `note`")
connection.execSQL("CREATE INDEX IF NOT EXISTS `index_note_profileId` ON `note` (`profileId`)")
```

`PRAGMA defer_foreign_keys = ON` disables FK enforcement during the migration so the temporary
`note_new` table doesn't trigger constraint errors.

### Add index

```kotlin
connection.execSQL("CREATE INDEX IF NOT EXISTS `index_note_createdAt` ON `note` (`createdAt`)")
```

Room expects index names of the form `index_<table>_<col>`. Match exactly or Room considers it
missing.

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

---

## Schema export (MUST)

`exportSchema = true` writes per-version JSON to
`data-services/database/schemas/com.<org>.<product>.services.database.Database/<N>.json`. These
files are **committed** — they let teammates see the schema at each version. When you bump the
version and rebuild, a new `<N+1>.json` appears. Commit it alongside the migration.

---

## Destructive migration fallback (NORMATIVE)

`fallbackToDestructiveMigration(dropAllTables = true)` is configured in `DatabaseBuilder`. If a
migration is **missing** or **fails**, Room wipes the DB and rebuilds from scratch. This is the
**safety net** — but missing migrations cause user-visible data loss (drafts, cached server data).
The fallback exists for emergencies, not as the routine path. When developing, treat a missing
migration as a bug. Add it before merging the schema bump.

---

## When NOT to add a migration (SHOULD)

- **Pre-release schema changes.** If the app hasn't shipped yet, bump the version, regenerate the
  schema JSON, and rely on the destructive fallback during development.
- **Cache-only data with no draft loss risk.** Same: the destructive fallback handles it.

When in production with active users on older versions, **every** schema change requires a
migration.

---

## Migration testing (MUST)

Automated platform tests are mandatory for every schema/migration change
(machine authority: `orchestrator/tasks/test-policy.json` — `room-schema`,
lanes `android-device` + `ios-simulator`):

1. **Android device lane** (`room.test.convention`): `MigrationTestHelper`
   against the committed old-schema JSON — the old schema comes from history,
   never regenerated by the current model; assert representative data survives
   and exact indices exist. The migration proof builder runs **without** the
   destructive fallback, so a missing path can never look green on a fresh DB.
2. **iOS simulator lane**: a runnable old-schema → new-schema fixture on the
   real pinned `BundledSQLiteDriver` actual (the helper is not published for
   Native; never copy Room 3.x API into the pinned 2.8.4 stack).
3. A separate structural case documents the production
   `fallbackToDestructiveMigration(dropAllTables = true)` policy and its
   prelaunch/cache-only boundary — it never substitutes for the migration test.

Manual install smoke (previous version → upgrade → data intact) remains
additional release evidence, not the certification path.

---

## Anti-patterns (MUST)

- **Editing an existing `Migration<N>To<N+1>` after it's shipped.** Migrations are immutable. Add a
  new `<N+1>To<N+2>` to fix.
- **Skipping the schema JSON commit.** PR review can't verify the migration without the JSON.
- **Using `ALTER TABLE ADD COLUMN` for a column without a default value** — Room rejects it.
- **Add-column migrations using the recreate-table pattern.** `ALTER TABLE ADD COLUMN` is cheaper;
  recreate-table is for changes `ALTER TABLE` can't handle.
- **Forgetting to recreate indices** after the recreate-table pattern. Room sees missing indices
  and destructive-fallbacks at runtime.
- **Inconsistent / missing table comments in multi-table migrations.** Use `// ─── <table> ─────`
  delimiters.
- **Manually testing on Android only.** iOS uses the same migration code; verify both.
- **Increasing `version` without writing a migration.** Destructive fallback kicks in — silent data
  loss for users.
- **Mixing platform-specific SQL.** Room's `SQLiteConnection` exposes the **same** SQL on Android
  and iOS; SQL must work universally.
