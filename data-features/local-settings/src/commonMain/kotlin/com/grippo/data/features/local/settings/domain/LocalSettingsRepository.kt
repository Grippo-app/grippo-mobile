package com.grippo.data.features.local.settings.domain

import com.grippo.data.features.api.local.settings.models.AnalyticsConsent
import com.grippo.data.features.api.local.settings.models.HomeWelcomeStatus
import com.grippo.data.features.api.local.settings.models.Language
import com.grippo.data.features.api.local.settings.models.Range
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

internal interface LocalSettingsRepository {

    fun observeOnboardingShown(): Flow<Boolean>
    suspend fun setOnboardingShown(): Unit

    fun observeAnalyticsConsent(): Flow<AnalyticsConsent>
    suspend fun setDefaultAnalyticsConsent(granted: Boolean): Unit
    suspend fun setAnalyticsConsent(granted: Boolean): Unit

    fun observeRange(): Flow<Range>
    suspend fun setRange(range: Range?): Unit

    fun observeLastGoalSuggestionShownAt(): Flow<LocalDateTime?>
    suspend fun setLastGoalSuggestionShownAt(value: LocalDateTime?): Unit

    fun observeHomeWelcomeStatus(): Flow<HomeWelcomeStatus>
    suspend fun setHomeWelcomeStatus(value: HomeWelcomeStatus): Unit

    fun observeLanguage(): Flow<Language?>
    suspend fun setLanguage(value: Language): Unit

    suspend fun clearUserSettings(): Unit
}
