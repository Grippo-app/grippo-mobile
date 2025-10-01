package com.grippo.height.picker

import androidx.compose.runtime.Immutable
import com.grippo.state.formatters.HeightFormatState
import com.grippo.state.formatters.HeightFormatState.Companion.HeightLimitation
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

internal val DefaultHeightSuggestions: PersistentList<Int> = HeightLimitation
    .toList()
    .toPersistentList()

@Immutable
public data class HeightPickerState(
    public val suggestions: PersistentList<Int> = DefaultHeightSuggestions,
    val value: HeightFormatState
)
