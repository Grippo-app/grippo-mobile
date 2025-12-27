package com.grippo.home.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.empty.EmptyDecorations
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.home_empty_subtitle
import com.grippo.design.resources.provider.home_empty_title
import com.grippo.design.resources.provider.start_workout

@Composable
internal fun EmptyHomeContent(
    modifier: Modifier,
    onStartTraining: () -> Unit
) {
    val title = AppTokens.strings.res(Res.string.home_empty_title)
    val subtitle = AppTokens.strings.res(Res.string.home_empty_subtitle)

    Box(modifier = modifier) {
        EmptyDecorations(
            modifier = Modifier.matchParentSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Text(
                text = title,
                style = AppTokens.typography.h2(),
                color = AppTokens.colors.text.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

            Text(
                text = subtitle,
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.secondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Button(
                modifier = Modifier.fillMaxWidth(),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.start_workout),
                ),
                style = ButtonStyle.Primary,
                onClick = onStartTraining
            )
        }
    }
}