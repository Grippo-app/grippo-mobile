package com.grippo.design.components.digest

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.digest.DailyDigestState
import com.grippo.core.state.digest.stubDailyDigest
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.daily_digest_template
import com.grippo.design.resources.provider.icons.ArrowRight
import com.grippo.design.resources.provider.icons.Trophy
import com.grippo.design.resources.provider.one_set
import com.grippo.design.resources.provider.value_sets
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat
import com.grippo.toolkit.date.utils.DateTimeUtils

@Composable
public fun DailyDigestCard(
    modifier: Modifier = Modifier,
    value: DailyDigestState,
    onViewStatsClick: () -> Unit
) {
    Column(modifier = modifier.scalableClick(onClick = onViewStatsClick)) {
        val formattedDate = DateCompose.rememberFormat(value.date, DateFormat.DateOnly.DateDdMmm)

        val sets = if (value.totalSets == 1) {
            AppTokens.strings.res(Res.string.one_set)
        } else {
            AppTokens.strings.res(Res.string.value_sets, value.totalSets)
        }

        val weekDayName = DateCompose.rememberFormat(
            value.date,
            DateFormat.DateOnly.WeekdayLong,
            false
        )

        val digestTitle = AppTokens.strings.res(Res.string.daily_digest_template, weekDayName)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent)
        ) {
            Icon(
                modifier = Modifier.size(AppTokens.dp.digest.daily.icon),
                imageVector = AppTokens.icons.Trophy,
                contentDescription = null,
                tint = AppTokens.colors.brand.color1,
            )

            Text(
                modifier = Modifier.weight(1f),
                text = digestTitle,
                style = AppTokens.typography.h2(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Icon(
                modifier = Modifier.size(AppTokens.dp.digest.daily.icon),
                imageVector = AppTokens.icons.ArrowRight,
                tint = AppTokens.colors.icon.primary,
                contentDescription = null
            )
        }

        Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

        val string = remember {
            buildString {
                append(formattedDate)
                append(" · ")
                append(DateTimeUtils.format(value.duration))
                append(" · ")
                append(sets)
            }
        }

        Text(
            text = string,
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@AppPreview
@Composable
private fun DailyDigestCardPreview() {
    PreviewContainer {
        DailyDigestCard(
            modifier = Modifier.fillMaxWidth(),
            value = stubDailyDigest(),
            onViewStatsClick = {}
        )
    }
}
