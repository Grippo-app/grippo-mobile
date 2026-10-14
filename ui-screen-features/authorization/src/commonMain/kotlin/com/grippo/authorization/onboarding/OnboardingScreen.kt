package com.grippo.authorization.onboarding

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.onboarding_get_started
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun OnboardingScreen(
    state: OnboardingState,
    loaders: ImmutableSet<OnboardingLoader>,
    contract: OnboardingContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {
    Spacer(Modifier.weight(1f))
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
        content = ButtonContent.Text(text = AppTokens.strings.res(Res.string.onboarding_get_started)),
        style = ButtonStyle.Primary,
        state = when {
            loaders.isNotEmpty() -> ButtonState.Loading
            !state.isReady -> ButtonState.Disabled
            else -> ButtonState.Enabled
        },
        onClick = contract::onGetStarted,
    )
    Spacer(Modifier.size(AppTokens.dp.screen.verticalPadding))
    Spacer(Modifier.navigationBarsPadding())
}

@AppPreview
@Composable
private fun ScreenPreview() {
    PreviewContainer {
        OnboardingScreen(
            state = OnboardingState(isReady = true),
            loaders = persistentSetOf(),
            contract = OnboardingContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewPreparing() {
    PreviewContainer {
        OnboardingScreen(
            state = OnboardingState(),
            loaders = persistentSetOf(OnboardingLoader.Preparation),
            contract = OnboardingContract.Empty,
        )
    }
}
