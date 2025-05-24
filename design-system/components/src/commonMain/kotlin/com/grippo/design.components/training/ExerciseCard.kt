package com.grippo.design.components.training

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.shadowDefault
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.presentation.api.trainings.models.ExerciseState
import com.grippo.presentation.api.trainings.models.stubExercise

@Composable
public fun ExerciseCard(
    modifier: Modifier = Modifier,
    value: ExerciseState
) {

    val shape = RoundedCornerShape(AppTokens.dp.shape.large)

    Row(
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
                horizontal = AppTokens.dp.paddings.mediumHorizontal,
                vertical = AppTokens.dp.paddings.mediumVertical
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = value.name,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = AppTokens.typography.h3(),
                color = AppTokens.colors.text.primary
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            value.iterations.forEachIndexed { index, iteration ->
                IterationCard(
                    modifier = Modifier,
                    value = iteration,
                    index = index,
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun ExerciseCardPreview() {
    PreviewContainer {
        ExerciseCard(
            value = stubExercise()
        )
    }
}