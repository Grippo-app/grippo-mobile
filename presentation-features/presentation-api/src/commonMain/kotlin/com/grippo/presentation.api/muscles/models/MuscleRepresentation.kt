package com.grippo.presentation.api.muscles.models

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.user.models.MuscleCoverage
import com.grippo.presentation.api.user.models.MuscleLoadEnum

@Immutable
public sealed class MuscleRepresentation(public open val value: MuscleState) {
    @Immutable
    public data class Plain(
        override val value: MuscleState
    ) : MuscleRepresentation(value)

    @Immutable
    public data class User(
        override val value: MuscleState,
        val load: MuscleLoadEnum?,
        val coverage: MuscleCoverage?
    ) : MuscleRepresentation(value)
}