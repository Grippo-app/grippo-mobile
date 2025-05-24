package com.grippo.design.components.training

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.grippo.design.components.modifiers.ShadowElevation
import com.grippo.design.components.modifiers.shadowDefault
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
    index: Int
) {

    val shape = RoundedCornerShape(AppTokens.dp.shape.small)

    Row(
        modifier = modifier.height(intrinsicSize = IntrinsicSize.Max),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .shadowDefault(
                    elevation = ShadowElevation.Component,
                    shape = shape,
                    color = AppTokens.colors.overlay.defaultShadow
                )
                .clip(shape = shape)
                .background(AppTokens.colors.background.secondary)
                .border(1.dp, AppTokens.colors.border.defaultPrimary, shape)
                .size(AppTokens.dp.size.smallComponentHeight)
                .wrapContentSize(),
            text = (index + 1).toString(),
            style = AppTokens.typography.b12Bold(),
            color = AppTokens.colors.text.primary,
        )

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            modifier = Modifier.width(58.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value.weight.toString(),
                style = AppTokens.typography.b14Bold(),
                color = AppTokens.colors.text.primary
            )

            Text(
                text = AppTokens.strings.res(Res.string.kg),
                style = AppTokens.typography.b14Semi(),
                color = AppTokens.colors.text.secondary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            color = AppTokens.colors.divider.default
        )

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            modifier = Modifier.width(52.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value.repetitions.toString(),
                style = AppTokens.typography.b14Bold(),
                color = AppTokens.colors.text.primary
            )

            Text(
                text = AppTokens.strings.res(Res.string.reps),
                style = AppTokens.typography.b14Semi(),
                color = AppTokens.colors.text.secondary
            )
        }
    }
}

@AppPreview
@Composable
private fun IterationCardPreview() {
    PreviewContainer {
        IterationCard(
            value = stubIteration(),
            index = 0
        )
    }
}