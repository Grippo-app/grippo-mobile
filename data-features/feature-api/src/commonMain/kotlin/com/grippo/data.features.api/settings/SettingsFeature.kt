package com.grippo.data.features.api.settings

import com.grippo.data.features.api.settings.models.Theme
import com.grippo.data.features.api.settings.models.Settings
import kotlinx.coroutines.flow.Flow

public interface SettingsFeature {
    public fun observeSettings(): Flow<Settings?>
    public suspend fun setTheme(mode: Theme): Result<Unit>
}