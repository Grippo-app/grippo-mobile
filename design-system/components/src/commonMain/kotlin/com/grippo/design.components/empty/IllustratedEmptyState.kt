package com.grippo.design.components.empty

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import com.grippo.design.components.illustration.Illustration
import com.grippo.design.components.illustration.IllustrationStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.empty_exercises
import com.grippo.design.resources.provider.empty_training_exercises

@Composable
public fun IllustratedEmptyState(
    modifier: Modifier = Modifier,
    painter: Painter,
    text: String,
    contentPadding: PaddingValues,
    title: String? = null,
    illustrationStyle: IllustrationStyle = IllustrationStyle.Large,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = AppTokens.dp.contentPadding.content,
            alignment = Alignment.CenterVertically,
        ),
    ) {
        Illustration(
            modifier = Modifier.fillMaxWidth(),
            painter = painter,
            style = illustrationStyle,
        )
        if (title != null) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                style = AppTokens.typography.h4(),
                color = AppTokens.colors.text.primary,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = text,
            style = if (title == null) AppTokens.typography.h5() else AppTokens.typography.b14Med(),
            color = if (title == null) AppTokens.colors.text.tertiary else AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@AppPreview
@Composable
private fun IllustratedEmptyStatePreview() {
    PreviewContainer {
        IllustratedEmptyState(
            modifier = Modifier.fillMaxSize(),
            painter = AppTokens.drawables.res(Res.drawable.empty_exercises),
            text = AppTokens.strings.res(Res.string.empty_training_exercises),
            contentPadding = PaddingValues(AppTokens.dp.screen.horizontalPadding),
        )
    }
}
