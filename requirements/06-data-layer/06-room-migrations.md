# Room Migrations

> **Illustrative domain.** Code blocks below use `Note` / `Tag` / `User` as the generic `<Entity>` / `<RelatedEntity>` for examples. Substitute identifiers from your product domain.

Migrations are first-class **infrastructure**. They live in `:data-services:database/migrations`, are collected in `DatabaseMigrations.all`, and are tested per platform (Android + iOS). Adding or modifying a migration is a deliberate, separate task — never bundled into a feature PR.

## File structure

```
data-services/database/src/commonMain/kotlin/com/<org>/<product>/services/database/migrations/
  DatabaseMigrations.kt
  Migration2To3.kt
  Migration3To4.kt
  Migration4To5.kt
  ... one per version step
```

## `DatabaseMigrations`

```kotlin
internal object DatabaseMigrations {
    val all: Array<Migration> = arrayOf(
        Migration2To3,
        Migration3To4,
        Migration4To5,
    )
}
```

`DatabaseBuilder.addMigrations(*DatabaseMigrations.all)` registers them all. Room's runtime picks the right chain (e.g. v3 → v5 runs `Migration3To4` then `Migration4To5`).

## Individual migration

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

Rules:

1. **`internal object Migration<N>To<N+1> : Migration(N, N+1)`** — one object per version step.
2. **`override fun migrate(connection: SQLiteConnection)`** — Room 2.8+ uses `SQLiteConnection`, not `SupportSQLiteDatabase`.
3. **Multi-table mods use the recreate-table pattern**:
   - `PRAGMA defer_foreign_keys = ON` — disables FK checks during the migration.
   - `CREATE TABLE IF NOT EXISTS <table>_new (...)` with the new schema.
   - `INSERT INTO <table>_new ... SELECT ... FROM <table>` — copy data, applying any transformations.
   - `DROP TABLE <table>` + `ALTER TABLE <table>_new RENAME TO <table>` — swap.
   - Recreate indices explicitly.
4. **Indices are recreated** after the rename — Room expects them with the exact names it generates (e.g. `index_<table>_<col>`).
5. **Comments delimit each table** (`// ─── <table> ─────`) for multi-table recreate-table migrations (e.g. `Migration4To5`). Single-table or add-only migrations don't need them; a short prose comment is enough when context helps.
6. **Raw SQL only.** No DSL.

## Adding a migration

1. Bump `@Database(version = N+1)` in `Database.kt`.
2. Update affected `@Entity` classes.
3. Build the project — Room's KSP regenerates schema JSONs in `data-services/database/schemas/`.
4. Compare the new schema JSON to the previous one to identify changes (added column, dropped column, changed type, renamed column, new index).
5. Write `Migration<N>To<N+1>` in `migrations/`. For:
   - **Add column:** `ALTER TABLE <t> ADD COLUMN <c> <TYPE> [NOT NULL DEFAULT <value>]`.
   - **Drop / rename column / change type:** use the recreate-table pattern.
   - **Add index:** `CREATE INDEX IF NOT EXISTS <name> ON <t> (<col>)`.
   - **Add table:** `CREATE TABLE IF NOT EXISTS <t> (...)`.
6. Add the new object to `DatabaseMigrations.all`.
7. Build for **both** platforms: `./gradlew :data-services:database:build :shared:assembleSharedDebugXCFramework`.
8. Manually verify upgrade by running a debug build with the previous DB, applying the new APK/build, and inspecting Room's internal state.

## Schema export

`exportSchema = true` writes per-version JSON to `data-services/database/schemas/com.<org>.<product>.services.database.Database/<N>.json`. These files are **committed** — they let teammates see the schema at each version.

When you bump the version and rebuild, a new `<N+1>.json` appears. Commit it alongside the migration.

## Destructive migration fallback

`fallbackToDestructiveMigration(dropAllTables = true)` is configured in `DatabaseBuilder`. If a migration is **missing** or **fails**, Room wipes the DB and rebuilds from scratch.

This is the **safety net** — but missing migrations cause user-visible data loss (drafts, cached server data). The fallback exists for emergencies, not as the routine path.

When developing, treat a missing migration as a bug. Add it before merging the schema bump.

## When NOT to add a migration

- **Pre-release schema changes.** If the app hasn't shipped yet, bump the version, regenerate the schema JSON, and rely on the destructive fallback during development.
- **Cache-only data with no draft loss risk.** Same: the destructive fallback handles it.

When in production with active users on older versions, **every** schema change requires a migration.

## Migration testing

Room ships `MigrationTestHelper` for instrumentation tests, but this project does not write tests by default (see `13-anti-patterns/01-forbidden-patterns.md`).

Manual verification is the convention:

1. Install the previous version of the app on a device or emulator.
2. Use the app to create a few records (a parent entity, a draft, etc.).
3. Install the new version (over the previous).
4. Open the app; verify nothing crashed, data is preserved, new fields are populated.

For a fresh new project, consider adding a single `MigrationTest` class once any migration ships to production — it pays for itself the first time it catches a bug.

## Anti-patterns

- **`fallbackToDestructiveMigration()` (no flag)** — Room's default drops only the affected tables, leaving FK orphans. The project uses `dropAllTables = true` deliberately.
- **Editing an existing `Migration<N>To<N+1>`** — once shipped, migrations are immutable. Add a new `<N+1>To<N+2>` to fix.
- **Skipping the schema JSON commit.** PR review can't verify the migration without the JSON.
- **Multiple changes in one migration without table comments.** Readability suffers; future devs misread.
- **Add-column migrations using the recreate-table pattern.** `ALTER TABLE ADD COLUMN` is cheaper. Recreate-table is for changes `ALTER TABLE` can't handle.
- **Forgetting to recreate indices.** Room compares to expected indices and may force destructive fallback at runtime.
- **Mixing platform-specific SQL.** Room's `SQLiteConnection` exposes the **same** SQL on Android and iOS; SQL must work on both.
