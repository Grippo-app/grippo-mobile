package com.grippo.height.picker

import androidx.compose.runtime.Immutable
import com.grippo.state.formatters.HeightFormatState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

@Immutable
public data class HeightPickerState(
    public val suggestions: PersistentList<Int> = (100..250).toPersistentList(),
    val value: HeightFormatState
)