package com.grippo.design.components.muscle

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.muscles.MuscleGroupEnumState
import com.grippo.core.state.muscles.MuscleGroupState
import com.grippo.core.state.muscles.MuscleRepresentationState
import com.grippo.design.resources.provider.muscles.MuscleColorPreset
import com.grippo.design.resources.provider.muscles.bodyBack
import com.grippo.design.resources.provider.muscles.bodyFront
import com.grippo.design.resources.provider.muscles.bodySplit
import com.grippo.design.resources.provider.muscles.legsSplit

@Composable
public fun MusclesImage(
    modifier: Modifier = Modifier,
    item: MuscleGroupState<MuscleRepresentationState.Plain>,
    preset: MuscleColorPreset,
) {
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
