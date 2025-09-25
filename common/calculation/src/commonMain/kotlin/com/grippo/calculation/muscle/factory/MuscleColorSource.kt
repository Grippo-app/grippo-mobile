package com.grippo.calculation.muscle.factory

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.grippo.state.muscles.MuscleEnumState

@Immutable
public interface MuscleColorSource {
    public val muscles: List<MuscleEnumState>
    public val color: Color
}
