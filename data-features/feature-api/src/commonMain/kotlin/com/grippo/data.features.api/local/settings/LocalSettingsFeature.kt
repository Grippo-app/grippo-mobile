package com.grippo.data.features.api.local.settings

import com.grippo.data.features.api.local.settings.models.AnalyticsConsent
import com.grippo.data.features.api.local.settings.models.HomeWelcomeStatus
import com.grippo.data.features.api.local.settings.models.Range
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

public interface LocalSettingsFeature {

    public fun observeOnboardingShown(): Flow<Boolean>
    public suspend fun setOnboardingShown(): Result<Unit>

    public fun observeAnalyticsConsent(): Flow<AnalyticsConsent>
    public suspend fun setDefaultAnalyticsConsent(granted: Boolean): Result<Unit>
    public suspend fun setAnalyticsConsent(granted: Boolean): Result<Unit>

    public fun observeRange(): Flow<Range?>
    public suspend fun setRange(range: Range?): Result<Unit>

    public fun observeLastGoalSuggestionShownAt(): Flow<LocalDateTime?>
    public suspend fun setLastGoalSuggestionShownAt(value: LocalDateTime?): Result<Unit>

    public fun observeHomeWelcomeStatus(): Flow<HomeWelcomeStatus>
    public suspend fun setHomeWelcomeStatus(value: HomeWelcomeStatus): Result<Unit>

    /** Clears account preferences, preserving this installation's onboarding and analytics preferences. */
    public suspend fun clearUserSettings(): Result<Unit>
}
