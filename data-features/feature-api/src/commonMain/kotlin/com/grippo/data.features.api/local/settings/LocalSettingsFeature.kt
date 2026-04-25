package com.grippo.data.features.api.local.settings

import com.grippo.data.features.api.local.settings.models.Range
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDateTime

public interface LocalSettingsFeature {

    public fun observeRange(): Flow<Range?>
    public suspend fun setRange(range: Range?): Result<Unit>

    public fun observeLastGoalSuggestionShownAt(): Flow<LocalDateTime?>
    public suspend fun setLastGoalSuggestionShownAt(value: LocalDateTime?): Result<Unit>

    public suspend fun clear(): Result<Unit>
}
