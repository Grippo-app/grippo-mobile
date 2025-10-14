package com.grippo.error.display

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import com.grippo.design.resources.provider.icons.WarningTriangleOutline
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ErrorDisplayScreen(
    state: ErrorDisplayState,
    loaders: ImmutableSet<ErrorDisplayLoader>,
    contract: ErrorDisplayContract
) = BaseComposeScreen(background = ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

        Icon(
            modifier = Modifier.size(AppTokens.dp.error.icon),
            imageVector = AppTokens.icons.WarningTriangleOutline,
            tint = AppTokens.colors.semantic.error,
            contentDescription = null
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = state.error.title().text(),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        val description = state.error.description()

        if (description != null) {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                text = description.text(),
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.secondary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Button(
            modifier = Modifier.fillMaxWidth(),
            content = ButtonContent.Text(
                text = AppTokens.strings.res(Res.string.got_it_btn),
            ),
            style = ButtonStyle.Primary,
            onClick = contract::onDismiss
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        ErrorDisplayScreen(
            state = ErrorDisplayState(
                error = AppErrorState.Unknown
            ),
            contract = ErrorDisplayContract.Empty,
            loaders = persistentSetOf()
        )
    }
}