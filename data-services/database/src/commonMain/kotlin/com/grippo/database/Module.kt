package com.grippo.database

import com.grippo.platform.core.NativeContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module
import org.koin.dsl.module

public val databaseModule: Module = module {
    single<AppDatabase> {
        get<NativeContext>().getDatabaseBuilder()
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }
}