package com.grippo.data.features.settings.domain

import com.grippo.data.features.api.settings.models.Theme
import com.grippo.data.features.api.settings.models.Settings
import kotlinx.coroutines.flow.Flow

internal interface SettingsRepository {
    fun observeSettings(): Flow<Settings?>
    suspend fun setTheme(theme: Theme): Result<Unit>
}