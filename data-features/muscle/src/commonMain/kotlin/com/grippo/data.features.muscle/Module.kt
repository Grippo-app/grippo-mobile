package com.grippo.data.features.muscle

import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.muscle.data.MuscleRepositoryImpl
import com.grippo.data.features.muscle.domain.MuscleFeatureImpl
import com.grippo.data.features.muscle.domain.MuscleRepository
import org.koin.core.module.Module
import org.koin.dsl.module

public val musclesFeatureModule: Module = module {
    single<MuscleFeature> {
        MuscleFeatureImpl(get())
    }

    single<MuscleRepository> {
        MuscleRepositoryImpl(get(), get())
    }
}