package com.grippo.design.components.digest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.core.state.digest.MonthlyDigestState
import com.grippo.core.state.digest.stubMonthlyDigest
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.exercises
import com.grippo.design.resources.provider.icons.NavArrowRight
import com.grippo.design.resources.provider.icons.Trophy
import com.grippo.design.resources.provider.monthly_digest_template
import com.grippo.design.resources.provider.sets
import com.grippo.design.resources.provider.trainings
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils

@Composable
public fun MonthlyDigestCard(
    modifier: Modifier = Modifier,
    value: MonthlyDigestState,
    onViewStatsClick: () -> Unit,
) {
    Column(modifier = modifier.scalableClick(onClick = onViewStatsClick)) {

        val monthName = DateCompose.rememberFormat(value.month, DateFormat.MONTH_FULL, false)

        val digestTitle = AppTokens.strings.res(Res.string.monthly_digest_template, monthName)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {
            Icon(
                modifier = Modifier
                    .size(AppTokens.dp.digest.weekly.icon)
                    .background(
                        color = AppTokens.colors.brand.color4,
                        shape = CircleShape
                    ),
                imageVector = AppTokens.icons.Trophy,
                contentDescription = null,
                tint = AppTokens.colors.icon.primary
            )

            Text(
                modifier = Modifier.weight(1f),
                text = digestTitle,
                style = AppTokens.typography.h2(),
                color = AppTokens.colors.text.primary
            )

            Icon(
                modifier = Modifier.size(AppTokens.dp.digest.monthly.icon),
                imageVector = AppTokens.icons.NavArrowRight,
                tint = AppTokens.colors.icon.primary,
                contentDescription = null
            )
        }

        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

        MonthlyDigestStatGrid(
            firstRow = listOf(
                MonthlyDigestStatValue(
                    label = AppTokens.strings.res(Res.string.trainings),
                    value = value.trainingsCount.toString()
                ),
                MonthlyDigestStatValue(
                    label = AppTokens.strings.res(Res.string.exercises),
                    value = value.exercisesCount.toString()
                )
            ),
            secondRow = listOf(
                MonthlyDigestStatValue(
                    label = AppTokens.strings.res(Res.string.sets),
                    value = value.totalSets.toString()
                ),
                MonthlyDigestStatValue(
                    label = AppTokens.strings.res(Res.string.duration),
                    value = DateTimeUtils.format(value.duration)
                )
            )
        )
    }
}

@Immutable
private data class MonthlyDigestStatValue(
    val label: String,
    val value: String,
)

@Composable
private fun MonthlyDigestStatGrid(
    firstRow: List<MonthlyDigestStatValue>,
    secondRow: List<MonthlyDigestStatValue>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.digest.monthly.stat.radius)
            )
    ) {
        DigestStatRow(values = firstRow)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppTokens.colors.divider.default.copy(alpha = 0.3f))
        )
        DigestStatRow(values = secondRow)
    }
}

@Composable
private fun DigestStatRow(values: List<MonthlyDigestStatValue>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTokens.dp.contentPadding.content),
    ) {
        values.forEachIndexed { index, stat ->
            if (index != 0) {
                Spacer(
                    modifier = Modifier
                        .width(AppTokens.dp.contentPadding.content / 2)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stat.label,
                    style = AppTokens.typography.b12Med(),
                    color = AppTokens.colors.text.secondary,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

                Text(
                    text = stat.value,
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun MonthlyDigestCardPreview() {
    PreviewContainer {
        MonthlyDigestCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubMonthlyDigest(),
            onViewStatsClick = {}
        )
    }
}
