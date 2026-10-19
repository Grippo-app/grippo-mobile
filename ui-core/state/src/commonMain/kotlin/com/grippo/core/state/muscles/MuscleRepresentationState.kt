package com.grippo.core.state.muscles

import androidx.compose.runtime.Immutable

@Immutable
public sealed class MuscleRepresentationState(public open val value: MuscleState) {
    @Immutable
    public data class Plain(
        override val value: MuscleState
    ) : MuscleRepresentationState(value)
}
