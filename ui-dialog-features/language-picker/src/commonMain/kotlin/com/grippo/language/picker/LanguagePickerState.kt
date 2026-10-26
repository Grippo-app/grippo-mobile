package com.grippo.language.picker

import androidx.compose.runtime.Immutable
import com.grippo.core.state.settings.LanguageEnumState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList

@Immutable
public data class LanguagePickerState(
    val value: LanguageEnumState,
    val languages: ImmutableList<LanguageEnumState> = LanguageEnumState.entries.toPersistentList(),
)
