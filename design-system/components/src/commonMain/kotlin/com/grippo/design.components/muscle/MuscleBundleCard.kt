package com.grippo.design.components.muscle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.shadowDefault
import com.grippo.design.core.AppTokens
import com.grippo.presentation.api.exercise.example.models.ExerciseExampleBundleState
import kotlinx.collections.immutable.ImmutableList

@Composable
public fun MuscleBundleCard(
    modifier: Modifier = Modifier,
    value: ImmutableList<ExerciseExampleBundleState>
) {

    val shape = RoundedCornerShape(AppTokens.dp.m.radius)

    Column(
        modifier = modifier
            .shadowDefault(
                elevation = ShadowElevation.Card,
                shape = shape,
                color = AppTokens.colors.overlay.defaultShadow
            )
            .clip(shape = shape)
            .background(AppTokens.colors.background.secondary)
            .border(1.dp, AppTokens.colors.border.defaultPrimary, shape)
            .padding(
                horizontal = AppTokens.dp.equipmentsCard.horizontalPadding,
                vertical = AppTokens.dp.equipmentsCard.verticalPadding,
            ),
    ) {

    }
}