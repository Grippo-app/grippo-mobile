package com.grippo.data.features.local.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.grippo.data.features.api.local.settings.models.AnalyticsConsent
import com.grippo.data.features.api.local.settings.models.HomeWelcomeStatus
import com.grippo.data.features.api.local.settings.models.Language
import com.grippo.data.features.api.local.settings.models.Range
import com.grippo.data.features.local.settings.domain.LocalSettingsRepository
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import org.koin.core.annotation.Single

@Single(binds = [LocalSettingsRepository::class])
internal class LocalSettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : LocalSettingsRepository {

    companion object {
        val OnboardingShownKey = booleanPreferencesKey("onboarding_shown")
        val AnalyticsConsentKey = booleanPreferencesKey("analytics_consent")
        val RangeKey = stringPreferencesKey("range")
        val LastGoalSuggestionShownAtKey = stringPreferencesKey("last_goal_suggestion_shown_at")
        val HomeWelcomeStatusKey = stringPreferencesKey("home_welcome_status")
        val LanguageKey = stringPreferencesKey("language")

        val DefaultRange = Range.LAST_30_DAYS
    }

    override fun observeOnboardingShown(): Flow<Boolean> {
        return dataStore.data.map { preferences -> preferences[OnboardingShownKey] ?: false }
    }

    override suspend fun setOnboardingShown(): Unit {
        dataStore.edit { preferences -> preferences[OnboardingShownKey] = true }
    }

    override fun observeAnalyticsConsent(): Flow<AnalyticsConsent> {
        return dataStore.data.map { preferences ->
            when (preferences[AnalyticsConsentKey]) {
                null -> AnalyticsConsent.Denied
                false -> AnalyticsConsent.Denied
                true -> AnalyticsConsent.Granted
            }
        }
    }

    override suspend fun setDefaultAnalyticsConsent(granted: Boolean): Unit {
        dataStore.edit { preferences ->
            if (preferences[AnalyticsConsentKey] == null) {
                preferences[AnalyticsConsentKey] = granted
            }
        }
    }

    override suspend fun setAnalyticsConsent(granted: Boolean): Unit {
        dataStore.edit { preferences -> preferences[AnalyticsConsentKey] = granted }
    }

    override fun observeRange(): Flow<Range> {
        return dataStore.data
            .map { preferences -> Range.of(preferences[RangeKey]) ?: DefaultRange }
    }

    override suspend fun setRange(range: Range?): Unit {
        dataStore.edit { preferences ->
            val key = range?.key
            if (key == null) {
                preferences.remove(RangeKey)
            } else {
                preferences[RangeKey] = key
            }
        }
    }

    override fun observeLastGoalSuggestionShownAt(): Flow<LocalDateTime?> {
        return dataStore.data.map { preferences ->
            preferences[LastGoalSuggestionShownAtKey]?.let(DateTimeUtils::toLocalDateTime)
        }
    }

    override suspend fun setLastGoalSuggestionShownAt(value: LocalDateTime?): Unit {
        dataStore.edit { preferences ->
            if (value == null) {
                preferences.remove(LastGoalSuggestionShownAtKey)
            } else {
                preferences[LastGoalSuggestionShownAtKey] = DateTimeUtils.toUtcIso(value)
            }
        }
    }

    override fun observeHomeWelcomeStatus(): Flow<HomeWelcomeStatus> {
        return dataStore.data.map { preferences ->
            HomeWelcomeStatus.of(preferences[HomeWelcomeStatusKey])
        }
    }

    override suspend fun setHomeWelcomeStatus(value: HomeWelcomeStatus): Unit {
        dataStore.edit { preferences ->
            preferences[HomeWelcomeStatusKey] = value.key
        }
    }

    override fun observeLanguage(): Flow<Language?> {
        return dataStore.data.map { preferences ->
            Language.of(preferences[LanguageKey])
        }
    }

    override suspend fun setLanguage(value: Language): Unit {
        dataStore.edit { preferences ->
            preferences[LanguageKey] = value.key
        }
    }

    override suspend fun clearUserSettings() {
        dataStore.edit { preferences ->
            val consent = preferences[AnalyticsConsentKey]
            val onboardingShown = preferences[OnboardingShownKey]
            val language = preferences[LanguageKey]
            preferences.clear()
            if (consent != null) {
                preferences[AnalyticsConsentKey] = consent
            }
            if (onboardingShown != null) {
                preferences[OnboardingShownKey] = onboardingShown
            }
            if (language != null) {
                preferences[LanguageKey] = language
            }
        }
    }
}
