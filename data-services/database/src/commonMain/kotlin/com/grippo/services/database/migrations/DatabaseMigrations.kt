package com.grippo.services.database.migrations

import androidx.room.migration.Migration

internal object DatabaseMigrations {
    val all: Array<Migration> = arrayOf(
        Migration2To3,
    )
}
