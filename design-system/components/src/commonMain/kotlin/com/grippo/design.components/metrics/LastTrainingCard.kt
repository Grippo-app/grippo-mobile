package com.grippo.design.components.metrics

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.trainings.TrainingState
import com.grippo.core.state.trainings.stubTraining
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.calendar
import com.grippo.design.resources.provider.icons.Check
import com.grippo.design.resources.provider.icons.Dumbbell
import com.grippo.design.resources.provider.icons.Timer
import com.grippo.design.resources.provider.last_workout
import com.grippo.design.resources.provider.plate
import com.grippo.design.resources.provider.plus_value_more
import kotlinx.collections.immutable.toPersistentList

/**
 * Compact "last workout" teaser shown on Home. Designed to read at a glance:
 * date as the subtitle, a soft icon-led meta row (duration · volume), then up
 * to a few exercise names with a check mark. Tonnage stays available but
 * loses the noisy bullet-separator style so it doesn't compete with the
 * primary heading.
 */
@Composable
public fun LastTrainingCard(
    modifier: Modifier = Modifier,
    value: TrainingState,
    onClick: () -> Unit,
) {
    val volume = value.total.volume.short()

    // Show 4 names when there are exactly 4 exercises so the UI never has to
    // render a useless "+1 more" line; otherwise show 3 and collapse the rest.
    val visibleExercises = remember(value.exercises) {
        if (value.exercises.size == 4) value.exercises.take(4) else value.exercises.take(3)
    }
    val hiddenExercises = remember(value.exercises.size) {
        (value.exercises.size - visibleExercises.size).coerceAtLeast(0)
    }

    Box(modifier = modifier.height(intrinsicSize = IntrinsicSize.Max)) {
        Image(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = (AppTokens.dp.metrics.lastTraining.image / 2))
                .size(AppTokens.dp.metrics.lastTraining.image)
                .scale(2f),
            painter = AppTokens.drawables.res(Res.drawable.plate),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
        )

        Column(modifier = Modifier.fillMaxSize()) {

            Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

            Header(
                modifier = Modifier.fillMaxWidth(),
                date = value.createdAt.display,
            )

            Spacer(Modifier.height(AppTokens.dp.contentPadding.subContent))

            MetaRow(
                duration = value.duration.display,
                volume = volume,
            )

            Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

            ExerciseList(
                modifier = Modifier.fillMaxWidth(),
                names = visibleExercises.map { it.id to it.exerciseExample.name },
                hiddenCount = hiddenExercises,
            )

            Spacer(Modifier.weight(1f))

            Spacer(Modifier.height(AppTokens.dp.contentPadding.block))

            Button(
                content = ButtonContent.Text(text = AppTokens.strings.res(Res.string.calendar)),
                size = ButtonSize.Small,
                style = ButtonStyle.Secondary,
                onClick = onClick,
            )

            Spacer(Modifier.height(AppTokens.dp.contentPadding.content))
        }
    }
}

@Composable
private fun Header(
    modifier: Modifier = Modifier,
    date: String,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Text(
            text = AppTokens.strings.res(Res.string.last_workout),
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.text.primary,
        )

        Text(
            text = date,
            style = AppTokens.typography.b13Semi(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MetaRow(
    modifier: Modifier = Modifier,
    duration: String,
    volume: String,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetaItem(icon = AppTokens.icons.Timer, value = duration)
        MetaItem(icon = AppTokens.icons.Dumbbell, value = volume)
    }
}

@Composable
private fun MetaItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.metrics.lastTraining.metaIcon),
            imageVector = icon,
            tint = AppTokens.colors.icon.tertiary,
            contentDescription = null,
        )

        Text(
            text = value,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.tertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ExerciseList(
    modifier: Modifier = Modifier,
    names: List<Pair<String, String>>,
    hiddenCount: Int,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        names.forEach { (id, name) ->
            key(id) {
                ExerciseRow(name = name)
            }
        }

        if (hiddenCount > 0) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = AppTokens.strings.res(Res.string.plus_value_more, hiddenCount),
                style = AppTokens.typography.b13Semi(),
                color = AppTokens.colors.text.tertiary,
            )
        }
    }
}

@Composable
private fun ExerciseRow(
    modifier: Modifier = Modifier,
    name: String,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(AppTokens.dp.metrics.lastTraining.checkIcon),
            imageVector = AppTokens.icons.Check,
            tint = AppTokens.colors.semantic.success,
            contentDescription = null,
        )

        Text(
            text = name,
            style = AppTokens.typography.b13Semi(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@AppPreview
@Composable
private fun LastTrainingCardPreview() {
    PreviewContainer {
        LastTrainingCard(
            value = stubTraining(),
            onClick = {}
        )

        LastTrainingCard(
            value = stubTraining()
                .copy(exercises = stubTraining().exercises.take(4).toPersistentList()),
            onClick = {}
        )

        LastTrainingCard(
            value = stubTraining()
                .copy(exercises = stubTraining().exercises.take(3).toPersistentList()),
            onClick = {}
        )

        LastTrainingCard(
            value = stubTraining()
                .copy(exercises = stubTraining().exercises.take(1).toPersistentList()),
            onClick = {}
        )
    }
}
