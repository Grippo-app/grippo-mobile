package com.grippo.data.features.trainings

import com.grippo.data.features.api.training.TrainingFeature
import com.grippo.data.features.trainings.data.TrainingRepositoryImpl
import com.grippo.data.features.trainings.domain.TrainingFeatureImpl
import com.grippo.data.features.trainings.domain.TrainingRepository
import org.koin.core.module.Module
import org.koin.dsl.module

public val trainingsFeatureModule: Module = module {
    single<TrainingFeature> {
        TrainingFeatureImpl(get())
    }

    single<TrainingRepository> {
        TrainingRepositoryImpl(get(), get())
    }
}