package com.grippo.design.components.training

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.kg
import com.grippo.design.resources.reps
import com.grippo.presentation.api.trainings.models.IterationState
import com.grippo.presentation.api.trainings.models.stubIteration

@Composable
public fun IterationCard(
    modifier: Modifier = Modifier,
    value: IterationState,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(AppTokens.dp.iterationCard.radius))
            .border(
                width = 1.dp,
                color = AppTokens.colors.border.defaultPrimary,
                shape = RoundedCornerShape(AppTokens.dp.iterationCard.radius)
            ).padding(
                vertical = AppTokens.dp.iterationCard.verticalPadding,
                horizontal = AppTokens.dp.iterationCard.horizontalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Text(
            text = value.weight.toString(),
            style = AppTokens.typography.b12Bold(),
            color = AppTokens.colors.text.primary
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.text))

        Text(
            text = AppTokens.strings.res(Res.string.kg),
            style = AppTokens.typography.b12Semi(),
            color = AppTokens.colors.text.tertiary
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.text))

        Text(
            text = value.repetitions.toString(),
            style = AppTokens.typography.b12Bold(),
            color = AppTokens.colors.text.primary
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.text))

        Text(
            text = AppTokens.strings.res(Res.string.reps),
            style = AppTokens.typography.b12Semi(),
            color = AppTokens.colors.text.tertiary
        )
    }
}

@AppPreview
@Composable
private fun IterationCardPreview() {
    PreviewContainer {
        IterationCard(
            value = stubIteration(),
        )
        IterationCard(
            value = stubIteration(),
        )
        IterationCard(
            value = stubIteration(),
        )
        IterationCard(
            value = stubIteration(),
        )
    }
}