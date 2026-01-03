package com.grippo.design.components.muscle

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.grippo.core.state.muscles.MuscleGroupEnumState
import com.grippo.core.state.muscles.MuscleGroupState
import com.grippo.core.state.muscles.MuscleRepresentationState
import com.grippo.core.state.muscles.stubMuscleGroup
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.muscles.MuscleColorPreset
import com.grippo.design.resources.provider.muscles.bodyBack
import com.grippo.design.resources.provider.muscles.bodyFront
import com.grippo.design.resources.provider.muscles.bodySplit
import com.grippo.design.resources.provider.muscles.legsSplit
import kotlin.random.Random

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

@AppPreview
@Composable
private fun MusclesImagePreview() {
    PreviewContainer {
        val muscleGroup = stubMuscleGroup().first()
        val nextColor = {
            Color(
                red = Random.nextFloat(),
                green = Random.nextFloat(),
                blue = Random.nextFloat(),
                alpha = 1f
            )
        }
        MusclesImage(
            item = muscleGroup,
            preset = MuscleColorPreset(
                biceps = nextColor(),
                triceps = nextColor(),
                forearm = nextColor(),
                forearmFront = nextColor(),
                forearmBack = nextColor(),
                lateralDeltoid = nextColor(),
                anteriorDeltoid = nextColor(),
                posteriorDeltoid = nextColor(),
                pectoralisMajorAbdominal = nextColor(),
                pectoralisMajorClavicular = nextColor(),
                pectoralisMajorSternocostal = nextColor(),
                rectusAbdominis = nextColor(),
                obliquesAbdominis = nextColor(),
                rhomboids = nextColor(),
                latissimus = nextColor(),
                trapezius = nextColor(),
                teresMajor = nextColor(),
                gluteal = nextColor(),
                hamstrings = nextColor(),
                calf = nextColor(),
                quadriceps = nextColor(),
                adductors = nextColor(),
                abductors = nextColor(),
                other = nextColor(),
                outline = nextColor(),
                backgroundFront = nextColor(),
                backgroundBack = nextColor()
            )
        )
    }
}
