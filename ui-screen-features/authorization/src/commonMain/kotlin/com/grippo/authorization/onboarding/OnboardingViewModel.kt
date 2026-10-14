package com.grippo.authorization.onboarding

import com.grippo.analytics.authorization.AuthAnalytics
import com.grippo.analytics.authorization.models.AuthEvent
import com.grippo.analytics.authorization.models.AuthScreen
import com.grippo.core.foundation.BaseViewModel
import com.grippo.data.features.api.local.settings.LocalSettingsFeature
import com.grippo.toolkit.analytics.context.NavigationReason
import kotlinx.coroutines.flow.first

internal class OnboardingViewModel(
    private val analytics: AuthAnalytics,
    private val localSettingsFeature: LocalSettingsFeature,
) : BaseViewModel<OnboardingState, OnboardingDirection, OnboardingLoader>(OnboardingState()),
    OnboardingContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(AuthScreen.Onboarding, reason)
    }

    init {
        prepare()
    }

    private fun prepare() {
        safeLaunch(loader = OnboardingLoader.Preparation, onError = ::prepare) {
            if (localSettingsFeature.observeOnboardingShown().first()) {
                navigateTo(OnboardingDirection.Splash)
                return@safeLaunch
            }
            localSettingsFeature.setOnboardingShown().getOrThrow()
            update { it.copy(isReady = true) }
        }
    }

    override fun onGetStarted() {
        if (!state.value.isReady || loaders.value.isNotEmpty()) return
        analytics.trackEvent(AuthEvent.Onboarding.GetStarted)
        navigateTo(OnboardingDirection.Splash)
    }

    override fun onBack() {
        if (!state.value.isReady || loaders.value.isNotEmpty()) return
        analytics.trackEvent(AuthEvent.Onboarding.DismissRequested)
        navigateTo(OnboardingDirection.Splash)
    }
}
