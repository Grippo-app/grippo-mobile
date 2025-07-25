package com.grippo.data.features.settings.domain

import com.grippo.data.features.api.settings.SettingsFeature
import com.grippo.data.features.api.settings.models.ColorMode
import com.grippo.data.features.api.settings.models.Settings
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

@Single(binds = [SettingsFeature::class])
internal class SettingsFeatureImpl(
    private val repository: SettingsRepository
) : SettingsFeature {

    override fun observeSettings(): Flow<Settings?> {
        return repository.observeSettings()
    }

    override suspend fun setColorMode(mode: ColorMode): Result<Unit> {
        return repository.setColorMode(mode)
    }
}