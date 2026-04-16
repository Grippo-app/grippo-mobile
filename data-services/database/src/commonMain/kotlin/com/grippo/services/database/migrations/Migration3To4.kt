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
    }
}
