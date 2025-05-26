package com.grippo.authorization.registration.excluded.muscles.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.design.components.cards.selectable.SelectableCardSkeleton
import com.grippo.design.components.cards.selectable.SelectableCardStyle
import com.grippo.design.components.modifiers.shimmerAnimation
import com.grippo.design.core.AppTokens

@Composable
internal fun MusclesSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(3) { index ->

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isEven = index % 2 == 0

                if (isEven) {
                    MusclesColumnSkeleton(modifier = Modifier.weight(1f))
                    MusclesImageSkeleton(modifier = Modifier.weight(1f))
                } else {
                    MusclesImageSkeleton(modifier = Modifier.weight(1f))
                    MusclesColumnSkeleton(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MusclesColumnSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(4) {
            SelectableCardSkeleton(
                modifier = Modifier.fillMaxWidth(),
                style = SelectableCardStyle.Small("")
            )
        }
    }
}

@Composable
private fun MusclesImageSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .shimmerAnimation(
                visible = true,
                radius = AppTokens.dp.shape.large
            )
            .aspectRatio(1f),
    )
}