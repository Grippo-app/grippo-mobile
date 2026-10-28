package com.grippo.authorization.splash

import com.grippo.analytics.authorization.AuthAnalytics
import com.grippo.analytics.authorization.models.AuthEvent
import com.grippo.analytics.authorization.models.AuthOperation
import com.grippo.analytics.authorization.models.AuthScreen
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.foundation.models.ErrorRecovery
import com.grippo.data.features.api.authorization.AuthorizationFeature
import com.grippo.data.features.api.local.settings.LocalSettingsFeature
import com.grippo.data.features.api.user.UserFeature
import com.grippo.toolkit.analytics.context.NavigationReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull

internal class SplashViewModel(
    private val analytics: AuthAnalytics,
    private val authorizationFeature: AuthorizationFeature,
    private val localSettingsFeature: LocalSettingsFeature,
    private val userFeature: UserFeature
) : BaseViewModel<SplashState, SplashDirection, SplashLoader>(SplashState), SplashContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(AuthScreen.Splash, reason)
    }

    init {
        safeLaunch(
            loader = SplashLoader.AppContent,
            recovery = ErrorRecovery.Retry,
        ) {
            val direction = resolveDirection()
            navigateTo(direction)
        }
    }

    override fun onBack() {
        analytics.trackEvent(AuthEvent.Splash.DismissRequested)
        navigateTo(SplashDirection.Back)
    }

    private suspend fun resolveDirection(): SplashDirection {
        return when {
            !localSettingsFeature.observeOnboardingShown().first() -> SplashDirection.Onboarding
            authorizationFeature.getToken().firstOrNull() == null -> SplashDirection.AuthProcess
            hasProfile() -> SplashDirection.Home
            else -> SplashDirection.ProfileCreation
        }
    }

    private suspend fun hasProfile(): Boolean {
        return analytics.trackOperation(AuthOperation.SessionCheck) {
            val cachedUser = userFeature.observeUser().firstOrNull()
            if (cachedUser != null) {
                return@trackOperation true
            }
            userFeature.getUser()
        }
    }
}
