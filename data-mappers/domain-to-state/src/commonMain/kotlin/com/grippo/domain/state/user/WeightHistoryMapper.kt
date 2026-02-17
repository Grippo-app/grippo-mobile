package com.grippo.domain.state.user

import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.core.state.profile.WeightHistoryState
import com.grippo.data.features.api.weight.history.models.WeightHistory
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

public fun List<WeightHistory>.toState(): ImmutableList<WeightHistoryState> {
    return map { it.toState() }.toPersistentList()
}

public fun WeightHistory.toState(): WeightHistoryState {
    return WeightHistoryState(
        id = id,
        value = WeightFormatState.of(weight),
        createdAt = createdAt
    )
}