package com.grippo.authorization.splash

import com.grippo.core.BaseViewModel
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.api.muscle.MuscleFeature
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.firstOrNull

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
            val muscles = async { muscleFeature.getMuscles().getOrThrow() }
            val equipment = async { equipmentFeature.getEquipments().getOrThrow() }
            awaitAll(muscles, equipment)

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