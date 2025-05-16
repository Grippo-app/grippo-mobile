package com.grippo.authorization.registration.excluded.muscles.internal

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.presentation.api.muscles.models.MuscleGroupState
import com.grippo.presentation.api.muscles.models.MuscleRepresentationState
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun MusclesImage(
    modifier: Modifier = Modifier,
    item: MuscleGroupState<MuscleRepresentationState.Plain>,
    selectedIds: ImmutableList<String>
) {
    Image(
        modifier = modifier.aspectRatio(1f),
        imageVector = item.image(selectedIds),
        contentDescription = null
    )
}