package com.grippo.design.components.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.metrics.digest.MonthlyDigestState
import com.grippo.core.state.metrics.digest.WeeklyDigestState
import com.grippo.core.state.metrics.digest.stubMonthlyDigest
import com.grippo.core.state.metrics.digest.stubWeeklyDigest
import com.grippo.design.components.metrics.MonthDigestCard
import com.grippo.design.components.metrics.WeekDigestCard
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.icons.LineUp
import com.grippo.design.resources.provider.personal_digests

@Composable
public fun DigestsCard(
    modifier: Modifier = Modifier,
    weekly: WeeklyDigestState,
    monthly: MonthlyDigestState,
    onWeeklyClick: () -> Unit,
    onMonthlyClick: () -> Unit
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.metrics.digests.icon),
                imageVector = AppTokens.icons.LineUp,
                tint = AppTokens.colors.icon.primary,
                contentDescription = null
            )

            Text(
                modifier = Modifier.weight(1f),
                text = AppTokens.strings.res(Res.string.personal_digests),
                style = AppTokens.typography.h4(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

        MonthDigestCard(
            modifier = Modifier
                .fillMaxWidth()
                .scalableClick(onClick = onMonthlyClick),
            value = monthly,
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

        WeekDigestCard(
            modifier = Modifier
                .fillMaxWidth()
                .scalableClick(onClick = onWeeklyClick),
            value = weekly,
        )
    }
}

@AppPreview
@Composable
private fun DigestsCardPreview() {
    PreviewContainer {
        DigestsCard(
            weekly = stubWeeklyDigest(),
            monthly = stubMonthlyDigest(),
            onWeeklyClick = {},
            onMonthlyClick = {}
        )
    }
}