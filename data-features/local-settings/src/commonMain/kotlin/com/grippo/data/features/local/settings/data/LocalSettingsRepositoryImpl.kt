package com.grippo.data.features.local.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.grippo.data.features.api.local.settings.models.AnalyticsConsent
import com.grippo.data.features.api.local.settings.models.HomeWelcomeStatus
import com.grippo.data.features.api.local.settings.models.Range
import com.grippo.data.features.local.settings.domain.LocalSettingsRepository
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlin.coroutines.cancellation.CancellationException
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
    }

    override fun observeOnboardingShown(): Flow<Boolean> {
        return dataStore.data.map { preferences -> preferences[OnboardingShownKey] ?: false }
    }

    override suspend fun setOnboardingShown(): Result<Unit> {
        return try {
            dataStore.edit { preferences -> preferences[OnboardingShownKey] = true }
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
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

    override suspend fun setAnalyticsConsent(granted: Boolean): Result<Unit> {
        return try {
            dataStore.edit { preferences -> preferences[AnalyticsConsentKey] = granted }
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override fun observeRange(): Flow<Range?> {
        return dataStore.data
            .map { preferences -> Range.of(preferences[RangeKey]) }
    }

    override suspend fun setRange(range: Range?): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                val key = range?.key
                if (key == null) {
                    preferences.remove(RangeKey)
                } else {
                    preferences[RangeKey] = key
                }
            }
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override fun observeLastGoalSuggestionShownAt(): Flow<LocalDateTime?> {
        return dataStore.data.map { preferences ->
            preferences[LastGoalSuggestionShownAtKey]?.let(DateTimeUtils::toLocalDateTime)
        }
    }

    override suspend fun setLastGoalSuggestionShownAt(value: LocalDateTime?): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                if (value == null) {
                    preferences.remove(LastGoalSuggestionShownAtKey)
                } else {
                    preferences[LastGoalSuggestionShownAtKey] = DateTimeUtils.toUtcIso(value)
                }
            }
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override fun observeHomeWelcomeStatus(): Flow<HomeWelcomeStatus> {
        return dataStore.data.map { preferences ->
            HomeWelcomeStatus.of(preferences[HomeWelcomeStatusKey])
        }
    }

    override suspend fun setHomeWelcomeStatus(value: HomeWelcomeStatus): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                preferences[HomeWelcomeStatusKey] = value.key
            }
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    override suspend fun clearUserSettings(): Result<Unit> {
        return try {
            dataStore.edit { preferences ->
                val consent = preferences[AnalyticsConsentKey]
                val onboardingShown = preferences[OnboardingShownKey]
                preferences.clear()
                if (consent != null) preferences[AnalyticsConsentKey] = consent
                if (onboardingShown != null) preferences[OnboardingShownKey] = onboardingShown
            }
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
