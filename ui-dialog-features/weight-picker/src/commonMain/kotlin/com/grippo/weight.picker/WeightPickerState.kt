package com.grippo.weight.picker

import androidx.compose.runtime.Immutable
import com.grippo.state.formatters.WeightFormatState
import com.grippo.state.formatters.WeightFormatState.Companion.WeightLimitation
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

private const val WeightStep: Int = 1

internal val DefaultWeightSuggestions: PersistentList<Float> =
    (WeightLimitation.start.toInt() * 10..WeightLimitation.endInclusive.toInt() * 10 step WeightStep)
        .map { it / 10f }
        .toPersistentList()

@Immutable
public data class WeightPickerState(
    val suggestions: PersistentList<Float> = DefaultWeightSuggestions,
    val value: WeightFormatState
)
