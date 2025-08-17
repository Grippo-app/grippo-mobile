package com.grippo.training.setup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.BaseComposeScreen
import com.grippo.core.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.components.toolbar.Toolbar
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.trainings
import com.grippo.state.muscles.stubMuscles
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun TrainingSetupScreen(
    state: TrainingSetupState,
    loaders: ImmutableSet<TrainingSetupLoader>,
    contract: TrainingSetupContract
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {

    Toolbar(
        modifier = Modifier.fillMaxWidth(),
        title = AppTokens.strings.res(Res.string.trainings),
        onBack = contract::onBack,
    )

    LazyColumn(
        modifier = Modifier.fillMaxWidth().weight(1f)
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
    ) {
        // todo implement content
    }

    Button(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
            .fillMaxWidth(),
        text = "Continue",
        style = ButtonStyle.Primary,
        onClick = contract::onContinueClick
    )

    Spacer(Modifier.size(AppTokens.dp.contentPadding.content))

    Button(
        modifier = Modifier
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
            .fillMaxWidth(),
        text = "Skip",
        style = ButtonStyle.Secondary,
        onClick = contract::onContinueClick
    )

    Spacer(modifier = Modifier.size(AppTokens.dp.screen.verticalPadding))

    Spacer(modifier = Modifier.navigationBarsPadding())
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        TrainingSetupScreen(
            state = TrainingSetupState(
                muscles = stubMuscles()
            ),
            loaders = persistentSetOf(),
            contract = TrainingSetupContract.Empty
        )
    }
}