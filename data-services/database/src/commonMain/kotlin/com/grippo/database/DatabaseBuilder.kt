package com.grippo.database

import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.grippo.platform.core.NativeContext

internal expect fun NativeContext.getDatabaseBuilder(): RoomDatabase.Builder<Database>

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal expect object DatabaseConstructor : RoomDatabaseConstructor<Database> {
    override fun initialize(): Database
}