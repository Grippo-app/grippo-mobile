package com.grippo.data.features.local.settings.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.grippo.data.features.api.local.settings.models.Range
import com.grippo.data.features.local.settings.domain.LocalSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [LocalSettingsRepository::class])
internal class LocalSettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : LocalSettingsRepository {

    companion object {
        val RangeKey = stringPreferencesKey("range")
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
}
