package com.grippo.list.picker

import androidx.compose.runtime.Immutable
import com.grippo.state.item.ItemState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class ListPickerState(
    val title: String,
    val items: ImmutableList<ItemState> = persistentListOf(),
)
