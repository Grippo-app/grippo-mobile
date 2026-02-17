package com.grippo.profile.body

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.WeightFormatState

@Immutable
internal data class ProfileBodyState(
    val weight: WeightFormatState = WeightFormatState.of(70.0F),
)
