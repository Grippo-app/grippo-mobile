package com.grippo.data.features.excluded.muscles

import com.grippo.data.features.api.excluded.muscles.ExcludedMusclesFeature
import com.grippo.data.features.excluded.muscles.data.ExcludedMusclesRepositoryImpl
import com.grippo.data.features.excluded.muscles.domain.ExcludedMusclesFeatureImpl
import com.grippo.data.features.excluded.muscles.domain.ExcludedMusclesRepository
import org.koin.core.module.Module
import org.koin.dsl.module

public val excludedMusclesFeatureModule: Module = module {
    single<ExcludedMusclesFeature> {
        ExcludedMusclesFeatureImpl(get())
    }

    single<ExcludedMusclesRepository> {
        ExcludedMusclesRepositoryImpl(get(), get())
    }
}