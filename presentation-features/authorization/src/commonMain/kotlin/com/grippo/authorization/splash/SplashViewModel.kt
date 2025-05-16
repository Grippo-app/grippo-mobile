package com.grippo.authorization.splash

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.api.muscle.MuscleFeature
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

internal class SplashViewModel(
    private val muscleFeature: MuscleFeature,
    private val equipmentFeature: EquipmentFeature,
) : BaseViewModel<SplashState, SplashDirection, SplashLoader>(SplashState), SplashContract {

    init {
        safeLaunch {
            val muscleJob = launch { muscleFeature.getMuscles() }
            val equipmentJob = launch { equipmentFeature.getPublicEquipments() }

            joinAll(muscleJob, equipmentJob)
            navigateTo(SplashDirection.AuthProcess)
        }
    }
}