package com.grippo.core.state.muscles

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
public data class MuscleState(
    val id: String,
    val type: MuscleEnumState,
)