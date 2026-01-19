package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.metrics.TrainingLoadProfileState
import com.grippo.core.state.metrics.stubTrainingLoadProfile
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_profile

@Composable
public fun TrainingLoadProfileDetailsCard(
    modifier: Modifier = Modifier,
    value: TrainingLoadProfileState,
) {
    MetricSectionPanel(
        modifier = modifier,
        style = MetricSectionPanelStyle.Small,
    ) {
        Text(
            text = AppTokens.strings.res(Res.string.training_profile),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = value.title(),
            style = AppTokens.typography.h6(),
            color = AppTokens.colors.text.primary,
        )

        Text(
            text = value.subtitle(),
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.primary,
        )

        Text(
            text = value.details(),
            style = AppTokens.typography.b11Med(),
            color = AppTokens.colors.text.tertiary,
        )

        Text(
            text = value.dominanceLine(),
            style = AppTokens.typography.b11Med(),
            color = AppTokens.colors.text.tertiary,
        )

        Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
            Text(
                text = value.distributionLine(),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )

            val confidence = value.confidence.coerceIn(0, 100)
            Text(
                text = "Confidence $confidence% • ${value.confidenceLabel()}",
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )
        }

        value.tip()?.let { tip ->
            Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.text))

            Text(
                text = "Tip",
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.secondary,
            )

            Text(
                text = tip,
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.primary,
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
