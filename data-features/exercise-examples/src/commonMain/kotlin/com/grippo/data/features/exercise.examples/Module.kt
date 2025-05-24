package com.grippo.data.features.exercise.examples

import com.grippo.data.features.api.exercise.example.ExerciseExampleFeature
import com.grippo.data.features.exercise.examples.data.ExerciseExampleRepositoryImpl
import com.grippo.data.features.exercise.examples.domain.ExerciseExampleFeatureImpl
import com.grippo.data.features.exercise.examples.domain.ExerciseExampleRepository
import org.koin.core.module.Module
import org.koin.dsl.module

public val exerciseExamplesFeatureModule: Module = module {
    single<ExerciseExampleFeature> {
        ExerciseExampleFeatureImpl(get())
    }

    single<ExerciseExampleRepository> {
        ExerciseExampleRepositoryImpl(get(), get())
    }
}