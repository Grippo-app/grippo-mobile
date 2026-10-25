package com.grippo.services.database

import androidx.room.RoomDatabaseConstructor
import com.grippo.toolkit.context.NativeContext

internal expect fun NativeContext.getDatabaseBuilder(): Database

@Suppress("NO_ACTUAL_FOR_EXPECT", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal expect object DatabaseConstructor : RoomDatabaseConstructor<Database> {
    override fun initialize(): Database
}