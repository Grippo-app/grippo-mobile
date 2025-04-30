package com.grippo.data.features.weight.history

import com.grippo.data.features.api.weight.history.WeightHistoryFeature
import com.grippo.data.features.weight.history.data.WeightHistoryRepositoryImpl
import com.grippo.data.features.weight.history.domain.WeightHistoryFeatureImpl
import com.grippo.data.features.weight.history.domain.WeightHistoryRepository
import org.koin.core.module.Module
import org.koin.dsl.module

public val weightHistoryFeatureModule: Module = module {
    single<WeightHistoryFeature> {
        WeightHistoryFeatureImpl(get())
    }

    single<WeightHistoryRepository> {
        WeightHistoryRepositoryImpl(get(), get())
    }
}