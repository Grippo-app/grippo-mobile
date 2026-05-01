package com.grippo.design.components.empty

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.EmptyExercise
import com.grippo.design.resources.provider.icons.EmptyExerciseExample
import com.grippo.design.resources.provider.no_data_yet

@Composable
public fun EmptyState(
    modifier: Modifier = Modifier,
    value: ImageVector,
    text: String,
) {
    Column(
        modifier = modifier.padding(horizontal = AppTokens.dp.screen.horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.6f))

        Image(
            modifier = Modifier.height(AppTokens.dp.empty.image),
            imageVector = value,
            contentDescription = null,
            contentScale = ContentScale.FillHeight
        )

        Text(
            text = text,
            style = AppTokens.typography.h5(),
            color = AppTokens.colors.text.tertiary,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.weight(1f))
    }
}

@AppPreview
@Composable
private fun EmptyState1Preview() {
    PreviewContainer {
        EmptyState(
            modifier = Modifier.fillMaxSize(),
            value = AppTokens.icons.EmptyExercise,
            text = AppTokens.strings.res(Res.string.no_data_yet)
        )
    }
}

@AppPreview
@Composable
private fun EmptyState2Preview() {
    PreviewContainer {
        EmptyState(
            modifier = Modifier.fillMaxSize(),
            value = AppTokens.icons.EmptyExerciseExample,
            text = AppTokens.strings.res(Res.string.no_data_yet)
        )
    }
}
