package com.grippo.authorization.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.design.components.selectors.Check
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.onboarding_analytics_consent

@Composable
internal fun OnboardingAnalyticsConsent(
    modifier: Modifier = Modifier,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val title = AppTokens.strings.res(Res.string.onboarding_analytics_consent)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        Check(
            checked = checked,
            enabled = enabled,
            contentDescription = title,
            onCheckedChange = { onCheckedChange(!checked) },
        )
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}
