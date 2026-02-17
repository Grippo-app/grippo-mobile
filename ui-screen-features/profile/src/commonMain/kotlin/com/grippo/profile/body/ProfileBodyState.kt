package com.grippo.profile.body

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.HeightFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.profile.UserState
import com.grippo.core.state.profile.WeightHistoryState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class ProfileBodyState(
    val user: UserState? = null,
    val weight: WeightFormatState = WeightFormatState.Empty(),
    val height: HeightFormatState = HeightFormatState.Empty(),
    val history: ImmutableList<WeightHistoryState> = persistentListOf()
)
