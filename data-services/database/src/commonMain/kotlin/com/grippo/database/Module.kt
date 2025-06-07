package com.grippo.database

import com.grippo.platform.core.NativeContext
import org.koin.core.module.Module
import org.koin.dsl.module

public val databaseModule: Module = module {
    single<Database> {
        get<NativeContext>().getDatabaseBuilder()
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
        get<Database>().userActiveDao()
    }
    single {
        get<Database>().equipmentDao()
    }
    single {
        get<Database>().exerciseExampleDao()
    }
    single {
        get<Database>().muscleDao()
    }
    single {
        get<Database>().trainingDao()
    }
}