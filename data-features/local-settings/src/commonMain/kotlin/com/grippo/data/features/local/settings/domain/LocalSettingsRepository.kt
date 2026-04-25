package com.grippo.data.features.local.settings.domain

import com.grippo.data.features.api.local.settings.models.Range
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

internal interface LocalSettingsRepository {

    fun observeRange(): Flow<Range?>

    suspend fun setRange(range: Range?): Result<Unit>

    fun observeLastGoalSuggestionShownAt(): Flow<LocalDateTime?>

    suspend fun setLastGoalSuggestionShownAt(value: LocalDateTime?): Result<Unit>
}
