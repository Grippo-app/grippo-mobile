package com.grippo.data.features.settings.domain

import com.grippo.data.features.api.settings.SettingsFeature
import com.grippo.data.features.api.settings.models.Locale
import com.grippo.data.features.api.settings.models.Settings
import com.grippo.data.features.api.settings.models.Theme
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

@Single(binds = [SettingsFeature::class])
internal class SettingsFeatureImpl(
    private val repository: SettingsRepository
) : SettingsFeature {

    override fun observeSettings(): Flow<Settings?> {
        return repository.observeSettings()
    }

    override suspend fun setTheme(mode: Theme): Result<Unit> {
        return repository.setTheme(mode)
    }

    override suspend fun setLocale(locale: Locale): Result<Unit> {
        return repository.setLocale(locale)
    }
}