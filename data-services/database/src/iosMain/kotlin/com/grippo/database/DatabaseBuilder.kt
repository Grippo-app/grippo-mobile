package com.grippo.database

import androidx.room.Room
import androidx.room.RoomDatabase
import com.grippo.platform.core.NativeContext
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

internal actual fun NativeContext.getDatabaseBuilder(): RoomDatabase.Builder<Database> {
    val dbFilePath = documentDirectory() + "/grippo_database.db"
    return Room.databaseBuilder<Database>(
        name = dbFilePath,
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}