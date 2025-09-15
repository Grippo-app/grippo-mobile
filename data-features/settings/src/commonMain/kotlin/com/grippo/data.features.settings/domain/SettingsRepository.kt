package com.grippo.data.features.settings.domain

import com.grippo.data.features.api.settings.models.Locale
import com.grippo.data.features.api.settings.models.Settings
import com.grippo.data.features.api.settings.models.Theme
import kotlinx.coroutines.flow.Flow

internal interface SettingsRepository {
    fun observeSettings(): Flow<Settings?>
    suspend fun setTheme(theme: Theme): Result<Unit>
    suspend fun setLocale(locale: Locale): Result<Unit>
}