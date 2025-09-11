package com.grippo.text.picker

import androidx.compose.runtime.Immutable
import com.grippo.state.text.TextWithId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
public data class TextPickerState(
    val list: ImmutableList<TextWithId> = persistentListOf(),
    val value: TextWithId
)