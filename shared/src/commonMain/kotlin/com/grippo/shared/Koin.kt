package com.grippo.shared

import com.grippo.core.CoreModule
import com.grippo.data.features.api.featureApiModule
import com.grippo.data.features.authorization.AuthorizationFeatureModule
import com.grippo.data.features.equipment.EquipmentFeatureModule
import com.grippo.data.features.excluded.equipments.ExcludedEquipmentFeatureModule
import com.grippo.data.features.excluded.muscles.ExcludedMusclesFeatureModule
import com.grippo.data.features.exercise.examples.ExerciseExamplesFeatureModule
import com.grippo.data.features.muscle.MusclesFeatureModule
import com.grippo.data.features.trainings.TrainingsFeatureModule
import com.grippo.data.features.user.UserFeatureModule
import com.grippo.data.features.weight.history.WeightHistoryFeatureModule
import com.grippo.database.DatabaseModule
import com.grippo.dialog.api.DialogModule
import com.grippo.error.provider.impl.ErrorModule
import com.grippo.network.NetworkModule
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
            DatabaseModule().module,
            NetworkModule().module,
            CoreModule().module,
            DialogModule().module,
            AuthorizationFeatureModule().module,
            ErrorModule().module,
            UserFeatureModule().module,
            WeightHistoryFeatureModule().module,
            MusclesFeatureModule().module,
            EquipmentFeatureModule().module,
            ExerciseExamplesFeatureModule().module,
            ExcludedMusclesFeatureModule().module,
            TrainingsFeatureModule().module,
            ExcludedEquipmentFeatureModule().module,
            featureApiModule,
        )
    }
}