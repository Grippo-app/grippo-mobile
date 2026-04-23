package com.grippo.core.state.profile

import androidx.compose.runtime.Immutable
import com.grippo.core.state.formatters.DateTimeFormatState
import com.grippo.core.state.formatters.WeightFormatState
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateRangePresets
import com.grippo.toolkit.date.utils.DateTimeUtils
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlin.random.Random
import kotlin.uuid.Uuid

@Immutable
public data class WeightHistoryState(
    val id: String,
    val value: WeightFormatState,
    val createdAt: DateTimeFormatState
)

public fun stubWeightHistoryList(): PersistentList<WeightHistoryState> = persistentListOf(
    stubWeightHistory(),
    stubWeightHistory(),
    stubWeightHistory(),
)

public fun stubWeightHistory(): WeightHistoryState = WeightHistoryState(
    id = Uuid.random().toString(),
    value = WeightFormatState.of(Random.nextInt(60, 100).toFloat()),
    createdAt = DateTimeFormatState.of(
        value = DateTimeUtils.now(),
        range = DateRangePresets.infinity(),
        format = DateFormat.DateOnly.DateMmmDdYyyy
    )
)