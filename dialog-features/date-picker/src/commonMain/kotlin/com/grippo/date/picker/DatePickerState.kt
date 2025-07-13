package com.grippo.date.picker

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.datetime.LocalDateTime

@Immutable
public data class DatePickerState(
    public val suggestions: PersistentList<Int> = (100..250).toPersistentList(),
    val initial: LocalDateTime
)