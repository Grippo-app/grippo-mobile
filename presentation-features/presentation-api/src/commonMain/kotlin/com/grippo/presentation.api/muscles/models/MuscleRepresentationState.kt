package com.grippo.presentation.api.muscles.models

import androidx.compose.runtime.Immutable
import com.grippo.presentation.api.muscles.models.MuscleRepresentationState.Plain
import com.grippo.presentation.api.user.models.MuscleCoverageState
import com.grippo.presentation.api.user.models.MuscleLoadEnumState
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

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