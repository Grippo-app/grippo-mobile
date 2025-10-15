package com.grippo.shared

import com.grippo.ai.agent.AiAgentModule
import com.grippo.backend.BackendModule
import com.grippo.core.error.provider.impl.ErrorModule
import com.grippo.core.foundation.CoreModule
import com.grippo.data.features.api.FeatureApiModule
import com.grippo.data.features.authorization.AuthorizationFeatureModule
import com.grippo.data.features.equipment.EquipmentFeatureModule
import com.grippo.data.features.excluded.equipments.ExcludedEquipmentFeatureModule
import com.grippo.data.features.excluded.muscles.ExcludedMusclesFeatureModule
import com.grippo.data.features.exercise.examples.ExerciseExamplesFeatureModule
import com.grippo.data.features.muscle.MusclesFeatureModule
import com.grippo.data.features.suggestions.AiSuggestionsFeatureModule
import com.grippo.data.features.trainings.TrainingsFeatureModule
import com.grippo.data.features.user.UserFeatureModule
import com.grippo.data.features.weight.history.WeightHistoryFeatureModule
import com.grippo.database.DatabaseModule
import com.grippo.design.resources.provider.impl.ResourcesProviderModule
import com.grippo.dialog.api.DialogModule
import com.grippo.toolkit.connectivity.ConnectivityModule
import com.grippo.toolkit.context.ContextModule
import com.grippo.toolkit.http.client.HttpModule
import com.grippo.toolkit.image.loader.ImageLoaderModule
import com.grippo.toolkit.serialization.SerializationModule
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
            ContextModule().module,
            DatabaseModule().module,
            BackendModule().module,
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
            FeatureApiModule().module,
            ConnectivityModule().module,
            ResourcesProviderModule().module,
            AiSuggestionsFeatureModule().module,
            SerializationModule().module,
            HttpModule().module,
            AiAgentModule().module,
            ImageLoaderModule().module,
        )
    }
}