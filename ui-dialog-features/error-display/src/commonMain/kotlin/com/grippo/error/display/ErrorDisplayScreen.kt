package com.grippo.error.display

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.core.state.error.AppErrorState
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.got_it_btn
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ErrorDisplayScreen(
    state: ErrorDisplayState,
    loaders: ImmutableSet<ErrorDisplayLoader>,
    contract: ErrorDisplayContract
) = BaseComposeScreen(background = ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    Text(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        text = state.error.title().text(),
        style = AppTokens.typography.h2(),
        color = AppTokens.colors.text.primary,
        textAlign = TextAlign.Center
    )

    val description = state.error.description()

    if (description != null) {
        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.subContent))

        Text(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
                .fillMaxWidth()
                .heightIn(max = 400.dp),
            text = description.text(),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center
        )
    }

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    Button(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        content = ButtonContent.Text(
            text = AppTokens.strings.res(Res.string.got_it_btn),
        ),
        style = ButtonStyle.Error,
        onClick = contract::onDismiss
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))

    Spacer(modifier = Modifier.navigationBarsPadding())
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        ErrorDisplayScreen(
            state = ErrorDisplayState(
                error = AppErrorState.Expected("Text error title", "Text error description")
            ),
            contract = ErrorDisplayContract.Empty,
            loaders = persistentSetOf()
        )
    }
}