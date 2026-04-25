package com.grippo.data.features.local.settings.domain

import com.grippo.data.features.api.local.settings.LocalSettingsFeature
import com.grippo.data.features.api.local.settings.models.Range
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime
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

    override fun observeLastGoalSuggestionShownAt(): Flow<LocalDateTime?> {
        return repository.observeLastGoalSuggestionShownAt()
    }

    override suspend fun setLastGoalSuggestionShownAt(value: LocalDateTime?): Result<Unit> {
        return repository.setLastGoalSuggestionShownAt(value)
    }

    override suspend fun clear(): Result<Unit> {
        return repository.clear()
    }
}
