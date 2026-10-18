package com.grippo.design.components.muscle

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.muscles.MuscleGroupState
import com.grippo.core.state.muscles.MuscleRepresentationState
import com.grippo.core.state.muscles.stubMuscleGroup
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.muscles.MuscleColorPreset

@Composable
public fun MusclesImage(
    modifier: Modifier = Modifier,
    item: MuscleGroupState<MuscleRepresentationState.Plain>,
    preset: MuscleColorPreset,
) {
    Image(
        modifier = modifier.aspectRatio(1f),
        imageVector = item.type.image(preset),
        contentDescription = null,
    )
}

@AppPreview
@Composable
private fun MusclesImagePreview() {
    PreviewContainer {
        val muscleGroup = stubMuscleGroup().first()
        MusclesImage(
            item = muscleGroup,
            preset = muscleGroup.colorPreset(muscleGroup.muscles.map { it.value.id }.toSet()),
        )
    }
}

@AppPreview
@Composable
private fun MusclesImageUnselectedPreview() {
    PreviewContainer {
        val muscleGroup = stubMuscleGroup().first()
        MusclesImage(item = muscleGroup, preset = muscleGroup.colorPreset(emptySet()))
    }
}
