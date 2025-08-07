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
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.got_it_btn
import com.grippo.design.resources.icons.WarningTriangleOutline
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
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

        Icon(
            modifier = Modifier.size(AppTokens.dp.error.icon),
            imageVector = AppTokens.icons.WarningTriangleOutline,
            tint = AppTokens.colors.semantic.error,
            contentDescription = null
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = state.title,
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center
        )

        if (state.description != null) {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                text = state.description,
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.secondary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Button(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.got_it_btn),
            style = ButtonStyle.Primary,
            onClick = contract::onDismiss
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        ErrorDisplayScreen(
            state = ErrorDisplayState(
                title = "Something went wrong",
                description = "Try again after few minutes."
            ),
            contract = ErrorDisplayContract.Empty,
            loaders = persistentSetOf()
        )
    }
}