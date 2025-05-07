package com.grippo.presentation.api.muscles.models

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.user.models.MuscleCoverage
import com.grippo.presentation.api.user.models.MuscleLoadEnum

@Immutable
public sealed class MuscleRepresentation(public open val muscle: Muscle) {
    @Immutable
    public data class Plain(override val muscle: Muscle) : MuscleRepresentation(muscle)
    public data class User(
        override val muscle: Muscle,
        val load: MuscleLoadEnum?,
        val coverage: MuscleCoverage?
    ) : MuscleRepresentation(muscle)
}