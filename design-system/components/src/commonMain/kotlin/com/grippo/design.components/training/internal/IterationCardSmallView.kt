package com.grippo.design.components.training.internal

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.core.state.trainings.IterationState
import com.grippo.core.state.trainings.stubIteration
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun IterationCardSmallView(
    modifier: Modifier = Modifier,
    value: IterationState,
) {
    Row(
        modifier = modifier.height(intrinsicSize = IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Text(
            text = value.external.short(),
            style = AppTokens.typography.b14Bold(),
            color = AppTokens.colors.text.secondary
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.text))

        Text(
            text = value.repetitions.short(),
            style = AppTokens.typography.b14Semi(),
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
