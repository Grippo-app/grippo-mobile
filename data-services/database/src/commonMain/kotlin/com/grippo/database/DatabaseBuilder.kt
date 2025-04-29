package com.grippo.database

import androidx.room.RoomDatabase
import com.grippo.platform.core.NativeContext

internal expect fun NativeContext.getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>