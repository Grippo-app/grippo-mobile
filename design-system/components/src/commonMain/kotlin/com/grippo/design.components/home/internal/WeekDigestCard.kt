package com.grippo.design.components.home.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import com.grippo.core.state.trainings.digest.WeeklyDigestState
import com.grippo.core.state.trainings.digest.stubWeeklyDigest
import com.grippo.design.components.modifiers.spot
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.duration
import com.grippo.design.resources.provider.icons.Trophy
import com.grippo.design.resources.provider.sets
import com.grippo.design.resources.provider.trainings
import com.grippo.design.resources.provider.volume
import com.grippo.design.resources.provider.weekly
import com.grippo.toolkit.date.utils.DateTimeUtils

@Composable
internal fun WeekDigestCard(
    modifier: Modifier = Modifier,
    value: WeeklyDigestState,
) {
    Box(
        modifier = modifier
            .height(intrinsicSize = IntrinsicSize.Max)
            .clip(RoundedCornerShape(AppTokens.dp.home.digest.week.radius))
            .background(
                AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.home.digest.week.radius)
            )
    ) {
        Icon(
            modifier = Modifier
                .spot(color = AppTokens.colors.brand.color4)
                .align(Alignment.CenterEnd)
                .offset(x = (AppTokens.dp.home.digest.week.image / 2))
                .size(AppTokens.dp.home.digest.week.image)
                .scale(1.6f)
                .alpha(0.2f),
            imageVector = AppTokens.icons.Trophy,
            contentDescription = null,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    vertical = AppTokens.dp.home.digest.week.verticalPadding,
                    horizontal = AppTokens.dp.home.digest.week.horizontalPadding
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)
            ) {
                Icon(
                    modifier = Modifier.size(AppTokens.dp.home.digest.week.icon),
                    imageVector = AppTokens.icons.Trophy,
                    contentDescription = null,
                    tint = AppTokens.colors.brand.color4,
                )

                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = AppTokens.strings.res(Res.string.weekly),
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.brand.color4,
                )
            }

            Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

            WeeklyDigestStatRow(
                modifier = Modifier.fillMaxWidth(),
                label = AppTokens.strings.res(Res.string.trainings),
                value = value.trainingsCount.toString()
            )

            Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

            WeeklyDigestStatRow(
                modifier = Modifier.fillMaxWidth(),
                label = AppTokens.strings.res(Res.string.sets),
                value = value.totalSets.toString()
            )

            Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

            WeeklyDigestStatRow(
                modifier = Modifier.fillMaxWidth(),
                label = AppTokens.strings.res(Res.string.duration),
                value = DateTimeUtils.format(value.duration)
            )

            Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

            WeeklyDigestStatRow(
                modifier = Modifier.fillMaxWidth(),
                label = AppTokens.strings.res(Res.string.volume),
                value = value.total.short()
            )

            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun WeeklyDigestStatRow(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = label,
            style = AppTokens.typography.b12Med(),
            color = AppTokens.colors.text.secondary,
        )

        Text(
            text = value,
            style = AppTokens.typography.b12Semi(),
            color = AppTokens.colors.text.primary,
        )
    }
}

@AppPreview
@Composable
private fun WeekDigestCardPreview() {
    PreviewContainer {
        WeekDigestCard(
            value = stubWeeklyDigest(),
        )
    }
}
