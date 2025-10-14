package com.grippo.core.state.muscles

import androidx.compose.runtime.Immutable
import com.grippo.core.state.profile.MuscleCoverageState
import com.grippo.core.state.profile.MuscleLoadEnumState

@Immutable
public sealed class MuscleRepresentationState(public open val value: MuscleState) {
    @Immutable
    public data class Plain(
        override val value: MuscleState
    ) : MuscleRepresentationState(value)

    @Immutable
    public data class User(
        override val value: MuscleState,
        val load: MuscleLoadEnumState?,
        val coverage: MuscleCoverageState?
    ) : MuscleRepresentationState(value)
}