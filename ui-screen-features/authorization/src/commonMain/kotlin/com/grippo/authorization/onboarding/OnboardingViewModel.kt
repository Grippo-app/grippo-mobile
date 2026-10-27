package com.grippo.authorization.onboarding

import com.grippo.analytics.authorization.AuthAnalytics
import com.grippo.analytics.authorization.models.AuthEvent
import com.grippo.analytics.authorization.models.AuthScreen
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.foundation.models.ErrorRecovery
import com.grippo.data.features.api.local.settings.LocalSettingsFeature
import com.grippo.data.features.api.local.settings.models.AnalyticsConsent
import com.grippo.toolkit.analytics.context.NavigationReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach

internal class OnboardingViewModel(
    private val analytics: AuthAnalytics,
    private val localSettingsFeature: LocalSettingsFeature,
) : BaseViewModel<OnboardingState, OnboardingDirection, OnboardingLoader>(OnboardingState()),
    OnboardingContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(AuthScreen.Onboarding, reason)
    }

    init {
        localSettingsFeature.observeAnalyticsConsent()
            .onEach(::provideAnalyticsConsent)
            .safeLaunch(processing = Processing.Infinity, recovery = ErrorRecovery.Retry)

        safeLaunch(loader = OnboardingLoader.Preparation, recovery = ErrorRecovery.Retry) {
            if (!localSettingsFeature.observeOnboardingShown().first()) {
                localSettingsFeature.setDefaultAnalyticsConsent(granted = true)
                localSettingsFeature.setOnboardingShown()
            }
        }
    }

    private fun provideAnalyticsConsent(value: AnalyticsConsent) {
        val analyticsConsent = value == AnalyticsConsent.Granted

        update { it.copy(analyticsConsent = analyticsConsent) }
    }

    override fun onAnalyticsConsentChange(granted: Boolean) {
        if (loaders.value.isNotEmpty()) return
        val current = state.value.analyticsConsent ?: return
        if (current == granted) return

        safeLaunch(loader = OnboardingLoader.AnalyticsConsent, recovery = ErrorRecovery.Retry) {
            localSettingsFeature.setAnalyticsConsent(granted)
        }
    }

    override fun onGetStarted() {
        if (state.value.analyticsConsent == null || loaders.value.isNotEmpty()) return

        analytics.trackEvent(AuthEvent.Onboarding.GetStarted)

        navigateTo(OnboardingDirection.Login)
    }

    override fun onBack() {
        if (state.value.analyticsConsent == null || loaders.value.isNotEmpty()) return

        analytics.trackEvent(AuthEvent.Onboarding.DismissRequested)

        navigateTo(OnboardingDirection.Login)
    }
}
