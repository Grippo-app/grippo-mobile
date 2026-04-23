package com.grippo.design.components.metrics.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.profile.TrainingLoadProfileArtifactsState
import com.grippo.core.state.metrics.profile.stubTrainingLoadProfileArtifacts
import com.grippo.design.components.metrics.profile.internal.ContributionRow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_profile_artifact_empty
import com.grippo.design.resources.provider.training_profile_artifact_sets
import com.grippo.design.resources.provider.training_profile_artifact_top_exercises_footer_all
import com.grippo.design.resources.provider.training_profile_artifact_top_exercises_footer_truncated
import com.grippo.design.resources.provider.training_profile_artifact_top_exercises_subtitle
import com.grippo.design.resources.provider.training_profile_artifact_top_exercises_title

/**
 * Shows which exercises contributed the most to the current training-load
 * profile, ranked by their share of total stimulus across the period.
 * Each row pairs the exercise name and total set count with a bar that
 * makes the relative contribution easy to scan. A footer clarifies that
 * the list is a "top N of total" — so the user doesn't wonder where the
 * rest of their exercises went.
 */
@Composable
public fun TrainingProfileTopExercisesCard(
    modifier: Modifier = Modifier,
    value: TrainingLoadProfileArtifactsState,
) {
    Column(
        modifier = modifier
            .background(
                AppTokens.colors.background.card,
                RoundedCornerShape(AppTokens.dp.metrics.profile.trainingProfile.details.radius)
            )
            .padding(
                horizontal = AppTokens.dp.metrics.profile.trainingProfile.details.horizontalPadding,
                vertical = AppTokens.dp.metrics.profile.trainingProfile.details.verticalPadding
            ),
    ) {
        Text(
            text = AppTokens.strings.res(Res.string.training_profile_artifact_top_exercises_title),
            style = AppTokens.typography.h6(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(Modifier.size(AppTokens.dp.contentPadding.text))

        Text(
            text = AppTokens.strings.res(Res.string.training_profile_artifact_top_exercises_subtitle),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
        )

        Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

        if (value.topExercises.isEmpty()) {
            Text(
                text = AppTokens.strings.res(Res.string.training_profile_artifact_empty),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )
            return@Column
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            value.topExercises.forEach { exercise ->
                val setsCaption = AppTokens.strings.res(
                    Res.string.training_profile_artifact_sets,
                    exercise.totalSets
                )
                ContributionRow(
                    label = exercise.name,
                    caption = setsCaption,
                    sharePercent = exercise.stimulusShare,
                )
            }
        }

        Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

        val shown = value.topExercises.size
        val total = value.totalExercisesCount.coerceAtLeast(shown)
        val footerText = if (shown < total) {
            AppTokens.strings.res(
                Res.string.training_profile_artifact_top_exercises_footer_truncated,
                shown,
                total,
            )
        } else {
            AppTokens.strings.res(
                Res.string.training_profile_artifact_top_exercises_footer_all,
                total,
            )
        }
        Text(
            text = footerText,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@AppPreview
@Composable
private fun TrainingProfileTopExercisesCardPreview() {
    PreviewContainer {
        TrainingProfileTopExercisesCard(
            value = stubTrainingLoadProfileArtifacts(),
        )
    }
}

@AppPreview
@Composable
private fun TrainingProfileTopExercisesCardEmptyPreview() {
    PreviewContainer {
        TrainingProfileTopExercisesCard(
            value = TrainingLoadProfileArtifactsState.empty(),
        )
    }
}
