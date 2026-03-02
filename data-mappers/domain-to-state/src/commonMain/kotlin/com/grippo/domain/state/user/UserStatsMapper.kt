package com.grippo.domain.state.user

import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.profile.UserStatsState
import com.grippo.data.features.api.user.models.UserStats

public fun UserStats.toState(): UserStatsState {
    return UserStatsState(
        trainingsCount = trainingsCount,
        totalDuration = totalDuration,
        totalVolume = VolumeFormatState.of(totalVolume),
        totalRepetitions = RepetitionsFormatState.of(totalRepetitions)
    )
}
