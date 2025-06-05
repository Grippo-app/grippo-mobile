package com.grippo.data.features.excluded.equipments

import com.grippo.data.features.api.excluded.equipments.ExcludedEquipmentsFeature
import com.grippo.data.features.excluded.equipments.data.ExcludedEquipmentsRepositoryImpl
import com.grippo.data.features.excluded.equipments.domain.ExcludedEquipmentsFeatureImpl
import com.grippo.data.features.excluded.equipments.domain.ExcludedEquipmentsRepository
import org.koin.core.module.Module
import org.koin.dsl.module

public val excludedEquipmentFeatureModule: Module = module {
    single<ExcludedEquipmentsFeature> {
        ExcludedEquipmentsFeatureImpl(get())
    }

    single<ExcludedEquipmentsRepository> {
        ExcludedEquipmentsRepositoryImpl(get(), get())
    }
}