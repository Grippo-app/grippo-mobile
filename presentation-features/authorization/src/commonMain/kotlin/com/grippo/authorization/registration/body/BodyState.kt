package com.grippo.authorization.registration.body

import androidx.compose.runtime.Immutable
import com.grippo.state.formatters.HeightFormatState
import com.grippo.state.formatters.WeightFormatState

@Immutable
internal data class BodyState(
    val weight: WeightFormatState = WeightFormatState.of(70.0F),
    val height: HeightFormatState = HeightFormatState.of(175),
)