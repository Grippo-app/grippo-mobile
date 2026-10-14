package com.grippo.authorization.splash

import com.grippo.analytics.authorization.AuthAnalytics
import com.grippo.analytics.authorization.models.AuthEvent
import com.grippo.analytics.authorization.models.AuthOperation
import com.grippo.analytics.authorization.models.AuthScreen
import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.data.features.api.equipment.EquipmentFeature
import com.grippo.data.features.api.local.settings.LocalSettingsFeature
import com.grippo.data.features.api.muscle.MuscleFeature
import com.grippo.data.features.api.user.UserFeature
import com.grippo.toolkit.analytics.context.NavigationReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

internal class SplashViewModel(
    private val analytics: AuthAnalytics,
    private val muscleFeature: MuscleFeature,
    private val equipmentFeature: EquipmentFeature,
    private val authorizationFeature: AuthorizationFeature,
    private val localSettingsFeature: LocalSettingsFeature,
    private val userFeature: UserFeature
) : BaseViewModel<SplashState, SplashDirection, SplashLoader>(SplashState), SplashContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(AuthScreen.Splash, reason)
    }

    init {
        loadContent()
    }

    override fun onBack() {
        analytics.trackEvent(AuthEvent.Splash.DismissRequested)
        navigateTo(SplashDirection.Back)
    }

    private fun loadContent() {
        safeLaunch(
            loader = SplashLoader.AppContent,
            onError = ::loadContent,
        ) {
            if (!localSettingsFeature.observeOnboardingShown().first()) {
                navigateTo(SplashDirection.Onboarding)
                return@safeLaunch
            }
            if (authorizationFeature.getToken().firstOrNull() == null) {
                muscleFeature.getMuscles().getOrThrow()
                equipmentFeature.getEquipments().getOrThrow()
                navigateTo(SplashDirection.AuthProcess)
            } else {
                loadProfile()
            }
        }
    }

    private suspend fun loadProfile() {
        val hasProfile = analytics.trackOperation(AuthOperation.SessionCheck) {
            hasProfile()
        }

        val destination = if (hasProfile) {
            SplashDirection.Home
        } else {
            SplashDirection.CreateProfile
        }
        navigateTo(destination)
    }

    private suspend fun hasProfile(): Boolean {
        val cachedUser = userFeature.observeUser().firstOrNull()
        if (cachedUser != null) {
            return true
        }
        return userFeature.getUser().getOrThrow()
    }
}
