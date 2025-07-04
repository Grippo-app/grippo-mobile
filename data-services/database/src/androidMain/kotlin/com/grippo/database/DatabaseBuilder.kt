package com.grippo.database

import androidx.room.Room
import com.grippo.platform.core.NativeContext
import kotlinx.coroutines.Dispatchers

internal actual fun NativeContext.getDatabaseBuilder(): Database {
    val appContext = this.context.applicationContext
    val dbFile = appContext.getDatabasePath("grippo_database.db")
    return Room.databaseBuilder<Database>(
        context = appContext,
        name = dbFile.absolutePath
    )
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
        .also { it.openHelper.writableDatabase }
}