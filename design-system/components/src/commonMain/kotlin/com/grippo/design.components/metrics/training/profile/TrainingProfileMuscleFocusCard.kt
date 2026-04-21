package com.grippo.design.components.metrics.training.profile

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
import com.grippo.core.state.metrics.TrainingLoadProfileArtifactsState
import com.grippo.core.state.metrics.stubTrainingLoadProfileArtifacts
import com.grippo.design.components.metrics.training.profile.internal.ContributionRow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_profile_artifact_empty
import com.grippo.design.resources.provider.training_profile_artifact_muscle_focus_footer_all
import com.grippo.design.resources.provider.training_profile_artifact_muscle_focus_footer_truncated
import com.grippo.design.resources.provider.training_profile_artifact_muscle_focus_subtitle
import com.grippo.design.resources.provider.training_profile_artifact_muscle_focus_title

/**
 * Breaks down where the training stimulus actually landed across muscle
 * groups. Each row pairs a muscle name with its share of total stimulus,
 * giving the athlete a concrete read on which muscles were worked the most.
 * A footer makes the "top N of total worked" nature of the list explicit.
 */
@Composable
public fun TrainingProfileMuscleFocusCard(
    modifier: Modifier = Modifier,
    value: TrainingLoadProfileArtifactsState,
) {
    Column(
        modifier = modifier
            .background(
                AppTokens.colors.background.card,
                RoundedCornerShape(AppTokens.dp.metrics.trainingProfile.details.radius)
            )
            .padding(
                horizontal = AppTokens.dp.metrics.trainingProfile.details.horizontalPadding,
                vertical = AppTokens.dp.metrics.trainingProfile.details.verticalPadding
            ),
    ) {
        Text(
            text = AppTokens.strings.res(Res.string.training_profile_artifact_muscle_focus_title),
            style = AppTokens.typography.h6(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(Modifier.size(AppTokens.dp.contentPadding.text))

        Text(
            text = AppTokens.strings.res(Res.string.training_profile_artifact_muscle_focus_subtitle),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
        )

        Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

        if (value.topMuscles.isEmpty()) {
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
            value.topMuscles.forEach { muscle ->
                ContributionRow(
                    label = muscle.muscle.title().text(),
                    sharePercent = muscle.share,
                )
            }
        }

        Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

        val shown = value.topMuscles.size
        val total = value.totalMusclesCount.coerceAtLeast(shown)
        val footerText = if (shown < total) {
            AppTokens.strings.res(
                Res.string.training_profile_artifact_muscle_focus_footer_truncated,
                shown,
                total,
            )
        } else {
            AppTokens.strings.res(
                Res.string.training_profile_artifact_muscle_focus_footer_all,
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
private fun TrainingProfileMuscleFocusCardPreview() {
    PreviewContainer {
        TrainingProfileMuscleFocusCard(
            value = stubTrainingLoadProfileArtifacts(),
        )
    }
}

@AppPreview
@Composable
private fun TrainingProfileMuscleFocusCardEmptyPreview() {
    PreviewContainer {
        TrainingProfileMuscleFocusCard(
            value = TrainingLoadProfileArtifactsState.empty(),
        )
    }
}
