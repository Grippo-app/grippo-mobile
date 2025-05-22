package com.grippo.data.features.exercise.examples

import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.exercise.examples.data.EquipmentRepositoryImpl
import com.grippo.data.features.exercise.examples.domain.EquipmentFeatureImpl
import com.grippo.data.features.exercise.examples.domain.EquipmentRepository
import org.koin.core.module.Module
import org.koin.dsl.module

public val exerciseExamplesFeatureModule: Module = module {
    single<EquipmentFeature> {
        EquipmentFeatureImpl(get())
    }

    single<EquipmentRepository> {
        EquipmentRepositoryImpl(get(), get())
    }
}