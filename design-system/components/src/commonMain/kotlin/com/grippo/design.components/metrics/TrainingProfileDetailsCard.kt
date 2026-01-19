package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.state.metrics.TrainingLoadProfileState
import com.grippo.core.state.metrics.stubTrainingLoadProfile
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_profile_tip_label

@Composable
public fun TrainingLoadProfileDetailsCard(
    modifier: Modifier = Modifier,
    value: TrainingLoadProfileState,
) {
    Column(
        modifier = modifier,
    ) {
        Text(
            text = value.title(),
            style = AppTokens.typography.h4(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(Modifier.size(AppTokens.dp.contentPadding.text))

        Text(
            text = value.subtitle(),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
        )

        value.tip()?.let { tip ->
            Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.block))

            Text(
                text = AppTokens.strings.res(Res.string.training_profile_tip_label),
                style = AppTokens.typography.b12Med(),
                color = AppTokens.colors.text.tertiary,
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
