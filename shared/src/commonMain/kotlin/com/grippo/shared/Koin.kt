package com.grippo.shared

import com.grippo.core.coreModule
import com.grippo.data.features.api.featureApiModule
import com.grippo.data.features.authorization.authorizationFeatureModule
import com.grippo.data.features.equipment.equipmentFeatureModule
import com.grippo.data.features.excluded.equipments.excludedEquipmentFeatureModule
import com.grippo.data.features.excluded.muscles.excludedMusclesFeatureModule
import com.grippo.data.features.exercise.examples.exerciseExamplesFeatureModule
import com.grippo.data.features.muscle.musclesFeatureModule
import com.grippo.data.features.trainings.trainingsFeatureModule
import com.grippo.data.features.user.userFeatureModule
import com.grippo.data.features.weight.history.weightHistoryFeatureModule
import com.grippo.database.databaseModule
import com.grippo.dialog.api.dialogModule
import com.grippo.error.provider.impl.errorModule
import com.grippo.network.networkModule
import com.grippo.platform.core.PlatformModule
import org.koin.core.KoinApplication
import org.koin.dsl.KoinAppDeclaration
import org.koin.ksp.generated.module
import org.koin.mp.KoinPlatformTools

public object Koin {
    public fun init(
        appDeclaration: KoinAppDeclaration = {},
    ): KoinApplication = KoinPlatformTools.defaultContext().startKoin {
        appDeclaration()
        modules(
            PlatformModule().module,
            errorModule,
            networkModule,
            databaseModule,
            authorizationFeatureModule,
            userFeatureModule,
            weightHistoryFeatureModule,
            coreModule,
            dialogModule,
            musclesFeatureModule,
            equipmentFeatureModule,
            featureApiModule,
            excludedMusclesFeatureModule,
            trainingsFeatureModule,
            excludedEquipmentFeatureModule,
            exerciseExamplesFeatureModule
        )
    }
}