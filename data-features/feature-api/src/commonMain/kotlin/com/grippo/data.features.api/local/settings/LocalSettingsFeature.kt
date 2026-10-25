package com.grippo.data.features.api.local.settings

import com.grippo.data.features.api.local.settings.models.AnalyticsConsent
import com.grippo.data.features.api.local.settings.models.HomeWelcomeStatus
import com.grippo.data.features.api.local.settings.models.Range
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

public interface LocalSettingsFeature {

    public fun observeOnboardingShown(): Flow<Boolean>
    public suspend fun setOnboardingShown(): Unit

    public fun observeAnalyticsConsent(): Flow<AnalyticsConsent>
    public suspend fun setDefaultAnalyticsConsent(granted: Boolean): Unit
    public suspend fun setAnalyticsConsent(granted: Boolean): Unit

    public fun observeRange(): Flow<Range?>
    public suspend fun setRange(range: Range?): Unit

    public fun observeLastGoalSuggestionShownAt(): Flow<LocalDateTime?>
    public suspend fun setLastGoalSuggestionShownAt(value: LocalDateTime?): Unit

    public fun observeHomeWelcomeStatus(): Flow<HomeWelcomeStatus>
    public suspend fun setHomeWelcomeStatus(value: HomeWelcomeStatus): Unit

    public suspend fun clearUserSettings(): Unit
}
