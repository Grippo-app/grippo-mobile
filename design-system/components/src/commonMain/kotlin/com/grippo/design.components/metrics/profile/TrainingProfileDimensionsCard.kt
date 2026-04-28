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
import com.grippo.core.state.metrics.profile.TrainingDimensionKindState
import com.grippo.core.state.metrics.profile.TrainingLoadProfileState
import com.grippo.core.state.metrics.profile.stubTrainingLoadProfile
import com.grippo.design.components.metrics.profile.internal.ContributionRow
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_profile_dimensions_endurance_caption
import com.grippo.design.resources.provider.training_profile_dimensions_hypertrophy_caption
import com.grippo.design.resources.provider.training_profile_dimensions_strength_caption
import com.grippo.design.resources.provider.training_profile_dimensions_subtitle

@Composable
public fun TrainingProfileDimensionsCard(
    modifier: Modifier = Modifier,
    value: TrainingLoadProfileState,
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
            text = AppTokens.strings.res(Res.string.training_profile_dimensions_subtitle),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
        )

        Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            ContributionRow(
                label = TrainingDimensionKindState.Strength.label(),
                caption = AppTokens.strings.res(Res.string.training_profile_dimensions_strength_caption),
                sharePercent = value.scoreOf(TrainingDimensionKindState.Strength),
            )
            ContributionRow(
                label = TrainingDimensionKindState.Hypertrophy.label(),
                caption = AppTokens.strings.res(Res.string.training_profile_dimensions_hypertrophy_caption),
                sharePercent = value.scoreOf(TrainingDimensionKindState.Hypertrophy),
            )
            ContributionRow(
                label = TrainingDimensionKindState.Endurance.label(),
                caption = AppTokens.strings.res(Res.string.training_profile_dimensions_endurance_caption),
                sharePercent = value.scoreOf(TrainingDimensionKindState.Endurance),
            )
        }
    }
}

@AppPreview
@Composable
private fun TrainingProfileDimensionsCardPreview() {
    PreviewContainer {
        TrainingProfileDimensionsCard(
            value = stubTrainingLoadProfile(),
        )
    }
}
