package com.grippo.database

import com.grippo.platform.core.NativeContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.core.module.Module
import org.koin.dsl.module

public val databaseModule: Module = module {
    single<Database> {
        get<NativeContext>().getDatabaseBuilder()
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
    }

    single {
        get<Database>().tokenDao()
    }
    single {
        get<Database>().userDao()
    }
    single {
        get<Database>().weightHistoryDao()
    }
    single {
        get<Database>().equipmentDao()
    }
    single {
        get<Database>().equipmentGroupDao()
    }
    single {
        get<Database>().exerciseExampleDao()
    }
    single {
        get<Database>().exerciseExampleBundleDao()
    }
    single {
        get<Database>().exerciseEquipmentDao()
    }
    single {
        get<Database>().exerciseTutorialDao()
    }
    single {
        get<Database>().muscleDao()
    }
    single {
        get<Database>().trainingDao()
    }
}