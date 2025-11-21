package com.grippo.design.components.digest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.grippo.core.state.digest.WeeklyDigestState
import com.grippo.core.state.digest.stubWeeklyDigest
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.exercises
import com.grippo.design.resources.provider.icons.NavArrowRight
import com.grippo.design.resources.provider.icons.Trophy
import com.grippo.design.resources.provider.sets
import com.grippo.design.resources.provider.trainings
import com.grippo.design.resources.provider.weekly_digest
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils

@Composable
public fun WeeklyDigestCard(
    modifier: Modifier = Modifier,
    value: WeeklyDigestState,
    onViewStatsClick: () -> Unit,
) {
    Column(modifier = modifier.scalableClick(onClick = onViewStatsClick)) {
        val start = DateCompose.rememberFormat(value.weekStart, DateFormat.DateOnly.DateDdMmm)
        val end = DateCompose.rememberFormat(value.weekEnd, DateFormat.DateOnly.DateDdMmm)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
        ) {
            Icon(
                modifier = Modifier
                    .size(AppTokens.dp.digest.weekly.icon)
                    .background(
                        color = AppTokens.colors.brand.color5,
                        shape = CircleShape
                    ),
                imageVector = AppTokens.icons.Trophy,
                contentDescription = null,
                tint = AppTokens.colors.icon.primary
            )

            Text(
                modifier = Modifier.weight(1f),
                text = AppTokens.strings.res(Res.string.weekly_digest),
                style = AppTokens.typography.h2(),
                color = AppTokens.colors.text.primary
            )

            Icon(
                modifier = Modifier.size(AppTokens.dp.digest.weekly.icon),
                imageVector = AppTokens.icons.NavArrowRight,
                tint = AppTokens.colors.icon.primary,
                contentDescription = null
            )
        }

        Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

        Text(
            text = "$start - $end",
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

        WeeklyDigestStatRow(
            values = listOf(
                WeeklyDigestStatValue(
                    label = AppTokens.strings.res(Res.string.trainings),
                    value = value.trainingsCount.toString()
                ),
                WeeklyDigestStatValue(
                    label = AppTokens.strings.res(Res.string.exercises),
                    value = value.exercisesCount.toString()
                )
            )
        )

        Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

        WeeklyDigestStatRow(
            values = listOf(
                WeeklyDigestStatValue(
                    label = AppTokens.strings.res(Res.string.duration),
                    value = DateTimeUtils.format(value.duration)
                ),
                WeeklyDigestStatValue(
                    label = AppTokens.strings.res(Res.string.sets),
                    value = value.totalSets.toString()
                )
            )
        )
    }
}

@Immutable
private data class WeeklyDigestStatValue(
    val label: String,
    val value: String,
)

@Composable
private fun WeeklyDigestStatRow(
    modifier: Modifier = Modifier,
    values: List<WeeklyDigestStatValue>,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        values.forEach { stat ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = AppTokens.colors.background.card,
                        shape = RoundedCornerShape(AppTokens.dp.digest.weekly.stat.radius)
                    )
                    .padding(
                        horizontal = AppTokens.dp.contentPadding.text,
                        vertical = AppTokens.dp.contentPadding.subContent
                    ),
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
private fun WeeklyDigestCardPreview() {
    PreviewContainer {
        WeeklyDigestCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubWeeklyDigest(),
            onViewStatsClick = {}
        )
    }
}
