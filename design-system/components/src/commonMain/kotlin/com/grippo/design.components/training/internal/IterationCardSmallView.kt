package com.grippo.design.components.training.internal

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.state.trainings.IterationState
import com.grippo.state.trainings.stubIteration

@Composable
internal fun IterationCardSmallView(
    modifier: Modifier = Modifier,
    value: IterationState,
) {
    Row(
        modifier = modifier
            .height(intrinsicSize = IntrinsicSize.Min)
            .padding(
                vertical = AppTokens.dp.iterationCard.smallView.verticalPadding,
                horizontal = AppTokens.dp.iterationCard.smallView.horizontalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Text(
            text = value.volume.short(),
            style = AppTokens.typography.b13Bold(),
            color = AppTokens.colors.text.primary
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.text))

        Text(
            text = value.repetitions.short(),
            style = AppTokens.typography.b13Semi(),
            color = AppTokens.colors.text.secondary
        )
    }
}

@AppPreview
@Composable
private fun IterationCardSmallViewPreview() {
    PreviewContainer {
        IterationCardSmallView(
            value = stubIteration(),
        )
        IterationCardSmallView(
            value = stubIteration(),
        )
        IterationCardSmallView(
            value = stubIteration(),
        )
        IterationCardSmallView(
            value = stubIteration(),
        )
    }
}
