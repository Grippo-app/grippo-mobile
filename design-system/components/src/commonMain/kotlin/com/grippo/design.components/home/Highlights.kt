package com.grippo.design.components.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.grippo.core.state.trainings.TrainingState
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.modifiers.spot
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.barbell
import com.grippo.design.resources.provider.highlights
import com.grippo.design.resources.provider.icons.ArrowRight
import com.grippo.design.resources.provider.plus_value_more
import com.grippo.design.resources.provider.view_workout
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils

@Composable
public fun HighlightsCard(
    modifier: Modifier = Modifier,
    value: TrainingState,
    onViewWorkout: () -> Unit
) {

    val startLabel = remember(value.createdAt, value.duration) {
        val v = DateTimeUtils.minus(value.createdAt, value.duration)
        DateTimeUtils.format(v, DateFormat.DateOnly.DateMmmDdYyyy)
    }

    val volume = value.metrics.volume.short()

    val string = remember {
        buildString {
            append(startLabel)
            append(" · ")
            append(DateTimeUtils.format(value.duration))
            append(" · ")
            append(volume)
        }
    }

    Box(
        modifier = modifier
            .height(intrinsicSize = IntrinsicSize.Max)
            .clip(RoundedCornerShape(AppTokens.dp.home.highlights.radius))
            .background(
                AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.home.highlights.radius)
            )
    ) {

        Image(
            modifier = Modifier
                .spot(color = AppTokens.colors.brand.color5)
                .align(Alignment.CenterEnd)
                .offset(x = 100.dp)
                .size(200.dp)
                .scale(2f),
            painter = AppTokens.drawables.res(Res.drawable.barbell),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    vertical = AppTokens.dp.home.highlights.verticalPadding,
                    horizontal = AppTokens.dp.home.highlights.horizontalPadding
                )
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = AppTokens.strings.res(Res.string.highlights),
                style = AppTokens.typography.h4(),
                color = AppTokens.colors.text.primary
            )

            Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = string,
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary
            )

            Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

            val firstExercises = if (value.exercises.size == 4) {
                value.exercises.take(4)
            } else {
                value.exercises.take(3)
            }

            firstExercises.forEach {
                Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "✓ ${it.name}",
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary
                )
            }

            if (value.exercises.size > 4) {
                val lastExercisesCount = value.exercises.size - 3

                Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = AppTokens.strings.res(Res.string.plus_value_more, lastExercisesCount),
                    style = AppTokens.typography.b13Med(),
                    color = AppTokens.colors.text.secondary
                )
            }

            Spacer(Modifier.weight(1f))

            Spacer(Modifier.height(AppTokens.dp.contentPadding.block))

            Button(
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.view_workout),
                    endIcon = ButtonIcon.Icon(AppTokens.icons.ArrowRight)
                ),
                size = ButtonSize.Small,
                onClick = onViewWorkout
            )
        }
    }
}

@AppPreview
@Composable
private fun HighlightsCardPreview() {
    PreviewContainer {
        HighlightsCard(
            value = stubTraining(),
            onViewWorkout = {}
        )
    }
}
