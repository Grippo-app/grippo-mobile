package com.grippo.authorization.splash

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.api.muscle.MuscleFeature
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

internal class SplashViewModel(
    private val muscleFeature: MuscleFeature,
    private val equipmentFeature: EquipmentFeature,
    private val authorizationFeature: AuthorizationFeature
) : BaseViewModel<SplashState, SplashDirection, SplashLoader>(SplashState), SplashContract {

    init {
        safeLaunch(
            loader = SplashLoader.AppContent,
            onError = { navigateTo(SplashDirection.AuthProcess) }
        ) {
            val muscleJob = launch { muscleFeature.getMuscles().getOrThrow() }
            val equipmentJob = launch { equipmentFeature.getEquipments().getOrThrow() }
            joinAll(muscleJob, equipmentJob)

            val token = authorizationFeature.getToken().firstOrNull()

            if (token == null) {
                navigateTo(SplashDirection.AuthProcess)
            } else {
                navigateTo(SplashDirection.Home)
            }
        }
    }

    override fun onBack() {
        navigateTo(SplashDirection.Back)
    }
}