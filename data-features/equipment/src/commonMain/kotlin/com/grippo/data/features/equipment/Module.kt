package com.grippo.data.features.equipment

import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.equipment.data.EquipmentRepositoryImpl
import com.grippo.data.features.equipment.domain.EquipmentFeatureImpl
import com.grippo.data.features.equipment.domain.EquipmentRepository
import org.koin.core.module.Module
import org.koin.dsl.module

public val equipmentFeatureModule: Module = module {
    single<EquipmentFeature> {
        EquipmentFeatureImpl(get())
    }

    single<EquipmentRepository> {
        EquipmentRepositoryImpl(get(), get())
    }
}