package com.grippo.profile.settings

import com.grippo.analytics.profile.ProfileAnalytics
import com.grippo.analytics.profile.models.ProfileEvent
import com.grippo.analytics.profile.models.ProfileOperation
import com.grippo.analytics.profile.models.ProfileScreen
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.UiText
import com.grippo.data.features.api.authorization.DeleteProfileUseCase
import com.grippo.data.features.api.authorization.LogoutUseCase
import com.grippo.data.features.api.local.settings.LocalSettingsFeature
import com.grippo.data.features.api.local.settings.models.AnalyticsConsent
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.account_deletion
import com.grippo.design.resources.provider.account_deletion_description
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.toolkit.analytics.context.NavigationReason
import kotlinx.coroutines.flow.onEach

internal class ProfileSettingsViewModel(
    private val analytics: ProfileAnalytics,
    private val localSettingsFeature: LocalSettingsFeature,
    private val deleteProfileUseCase: DeleteProfileUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val dialogController: DialogController,
) : BaseViewModel<ProfileSettingsState, ProfileSettingsDirection, ProfileSettingsLoader>(
    ProfileSettingsState()
), ProfileSettingsContract {

    override val screenPresentation = { reason: NavigationReason ->
        analytics.presentScreen(ProfileScreen.Settings, reason)
    }

    init {
        observeAnalyticsConsent()
    }

    private fun observeAnalyticsConsent() {
        update { it.copy(analyticsConsent = null) }
        localSettingsFeature.observeAnalyticsConsent()
            .onEach { consent -> update { it.copy(analyticsConsent = consent == AnalyticsConsent.Granted) } }
            .safeLaunch(processing = Processing.Infinity, onError = ::observeAnalyticsConsent)
    }

    override fun onAnalyticsConsentChange(granted: Boolean) {
        val current = state.value.analyticsConsent ?: return
        if (current == granted || ProfileSettingsLoader.AnalyticsConsent in loaders.value) return
        safeLaunch(loader = ProfileSettingsLoader.AnalyticsConsent) {
            localSettingsFeature.setAnalyticsConsent(granted).getOrThrow()
        }
    }

    override fun onBack() {
        analytics.trackEvent(ProfileEvent.Settings.DismissRequested)
        navigateTo(ProfileSettingsDirection.Back)
    }

    override fun onLogoutClick() {
        analytics.trackEvent(ProfileEvent.Settings.Logout)
        safeLaunch(loader = ProfileSettingsLoader.LogoutButton) {
            analytics.trackOperation(ProfileOperation.Logout) {
                logoutUseCase.execute()
            }
        }
    }

    override fun onDeleteAccount() {
        analytics.trackEvent(ProfileEvent.Settings.DeleteOpened)
        val config = DialogConfig.Confirmation(
            title = UiText.Res(Res.string.account_deletion),
            description = UiText.Res(Res.string.account_deletion_description),
            onResult = {
                analytics.trackEvent(ProfileEvent.Settings.DeleteConfirmed)
                safeLaunch(loader = ProfileSettingsLoader.DeleteAccountButton) {
                    analytics.trackOperation(ProfileOperation.DeleteAccount) {
                        deleteProfileUseCase.execute()
                    }
                }
            })
        dialogController.open(config)
    }
}
