package com.grippo.authorization.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.design.components.selectors.Toggle
import com.grippo.design.core.AppTokens
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.analytics_consent_title

@Composable
internal fun OnboardingAnalyticsConsent(
    modifier: Modifier = Modifier,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val title = AppTokens.strings.res(Res.string.analytics_consent_title)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = AppTokens.typography.h6(),
            color = AppTokens.colors.text.primary,
        )
        Toggle(
            checked = checked,
            enabled = enabled,
            contentDescription = title,
            onCheckedChange = { onCheckedChange(!checked) },
        )
    }
}
