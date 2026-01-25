package com.grippo.authorization.profile.creation.user

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.NameFormatState
import com.grippo.core.state.formatters.WeightFormatState

@Immutable
internal data class UserState(
    val name: NameFormatState = NameFormatState.of(""),
    val weight: WeightFormatState = WeightFormatState.of(70.0F),
    val height: HeightFormatState = HeightFormatState.of(175),
)