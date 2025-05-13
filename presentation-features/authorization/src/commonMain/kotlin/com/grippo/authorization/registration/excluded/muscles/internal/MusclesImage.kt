package com.grippo.authorization.registration.excluded.muscles.internal

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.design.components.modifiers.shimmerAnimation
import com.grippo.design.core.AppTokens
import com.grippo.presentation.api.muscles.models.MuscleGroupState
import com.grippo.presentation.api.muscles.models.MuscleRepresentation
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun MusclesImage(
    modifier: Modifier = Modifier,
    item: MuscleGroupState<MuscleRepresentation.Plain>,
    selectedIds: ImmutableList<String>
) {
    Image(
        modifier = modifier.aspectRatio(1f),
        imageVector = item.image(selectedIds),
        contentDescription = null
    )
}

@Composable
internal fun MusclesImageSkeleton(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .shimmerAnimation(
                visible = true,
                radius = AppTokens.dp.shape.medium
            )
            .aspectRatio(1f),
    )
}