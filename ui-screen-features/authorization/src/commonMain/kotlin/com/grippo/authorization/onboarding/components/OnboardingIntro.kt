package com.grippo.authorization.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.design.components.illustration.Illustration
import com.grippo.design.components.illustration.IllustrationStyle
import com.grippo.design.components.modifiers.illustrationBackground
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.onboarding_description
import com.grippo.design.resources.provider.onboarding_hero
import com.grippo.design.resources.provider.onboarding_title

@Composable
internal fun OnboardingIntro(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        Illustration(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTokens.dp.screen.onboardingHeroHeight)
                .illustrationBackground(RoundedCornerShape(AppTokens.dp.screen.onboardingHeroRadius))
                .padding(AppTokens.dp.contentPadding.content),
            painter = AppTokens.drawables.res(Res.drawable.onboarding_hero),
            style = IllustrationStyle.Large,
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.onboarding_title),
            style = AppTokens.typography.h2(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.onboarding_description),
            style = AppTokens.typography.b14Med(),
            color = AppTokens.colors.text.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@AppPreview
@Composable
private fun OnboardingIntroPreview() {
    PreviewContainer {
        OnboardingIntro(modifier = Modifier.fillMaxWidth())
    }
}
