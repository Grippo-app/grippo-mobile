package com.grippo.authorization.splash

import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.user.UserFeature
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.firstOrNull

internal class SplashViewModel(
    private val muscleFeature: MuscleFeature,
    private val equipmentFeature: EquipmentFeature,
    private val authorizationFeature: AuthorizationFeature,
    private val userFeature: UserFeature
) : BaseViewModel<SplashState, SplashDirection, SplashLoader>(SplashState), SplashContract {

    init {
        safeLaunch(
            loader = SplashLoader.AppContent,
            onError = { resolveNavigation() }
        ) {
            val muscles = async { muscleFeature.getMuscles().getOrThrow() }
            val equipment = async { equipmentFeature.getEquipments().getOrThrow() }
            awaitAll(muscles, equipment)

            resolveNavigation()
        }
    }

    private fun resolveNavigation() {
        safeLaunch {
            val token = authorizationFeature.getToken().firstOrNull()

            if (token == null) {
                navigateTo(SplashDirection.AuthProcess)
                return@safeLaunch
            }

            val hasProfile = try {
                userFeature.getUser().getOrThrow()
            } catch (error: Throwable) {
                navigateTo(SplashDirection.AuthProcess)
                return@safeLaunch
            }

            if (hasProfile) {
                navigateTo(SplashDirection.Home)
            } else {
                navigateTo(SplashDirection.CreateProfile)
            }
        }
    }

    override fun onBack() {
        navigateTo(SplashDirection.Back)
    }
}
