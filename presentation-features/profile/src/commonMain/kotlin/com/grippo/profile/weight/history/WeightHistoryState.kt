package com.grippo.profile.weight.history

import androidx.compose.runtime.Immutable
import com.grippo.state.profile.WeightFormatState

@Immutable
internal data class WeightHistoryState(
    val weight: WeightFormatState = WeightFormatState.of(70.0F),
)