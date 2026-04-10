package com.grippo.data.features.local.settings.domain

import com.grippo.data.features.api.local.settings.LocalSettingsFeature
import com.grippo.data.features.api.local.settings.models.Range
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single

@Single(binds = [LocalSettingsFeature::class])
internal class LocalSettingsFeatureImpl(
    private val repository: LocalSettingsRepository
) : LocalSettingsFeature {

    override fun observeRange(): Flow<Range?> {
        return repository.observeRange()
    }

    override suspend fun setRange(range: Range?): Result<Unit> {
        return repository.setRange(range)
    }
}
