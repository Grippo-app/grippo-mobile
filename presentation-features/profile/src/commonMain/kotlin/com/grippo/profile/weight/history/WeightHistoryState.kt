package com.grippo.profile.weight.history

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.profile.models.WeightFormatState

@Immutable
internal data class WeightHistoryState(
    val weight: WeightFormatState = WeightFormatState.of(70.0F),
)