package com.grippo.state.muscles

import androidx.compose.runtime.Immutable

@Immutable
public data class MuscleState(
    val id: String,
    val name: String,
    val type: MuscleEnumState,
)