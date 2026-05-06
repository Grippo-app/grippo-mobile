package com.grippo.services.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Drops vestigial fields from the `draft_training` and `draft_exercise`
 * tables — aggregate columns (`volume`, `repetitions`, `intensity`) and the
 * `draft_exercise.name` snapshot. Drafts no longer carry aggregates in the
 * domain model (totals are recomputed on restore from the iterations alone),
 * and the exercise name is read directly from the joined `exercise_example`
 * row instead of being duplicated.
 *
 * SQLite < 3.35 doesn't support `ALTER TABLE ... DROP COLUMN`, so we use the
 * recreate-table pattern: create a new table with the desired shape, copy
 * data over, drop the old, rename the new. `defer_foreign_keys` is enabled
 * for the duration of the transaction so the cascading FK on
 * `draft_exercise(trainingId) -> draft_training(id)` doesn't cascade-delete
 * the children while the parent is being recreated.
 */
internal object Migration4To5 : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA defer_foreign_keys = ON")

        // ─── draft_training ──────────────────────────────────────────────────
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `draft_training_new` (
                `id` TEXT NOT NULL,
                `trainingId` TEXT,
                `profileId` TEXT NOT NULL,
                `duration` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`profileId`) REFERENCES `user`(`profileId`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            """
            INSERT INTO `draft_training_new` (`id`, `trainingId`, `profileId`, `duration`)
            SELECT `id`, `trainingId`, `profileId`, `duration` FROM `draft_training`
            """.trimIndent()
        )
        connection.execSQL("DROP TABLE `draft_training`")
        connection.execSQL("ALTER TABLE `draft_training_new` RENAME TO `draft_training`")

        // ─── draft_exercise ──────────────────────────────────────────────────
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `draft_exercise_new` (
                `id` TEXT NOT NULL,
                `trainingId` TEXT NOT NULL,
                `exerciseExampleId` TEXT NOT NULL,
                `createdAt` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`trainingId`) REFERENCES `draft_training`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            """
            INSERT INTO `draft_exercise_new` (`id`, `trainingId`, `exerciseExampleId`, `createdAt`)
            SELECT `id`, `trainingId`, `exerciseExampleId`, `createdAt` FROM `draft_exercise`
            """.trimIndent()
        )
        connection.execSQL("DROP TABLE `draft_exercise`")
        connection.execSQL("ALTER TABLE `draft_exercise_new` RENAME TO `draft_exercise`")

        // Indices were dropped along with the old table — recreate them with
        // the exact names Room expects so the schema validator passes.
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_draft_exercise_trainingId` ON `draft_exercise` (`trainingId`)"
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_draft_exercise_exerciseExampleId` ON `draft_exercise` (`exerciseExampleId`)"
        )
    }
}
