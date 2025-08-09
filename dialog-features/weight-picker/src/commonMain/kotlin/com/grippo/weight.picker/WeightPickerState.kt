package com.grippo.weight.picker

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList

@Immutable
public data class WeightPickerState(
    val suggestions: PersistentList<Float> = buildList(capacity = 1201) {
        for (i in 301..1500) {
            add(i / 10f)
        }
    }.toPersistentList(),
    val value: Float
)