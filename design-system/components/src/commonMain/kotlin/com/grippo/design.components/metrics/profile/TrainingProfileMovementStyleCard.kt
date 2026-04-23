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
import com.grippo.design.resources.provider.category_compound
import com.grippo.design.resources.provider.category_isolation
import com.grippo.design.resources.provider.force_type_hinge
import com.grippo.design.resources.provider.force_type_pull
import com.grippo.design.resources.provider.force_type_push
import com.grippo.design.resources.provider.training_profile_artifact_compound_vs_isolation
import com.grippo.design.resources.provider.training_profile_artifact_compound_vs_isolation_hint
import com.grippo.design.resources.provider.training_profile_artifact_empty
import com.grippo.design.resources.provider.training_profile_artifact_movement_style_subtitle
import com.grippo.design.resources.provider.training_profile_artifact_movement_style_title
import com.grippo.design.resources.provider.training_profile_artifact_push_vs_pull
import com.grippo.design.resources.provider.training_profile_artifact_push_vs_pull_hint

/**
 * Explains how the current training is balanced in terms of movement type
 * (compound vs. isolation) and direction (push vs. pull vs. hinge). Two
 * sub-sections each show labelled rows with percent share bars plus a
 * one-line hint explaining what the category actually means, so the
 * athlete can see at a glance whether the session was compound-dominant
 * or push-heavy — and understand why.
 */
@Composable
public fun TrainingProfileMovementStyleCard(
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
            text = AppTokens.strings.res(Res.string.training_profile_artifact_movement_style_title),
            style = AppTokens.typography.h6(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(Modifier.size(AppTokens.dp.contentPadding.text))

        Text(
            text = AppTokens.strings.res(Res.string.training_profile_artifact_movement_style_subtitle),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
        )

        if (value.isEmpty) {
            Spacer(Modifier.size(AppTokens.dp.contentPadding.block))
            Text(
                text = AppTokens.strings.res(Res.string.training_profile_artifact_empty),
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.secondary,
            )
            return@Column
        }

        Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

        // --- Compound vs Isolation sub-section ---
        Text(
            text = AppTokens.strings.res(Res.string.training_profile_artifact_compound_vs_isolation),
            style = AppTokens.typography.b14Semi(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(Modifier.size(AppTokens.dp.contentPadding.text))

        Text(
            text = AppTokens.strings.res(Res.string.training_profile_artifact_compound_vs_isolation_hint),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )

        Spacer(Modifier.size(AppTokens.dp.contentPadding.subContent))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            ContributionRow(
                label = AppTokens.strings.res(Res.string.category_compound),
                sharePercent = value.compoundRatio,
            )
            ContributionRow(
                label = AppTokens.strings.res(Res.string.category_isolation),
                sharePercent = value.isolationRatio,
            )
        }

        Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

        // --- Push vs Pull vs Hinge sub-section ---
        Text(
            text = AppTokens.strings.res(Res.string.training_profile_artifact_push_vs_pull),
            style = AppTokens.typography.b14Semi(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(Modifier.size(AppTokens.dp.contentPadding.text))

        Text(
            text = AppTokens.strings.res(Res.string.training_profile_artifact_push_vs_pull_hint),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )

        Spacer(Modifier.size(AppTokens.dp.contentPadding.subContent))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            ContributionRow(
                label = AppTokens.strings.res(Res.string.force_type_push),
                sharePercent = value.pushRatio,
            )
            ContributionRow(
                label = AppTokens.strings.res(Res.string.force_type_pull),
                sharePercent = value.pullRatio,
            )
            ContributionRow(
                label = AppTokens.strings.res(Res.string.force_type_hinge),
                sharePercent = value.hingeRatio,
            )
        }
    }
}

@AppPreview
@Composable
private fun TrainingProfileMovementStyleCardPreview() {
    PreviewContainer {
        TrainingProfileMovementStyleCard(
            value = stubTrainingLoadProfileArtifacts(),
        )
    }
}

@AppPreview
@Composable
private fun TrainingProfileMovementStyleCardEmptyPreview() {
    PreviewContainer {
        TrainingProfileMovementStyleCard(
            value = TrainingLoadProfileArtifactsState.empty(),
        )
    }
}
