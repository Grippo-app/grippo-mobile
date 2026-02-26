package com.grippo.services.database

import androidx.room.Room
import com.grippo.services.database.migrations.DatabaseMigrations
import com.grippo.toolkit.context.NativeContext
import kotlinx.coroutines.Dispatchers

internal actual fun NativeContext.getDatabaseBuilder(): Database {
    val appContext = this.context.applicationContext
    val dbFile = appContext.getDatabasePath("grippo_database.db")
    return Room.databaseBuilder<Database>(
        context = appContext,
        name = dbFile.absolutePath
    )
        .addMigrations(*DatabaseMigrations.all)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
        .also { it.openHelper.writableDatabase }
}
