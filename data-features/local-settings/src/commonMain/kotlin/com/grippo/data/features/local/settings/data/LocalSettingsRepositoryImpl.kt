package com.grippo.data.features.local.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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
        val RangeKey = stringPreferencesKey("range")
        val LastGoalSuggestionShownAtKey = stringPreferencesKey("last_goal_suggestion_shown_at")
    }

    override fun observeRange(): Flow<Range?> {
        return dataStore.data
            .map { preferences -> Range.of(preferences[RangeKey]) }
    }

    override suspend fun setRange(range: Range?): Result<Unit> {
        return runCatching {
            dataStore.edit { preferences ->
                val key = range?.key
                if (key == null) {
                    preferences.remove(RangeKey)
                } else {
                    preferences[RangeKey] = key
                }
            }
        }
    }

    override fun observeLastGoalSuggestionShownAt(): Flow<LocalDateTime?> {
        return dataStore.data.map { preferences ->
            preferences[LastGoalSuggestionShownAtKey]?.let(DateTimeUtils::toLocalDateTime)
        }
    }

    override suspend fun setLastGoalSuggestionShownAt(value: LocalDateTime?): Result<Unit> {
        return runCatching {
            dataStore.edit { preferences ->
                if (value == null) {
                    preferences.remove(LastGoalSuggestionShownAtKey)
                } else {
                    preferences[LastGoalSuggestionShownAtKey] = DateTimeUtils.toUtcIso(value)
                }
            }
        }
    }

    override suspend fun clear(): Result<Unit> {
        return runCatching {
            dataStore.edit { preferences ->
                preferences.clear()
            }
        }
    }
}
