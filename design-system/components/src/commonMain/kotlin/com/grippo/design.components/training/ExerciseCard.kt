package com.grippo.design.components.training

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.nonRippleClick
import com.grippo.design.components.modifiers.shadowDefault
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.icons.ChevronDown
import com.grippo.presentation.api.trainings.models.ExerciseState
import com.grippo.presentation.api.trainings.models.stubExercise

@Composable
public fun ExerciseCard(
    modifier: Modifier = Modifier,
    value: ExerciseState
) {

    val expanded = remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (expanded.value) -180f else 0f,
        label = "rotation"
    )

    val shape = RoundedCornerShape(AppTokens.dp.shape.large)

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
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .nonRippleClick(onClick = { expanded.value = expanded.value.not() })
                .padding(
                    horizontal = AppTokens.dp.paddings.mediumHorizontal,
                    vertical = AppTokens.dp.paddings.mediumVertical
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = value.name,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                style = AppTokens.typography.h3(),
                color = AppTokens.colors.text.primary
            )

            Icon(
                modifier = Modifier
                    .graphicsLayer { rotationZ = rotation }
                    .size(AppTokens.dp.icon.medium),
                imageVector = AppTokens.icons.ChevronDown,
                tint = AppTokens.colors.icon.default,
                contentDescription = null
            )
        }

        AnimatedVisibility(
            visible = expanded.value,
            enter = fadeIn(animationSpec = tween(durationMillis = 100)),
            exit = fadeOut(animationSpec = tween(durationMillis = 100))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = AppTokens.dp.paddings.mediumHorizontal,
                    ),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                value.iterations.forEachIndexed { index, iteration ->
                    IterationCard(
                        modifier = Modifier.fillMaxWidth(),
                        value = iteration,
                        index = index,
                    )
                }

                Spacer(Modifier.height(AppTokens.dp.paddings.mediumVertical))
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