package com.grippo.authorization.registration.body

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.profile.models.HeightFormatState
import com.grippo.presentation.api.profile.models.WeightFormatState

@Immutable
internal data class BodyState(
    val weight: WeightFormatState = WeightFormatState.of(70.0F),
    val height: HeightFormatState = HeightFormatState.of(175),
)