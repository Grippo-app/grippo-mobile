package com.grippo.data.features.api.settings

import com.grippo.data.features.api.settings.models.Locale
import com.grippo.data.features.api.settings.models.Settings
import com.grippo.data.features.api.settings.models.Theme
import kotlinx.coroutines.flow.Flow

public interface SettingsFeature {
    public fun observeSettings(): Flow<Settings?>
    public suspend fun setTheme(mode: Theme): Result<Unit>
    public suspend fun setLocale(locale: Locale): Result<Unit>
}