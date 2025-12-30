package com.grippo.core.state.muscles.metrics

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.muscles.MuscleEnumState
import kotlinx.collections.immutable.ImmutableList

@Immutable
public interface MuscleColorSource {
    public val muscles: ImmutableList<MuscleEnumState>
    public val color: Color
}

@Immutable
public data class MuscleLoadEntry(
    val label: String,
    val value: Float,
    override val color: Color,
    override val muscles: ImmutableList<MuscleEnumState>,
) : MuscleColorSource

@Immutable
public data class MuscleLoadBreakdown(
    val entries: List<MuscleLoadEntry>,
)

@Immutable
public data class MuscleImages(
    val front: ImageVector,
    val back: ImageVector,
)

@Immutable
public data class MuscleLoadSummary(
    val perGroup: MuscleLoadBreakdown,
    val images: MuscleImages?,
)
