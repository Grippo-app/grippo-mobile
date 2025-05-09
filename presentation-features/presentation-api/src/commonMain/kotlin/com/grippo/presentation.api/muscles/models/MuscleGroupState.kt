package com.grippo.presentation.api.muscles.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.resources.icons.muscles.bodyBack
import com.grippo.design.resources.icons.muscles.bodyFront
import com.grippo.design.resources.icons.muscles.bodySplit
import com.grippo.design.resources.icons.muscles.legsSplit
import com.grippo.presentation.api.muscles.factories.MuscleColorPresetFactory
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableSet

@Immutable
public data class MuscleGroupState<T : MuscleRepresentation>(
    val id: String,
    val name: String,
    val muscles: ImmutableList<T>,
    val type: MuscleGroupEnumState,
) {
    @Composable
    public fun image(
        selectedIds: ImmutableList<String>
    ): ImageVector {
        val preset = MuscleColorPresetFactory.fromSelected(
            group = this,
            selected = selectedIds.toImmutableSet()
        )

        return when (type) {
            MuscleGroupEnumState.CHEST_MUSCLES -> bodyFront(preset)
            MuscleGroupEnumState.BACK_MUSCLES -> bodyBack(preset)
            MuscleGroupEnumState.ABDOMINAL_MUSCLES -> bodyFront(preset)
            MuscleGroupEnumState.LEGS -> legsSplit(preset)
            MuscleGroupEnumState.ARMS_AND_FOREARMS -> bodySplit(preset)
            MuscleGroupEnumState.SHOULDER_MUSCLES -> bodySplit(preset)
        }
    }
}