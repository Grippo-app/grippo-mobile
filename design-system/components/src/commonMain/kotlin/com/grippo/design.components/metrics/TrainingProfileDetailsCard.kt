package com.grippo.design.components.metrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.TrainingLoadProfileState
import com.grippo.core.state.metrics.stubTrainingLoadProfile
import com.grippo.design.components.tip.TipCard
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun TrainingLoadProfileDetailsCard(
    modifier: Modifier = Modifier,
    value: TrainingLoadProfileState,
) {
    Column(
        modifier = modifier
            .background(
                AppTokens.colors.background.card,
                RoundedCornerShape(AppTokens.dp.metrics.trainingProfile.details.radius)
            ).padding(
                horizontal = AppTokens.dp.metrics.trainingProfile.details.horizontalPadding,
                vertical = AppTokens.dp.metrics.trainingProfile.details.verticalPadding
            ),
    ) {
        Text(
            text = value.title(),
            style = AppTokens.typography.h6(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(Modifier.size(AppTokens.dp.contentPadding.text))

        Text(
            text = value.subtitle(),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
        )

        value.tip()?.let { tip ->
            Spacer(Modifier.size(AppTokens.dp.contentPadding.block))

            TipCard(
                modifier = Modifier.fillMaxWidth(),
                value = tip
            )
        }
    }
}

@AppPreview
@Composable
private fun TrainingLoadProfileDetailsCardPreview() {
    PreviewContainer {
        TrainingLoadProfileDetailsCard(
            value = stubTrainingLoadProfile(),
        )
    }
}
