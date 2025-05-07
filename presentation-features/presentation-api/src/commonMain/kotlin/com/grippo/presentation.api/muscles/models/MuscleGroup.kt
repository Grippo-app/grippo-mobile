package com.grippo.presentation.api.muscles.models

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.collections.immutable.ImmutableList

@Immutable
public data class MuscleGroup<T : MuscleRepresentation>(
    val id: String,
    val name: String = "",
    val muscles: ImmutableList<T>,
    val isSelected: Boolean,
    val type: MuscleGroupEnum,
    val bodyImageVector: ImageVector
)