package com.grippo.design.components.metrics

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
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
