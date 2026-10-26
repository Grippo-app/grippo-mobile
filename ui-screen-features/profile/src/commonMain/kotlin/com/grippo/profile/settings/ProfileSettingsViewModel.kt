package com.grippo.profile.settings

import com.grippo.analytics.profile.ProfileAnalytics
import com.grippo.analytics.profile.models.ProfileEvent
import com.grippo.analytics.profile.models.ProfileOperation
import com.grippo.analytics.profile.models.ProfileScreen
import com.grippo.core.foundation.BaseViewModel
import com.grippo.core.state.formatters.UiText
import com.grippo.core.state.settings.LanguageEnumState
import com.grippo.data.features.api.authorization.DeleteProfileUseCase
import com.grippo.data.features.api.authorization.LogoutUseCase
import com.grippo.data.features.api.local.settings.LocalSettingsFeature
import com.grippo.data.features.api.local.settings.models.AnalyticsConsent
import com.grippo.data.features.api.user.UserFeature
import com.grippo.data.features.api.user.models.User
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.account_deletion
import com.grippo.design.resources.provider.account_deletion_description
import com.grippo.design.resources.provider.app_language
import com.grippo.dialog.api.DialogConfig
import com.grippo.dialog.api.DialogController
import com.grippo.domain.state.user.toState
import com.grippo.state.domain.language.toDomain
import com.grippo.toolkit.analytics.context.NavigationReason
import com.grippo.toolkit.localization.AppLocale
import kotlinx.coroutines.flow.onEach

internal class ProfileSettingsViewModel(
    private val analytics: ProfileAnalytics,
    userFeature: UserFeature,
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
        userFeature.observeUser()
            .onEach(::provideUser)
            .safeLaunch()

        observeAnalyticsConsent()
    }

    private fun provideUser(value: User?) {
        val user = value?.toState()

        update { it.copy(user = user) }
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
            localSettingsFeature.setAnalyticsConsent(granted)
        }
    }

    override fun onBack() {
        analytics.trackEvent(ProfileEvent.Settings.DismissRequested)
        navigateTo(ProfileSettingsDirection.Back)
    }

    override fun onLanguageClick() {
        analytics.trackEvent(ProfileEvent.Settings.LanguageOpened)

        val config = DialogConfig.LanguagePicker(
            initial = currentLanguage(),
            title = UiText.Res(Res.string.app_language),
            onResult = { value ->
                analytics.trackEvent(ProfileEvent.Settings.LanguageConfirmed)

                if (value != currentLanguage()) {
                    val language = value.toDomain()

                    safeLaunch {
                        analytics.trackOperation(ProfileOperation.SaveLanguage) {
                            localSettingsFeature.setLanguage(language)
                        }
                    }
                }
            },
        )

        dialogController.open(config)
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

    private fun currentLanguage(): LanguageEnumState {
        val localeTag = AppLocale.current()

        return LanguageEnumState.of(localeTag)
    }
}
