package com.grippo.services.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

internal object Migration3To4 : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `goal` (
                `userId` TEXT NOT NULL,
                `primaryGoal` TEXT NOT NULL,
                `secondaryGoal` TEXT,
                `target` TEXT NOT NULL,
                `personalizations` TEXT NOT NULL,
                `confidence` REAL NOT NULL,
                `createdAt` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `lastConfirmedAt` TEXT,
                PRIMARY KEY(`userId`),
                FOREIGN KEY(`userId`) REFERENCES `user`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // weight_history: add userId column + FK to user with CASCADE on delete.
        // Existing rows are cached from the backend and will be re-fetched,
        // so we drop and recreate the table instead of trying to back-fill userId.
        connection.execSQL("DROP TABLE IF EXISTS `weight_history`")
        connection.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `weight_history` (
                `id` TEXT NOT NULL,
                `userId` TEXT NOT NULL,
                `weight` REAL NOT NULL,
                `createdAt` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`userId`) REFERENCES `user`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_weight_history_userId` ON `weight_history` (`userId`)"
        )
    }
}
