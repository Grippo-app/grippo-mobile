package com.grippo.design.components.metrics.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.metrics.profile.TrainingLoadProfileState
import com.grippo.core.state.metrics.profile.stubTrainingLoadProfile
import com.grippo.design.components.metrics.internal.MetricSectionPanel
import com.grippo.design.components.metrics.internal.MetricSectionPanelStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_profile

@Composable
public fun TrainingLoadProfileCard(
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

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
        ) {
            Text(
                text = value.title(),
                style = AppTokens.typography.h3(),
                color = AppTokens.colors.semantic.notice,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = value.subtitle(),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.semantic.notice.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@AppPreview
@Composable
private fun TrainingLoadProfileCardPreview() {
    PreviewContainer {
        TrainingLoadProfileCard(
            value = stubTrainingLoadProfile()
        )
    }
}
