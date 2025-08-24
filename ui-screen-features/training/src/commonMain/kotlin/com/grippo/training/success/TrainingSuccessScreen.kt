package com.grippo.training.success

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.konfetti.KonfettiParade
import com.grippo.design.components.loading.Loader
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.components.toolbar.ToolbarStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.get_started_btn
import com.grippo.design.resources.provider.icons.Check
import com.grippo.design.resources.provider.workout_saved
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.delay

@Composable
internal fun TrainingSuccessScreen(
    state: TrainingSuccessState,
    loaders: ImmutableSet<TrainingSuccessLoader>,
    contract: TrainingSuccessContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    if (loaders.contains(TrainingSuccessLoader.SaveTraining)) {
        Loader(modifier = Modifier.fillMaxSize())
        return@BaseComposeScreen
    }

    val cardVisible = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(200)
        cardVisible.value = true
    }

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        style = ToolbarStyle.Transparent
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxSize()
                .padding(
                    horizontal = AppTokens.dp.screen.horizontalPadding,
                    vertical = AppTokens.dp.contentPadding.content
                ).imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Spacer(modifier = Modifier.weight(0.5f))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = AppTokens.strings.res(Res.string.workout_saved),
                style = AppTokens.typography.h1(),
                color = AppTokens.colors.text.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

            Button(
                modifier = Modifier.fillMaxWidth(),
                content = ButtonContent.Text(
                    text = AppTokens.strings.res(Res.string.get_started_btn),
                    startIcon = AppTokens.icons.Check,
                ),
                style = ButtonStyle.Primary,
                onClick = contract::onBack
            )
        }

        if (cardVisible.value) {
            KonfettiParade()
        }
    }
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        TrainingSuccessScreen(
            state = TrainingSuccessState,
            loaders = persistentSetOf(),
            contract = TrainingSuccessContract.Empty
        )
    }
}
