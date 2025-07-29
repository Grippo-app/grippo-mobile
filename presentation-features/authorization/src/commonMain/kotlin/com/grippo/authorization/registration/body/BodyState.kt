package com.grippo.authorization.registration.body

import androidx.compose.runtime.Immutable
import com.grippo.state.profile.HeightFormatState
import com.grippo.state.profile.WeightFormatState

@Immutable
internal data class BodyState(
    val weight: WeightFormatState = WeightFormatState.of(70.0F),
    val height: HeightFormatState = HeightFormatState.of(175),
)