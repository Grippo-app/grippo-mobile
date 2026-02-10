package com.grippo.confirm.training.completion

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.inputs.InputDuration
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.cancel_btn
import com.grippo.design.resources.provider.confirm_btn
import com.grippo.design.resources.provider.confirm_training_completion_title
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlin.time.Duration.Companion.minutes

@Composable
internal fun ConfirmTrainingCompletionScreen(
    state: ConfirmTrainingCompletionState,
    loaders: ImmutableSet<ConfirmTrainingCompletionLoader>,
    contract: ConfirmTrainingCompletionContract
) = BaseComposeScreen(background = ScreenBackground.Color(AppTokens.colors.background.dialog)) {
    Spacer(modifier = Modifier.size(AppTokens.dp.dialog.top))

    Text(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        text = AppTokens.strings.res(Res.string.confirm_training_completion_title),
        style = AppTokens.typography.h2(),
        color = AppTokens.colors.text.primary,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    InputDuration(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
        value = state.duration,
        onClick = contract::onDurationInputClick
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

    Row(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.dialog.horizontalPadding)
            .fillMaxWidth(),
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

    Spacer(modifier = Modifier.navigationBarsPadding())
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        ConfirmTrainingCompletionScreen(
            state = ConfirmTrainingCompletionState(
                duration = 45.minutes
            ),
            contract = ConfirmTrainingCompletionContract.Empty,
            loaders = persistentSetOf()
        )
    }
}
