package com.grippo.presentation.api.muscles.models

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.resources.icons.muscles.MuscleColorProperties
import com.grippo.design.resources.icons.muscles.bodyBack
import com.grippo.design.resources.icons.muscles.bodyFront
import com.grippo.design.resources.icons.muscles.bodySplit
import com.grippo.design.resources.icons.muscles.legsSplit
import kotlinx.collections.immutable.ImmutableList

@Immutable
public data class MuscleGroupState<T : MuscleRepresentation>(
    val id: String,
    val name: String,
    val muscles: ImmutableList<T>,
    val type: MuscleGroupEnumState,
) {
    @Composable
    public fun image(): ImageVector {
        val colors = MuscleColorProperties(
            muscle = Color.Green,
            background = Color.LightGray,
            outline = Color.Transparent,
            defaultBack = Color.Gray,
            defaultFront = Color.Gray,
            backgroundBack = Color.Gray,
            backgroundFront = Color.Gray
        )

        return when (type) {
            MuscleGroupEnumState.CHEST_MUSCLES -> {
                bodyFront(colors)
            }

            MuscleGroupEnumState.BACK_MUSCLES -> {
                bodyBack(colors)
            }

            MuscleGroupEnumState.ABDOMINAL_MUSCLES -> {
                bodyFront(colors)
            }

            MuscleGroupEnumState.LEGS -> {
                legsSplit(colors)
            }

            MuscleGroupEnumState.ARMS_AND_FOREARMS -> {
                bodySplit(colors)
            }

            MuscleGroupEnumState.SHOULDER_MUSCLES -> {
                bodySplit(colors)
            }
        }
    }
}