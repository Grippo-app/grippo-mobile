package com.grippo.authorization.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.authorization.onboarding.components.OnboardingAnalyticsConsent
import com.grippo.authorization.onboarding.components.OnboardingIntro
import com.grippo.core.foundation.BaseComposeScreen
import com.grippo.core.foundation.ScreenBackground
import com.grippo.design.components.banner.BannerCard
import com.grippo.design.components.banner.BannerCardStyle
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.Dumbbell
import com.grippo.design.resources.provider.icons.LineUp
import com.grippo.design.resources.provider.icons.Tune
import com.grippo.design.resources.provider.onboarding_get_started
import com.grippo.design.resources.provider.onboarding_progress_description
import com.grippo.design.resources.provider.onboarding_progress_title
import com.grippo.design.resources.provider.onboarding_setup_description
import com.grippo.design.resources.provider.onboarding_setup_title
import com.grippo.design.resources.provider.onboarding_workouts_description
import com.grippo.design.resources.provider.onboarding_workouts_title
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentSetOf

@Composable
internal fun OnboardingScreen(
    state: OnboardingState,
    loaders: ImmutableSet<OnboardingLoader>,
    contract: OnboardingContract,
) = BaseComposeScreen(ScreenBackground.Color(AppTokens.colors.background.screen)) {
    val benefitStyle = BannerCardStyle.Custom(
        tint = AppTokens.colors.icon.secondary,
        surface = AppTokens.colors.bannerCard.mutedBackground,
    )

    val enabled = state.analyticsConsent != null && loaders.isEmpty()

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = AppTokens.dp.screen.horizontalPadding,
                vertical = AppTokens.dp.screen.verticalPadding,
            ),
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block),
    ) {
        OnboardingIntro(modifier = Modifier.fillMaxWidth())
        Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)) {
            BannerCard(
                modifier = Modifier.fillMaxWidth(),
                style = benefitStyle,
                icon = AppTokens.icons.Dumbbell,
                title = AppTokens.strings.res(Res.string.onboarding_workouts_title),
                description = AppTokens.strings.res(Res.string.onboarding_workouts_description),
            )
            BannerCard(
                modifier = Modifier.fillMaxWidth(),
                style = benefitStyle,
                icon = AppTokens.icons.LineUp,
                title = AppTokens.strings.res(Res.string.onboarding_progress_title),
                description = AppTokens.strings.res(Res.string.onboarding_progress_description),
            )
            BannerCard(
                modifier = Modifier.fillMaxWidth(),
                style = benefitStyle,
                icon = AppTokens.icons.Tune,
                title = AppTokens.strings.res(Res.string.onboarding_setup_title),
                description = AppTokens.strings.res(Res.string.onboarding_setup_description),
            )
        }
    }
    OnboardingAnalyticsConsent(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
        checked = state.analyticsConsent != false,
        enabled = enabled,
        onCheckedChange = contract::onAnalyticsConsentChange,
    )
    Spacer(Modifier.size(AppTokens.dp.contentPadding.subContent))
    Button(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
        content = ButtonContent.Text(text = AppTokens.strings.res(Res.string.onboarding_get_started)),
        style = ButtonStyle.Primary,
        state = when {
            loaders.isNotEmpty() -> ButtonState.Loading
            !enabled -> ButtonState.Disabled
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
            state = OnboardingState(analyticsConsent = true),
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

@AppPreview
@Composable
private fun ScreenPreviewAnalyticsDisabled() {
    PreviewContainer {
        OnboardingScreen(
            state = OnboardingState(analyticsConsent = false),
            loaders = persistentSetOf(),
            contract = OnboardingContract.Empty,
        )
    }
}

@AppPreview
@Composable
private fun ScreenPreviewSavingConsent() {
    PreviewContainer {
        OnboardingScreen(
            state = OnboardingState(analyticsConsent = true),
            loaders = persistentSetOf(OnboardingLoader.AnalyticsConsent),
            contract = OnboardingContract.Empty,
        )
    }
}
