package com.grippo.trainings.trainings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import com.grippo.design.components.empty.EmptyDecorations
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.no_data_yet

@Composable
internal fun TrainingsEmptyState(
    modifier: Modifier = Modifier
) {
    val text = AppTokens.strings.res(Res.string.no_data_yet)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        EmptyDecorations(
            modifier = Modifier
                .matchParentSize()
                .alpha(0.3f)
        )

        Text(
            text = text,
            style = AppTokens.typography.h6(),
            color = AppTokens.colors.text.tertiary,
            textAlign = TextAlign.Center
        )
    }
}

@AppPreview
@Composable
private fun TrainingsEmptyStatePreview() {
    PreviewContainer {
        TrainingsEmptyState()
    }
}
