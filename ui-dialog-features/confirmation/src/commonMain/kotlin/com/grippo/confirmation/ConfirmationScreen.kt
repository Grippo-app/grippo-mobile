package com.grippo.confirmation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.cancel_btn
import com.grippo.design.resources.provider.confirm_btn
import com.grippo.design.resources.provider.icons.QuestionMarkCircleOutline
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun ConfirmationScreen(
    state: ConfirmationState,
    loaders: ImmutableSet<ConfirmationLoader>,
    contract: ConfirmationContract
) = BaseComposeScreen(background = ScreenBackground.Color(AppTokens.colors.background.dialog)) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

        Icon(
            modifier = Modifier.size(AppTokens.dp.confirmation.icon),
            imageVector = AppTokens.icons.QuestionMarkCircleOutline,
            tint = AppTokens.colors.semantic.info,
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
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.text))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                text = state.description,
                style = AppTokens.typography.b14Med(),
                color = AppTokens.colors.text.secondary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
                AppTokens.dp.contentPadding.content
            )
        ) {
            Button(
                modifier = Modifier.weight(1f),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.cancel_btn),
                ),
                style = ButtonStyle.Secondary,
                onClick = contract::onBack
            )

            Button(
                modifier = Modifier.weight(1f),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.confirm_btn),
                ),
                style = ButtonStyle.Primary,
                onClick = contract::onConfirm
            )
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.dialog.bottom))
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        ConfirmationScreen(
            state = ConfirmationState(
                title = "Confirm Action",
                description = "Are you sure you want to proceed with this action? This cannot be undone."
            ),
            contract = ConfirmationContract.Empty,
            loaders = persistentSetOf()
        )
    }
}
