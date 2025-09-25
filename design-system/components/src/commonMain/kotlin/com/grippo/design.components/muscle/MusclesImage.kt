package com.grippo.design.components.muscle

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.calculation.muscle.MuscleEngine
import com.grippo.calculation.muscle.factory.MuscleColorStrategy
import com.grippo.design.resources.provider.muscles.bodyBack
import com.grippo.design.resources.provider.muscles.bodyFront
import com.grippo.design.resources.provider.muscles.bodySplit
import com.grippo.design.resources.provider.muscles.legsSplit
import com.grippo.state.muscles.MuscleGroupEnumState
import com.grippo.state.muscles.MuscleGroupState
import com.grippo.state.muscles.MuscleRepresentationState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableSet

@Composable
public fun MusclesImage(
    modifier: Modifier = Modifier,
    item: MuscleGroupState<MuscleRepresentationState.Plain>,
    selectedIds: ImmutableList<String>
) {
    val preset = MuscleEngine.generatePreset(
        MuscleColorStrategy.BySelection(
            group = item,
            selectedIds = selectedIds.toImmutableSet(),
        )
    )

    val imageVector = when (item.type) {
        MuscleGroupEnumState.CHEST_MUSCLES -> bodyFront(preset)
        MuscleGroupEnumState.BACK_MUSCLES -> bodyBack(preset)
        MuscleGroupEnumState.ABDOMINAL_MUSCLES -> bodyFront(preset)
        MuscleGroupEnumState.LEGS -> legsSplit(preset)
        MuscleGroupEnumState.ARMS_AND_FOREARMS -> bodySplit(preset)
        MuscleGroupEnumState.SHOULDER_MUSCLES -> bodySplit(preset)
    }

    Image(
        modifier = modifier.aspectRatio(1f),
        imageVector = imageVector,
        contentDescription = null,
    )
}
