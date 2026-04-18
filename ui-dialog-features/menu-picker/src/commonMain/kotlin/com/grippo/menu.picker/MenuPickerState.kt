package com.grippo.menu.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.state.menu.PickerMenuItem
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class MenuPickerState(
    val items: ImmutableList<PickerMenuItem> = persistentListOf(),
)
