package com.grippo.data.features.api.local.settings

import com.grippo.data.features.api.local.settings.models.Range
import kotlinx.coroutines.flow.Flow

public interface LocalSettingsFeature {

    public fun observeRange(): Flow<Range?>

    public suspend fun setRange(range: Range?): Result<Unit>
}
