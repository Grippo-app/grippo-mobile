package com.grippo.authorization.onboarding.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.GrippoLogo
import com.grippo.design.resources.provider.onboarding_description
import com.grippo.design.resources.provider.onboarding_title

@Composable
internal fun OnboardingIntro(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        Image(
            modifier = Modifier.size(AppTokens.dp.screen.logoSize),
            imageVector = AppTokens.icons.GrippoLogo,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.onboarding_title),
            style = AppTokens.typography.h1(),
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
