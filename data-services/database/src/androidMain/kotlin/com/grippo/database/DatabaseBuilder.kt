package com.grippo.database

import androidx.room.Room
import androidx.room.RoomDatabase
import com.grippo.platform.core.NativeContext

internal actual fun NativeContext.getDatabaseBuilder(): RoomDatabase.Builder<Database> {
    val appContext = this.context.applicationContext
    val dbFile = appContext.getDatabasePath("grippo_database.db")
    return Room.databaseBuilder<Database>(
        context = appContext,
        name = dbFile.absolutePath
    )
}