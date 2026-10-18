package com.grippo.draft.training.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import com.grippo.core.state.formatters.DurationFormatState
import com.grippo.core.state.formatters.RepetitionsFormatState
import com.grippo.core.state.formatters.VolumeFormatState
import com.grippo.core.state.trainings.stubExercise
import com.grippo.core.state.trainings.stubPendingIteration
import com.grippo.design.components.training.TrainingStat
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.draft_training_summary_exercises
import com.grippo.design.resources.provider.draft_training_summary_sets
import com.grippo.design.resources.provider.draft_training_summary_time
import com.grippo.draft.training.DraftTrainingState
import kotlin.time.Duration.Companion.minutes
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun DraftTrainingSummary(
    modifier: Modifier = Modifier,
    state: DraftTrainingState,
) {
    val filledSets = state.exercises.sumOf { exercise ->
        exercise.iterations.count { !it.isPending }
    }
    val totalSets = state.exercises.sumOf { it.iterations.size }
    val duration = state.duration

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.metrics.panel.large.radius),
            )
            .padding(
                horizontal = AppTokens.dp.metrics.panel.large.horizontalPadding,
                vertical = AppTokens.dp.metrics.panel.large.verticalPadding,
            ),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        TrainingStat(
            modifier = Modifier.weight(1f),
            label = AppTokens.strings.res(Res.string.draft_training_summary_exercises),
            value = AnnotatedString(state.exercises.size.toString()),
            contentColor = AppTokens.colors.text.primary,
        )
        TrainingStat(
            modifier = Modifier.weight(1f),
            label = AppTokens.strings.res(Res.string.draft_training_summary_sets),
            value = AnnotatedString("$filledSets / $totalSets"),
            contentColor = AppTokens.colors.text.primary,
        )
        if (duration.value?.isPositive() == true) {
            TrainingStat(
                modifier = Modifier.weight(1f),
                label = AppTokens.strings.res(Res.string.draft_training_summary_time),
                value = AnnotatedString(duration.short()),
                contentColor = AppTokens.colors.text.primary,
            )
        }
    }
}

@AppPreview
@Composable
private fun DraftTrainingSummaryPreview() {
    PreviewContainer {
        val exercise = stubExercise().copy(
            id = "summary-exercise",
            iterations = persistentListOf(
                stubPendingIteration().copy(
                    id = "filled-set",
                    repetitions = RepetitionsFormatState.of(10),
                    externalWeight = VolumeFormatState.of(60f),
                ),
                stubPendingIteration().copy(
                    id = "planned-set",
                    repetitions = RepetitionsFormatState.of(10),
                ),
            ),
        )
        val state = DraftTrainingState(
            exercises = persistentListOf(exercise),
            duration = DurationFormatState.of(18.minutes),
        )

        DraftTrainingSummary(state = state)
        DraftTrainingSummary(state = state.copy(duration = DurationFormatState.Empty()))
    }
}
