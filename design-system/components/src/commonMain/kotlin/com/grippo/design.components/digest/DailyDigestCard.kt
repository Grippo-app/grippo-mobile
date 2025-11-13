package com.grippo.design.components.digest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.core.state.digest.DailyDigestState
import com.grippo.core.state.digest.stubDailyDigest
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.chip.ChipSize
import com.grippo.design.components.chip.VolumeChip
import com.grippo.design.components.chip.VolumeChipStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.daily_digest
import com.grippo.design.resources.provider.icons.FireFlame
import com.grippo.design.resources.provider.one_set
import com.grippo.design.resources.provider.value_sets
import com.grippo.design.resources.provider.view_stats_btn
import com.grippo.toolkit.date.utils.DateCompose
import com.grippo.toolkit.date.utils.DateFormat

@Composable
public fun DailyDigestCard(
    modifier: Modifier = Modifier,
    value: DailyDigestState,
    onViewStatsClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(AppTokens.dp.digest.daily.radius))
            .height(intrinsicSize = IntrinsicSize.Min)
            .background(
                color = AppTokens.colors.background.card,
                shape = RoundedCornerShape(AppTokens.dp.digest.daily.radius)
            )
    ) {
        Icon(
            modifier = Modifier
                .padding(end = AppTokens.dp.contentPadding.content)
                .scale(1.8f)
                .size(AppTokens.dp.digest.daily.icon)
                .alpha(0.2f)
                .align(Alignment.CenterEnd),
            imageVector = AppTokens.icons.FireFlame,
            contentDescription = null,
            tint = AppTokens.colors.icon.secondary,
        )

        Column(
            modifier = Modifier.padding(
                horizontal = AppTokens.dp.digest.daily.horizontalPadding,
                vertical = AppTokens.dp.digest.daily.verticalPadding
            ),
        ) {
            val formattedDate = DateCompose.rememberFormat(value.date, DateFormat.DATE_DD_MMM)

            val sets = if (value.totalSets == 1) {
                AppTokens.strings.res(Res.string.one_set)
            } else {
                AppTokens.strings.res(Res.string.value_sets, value.totalSets)
            }

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = AppTokens.strings.res(Res.string.daily_digest),
                style = AppTokens.typography.h2(),
                color = AppTokens.colors.text.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(AppTokens.dp.contentPadding.text))

            val string = remember {
                buildString {
                    append(formattedDate)
                    append(" · ")
                    append(value.duration)
                    append(" · ")
                    append(sets)
                }
            }

            Text(
                text = string,
                style = AppTokens.typography.b13Med(),
                color = AppTokens.colors.text.primary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(AppTokens.dp.contentPadding.content))

            Row(modifier = Modifier.fillMaxWidth()) {
                VolumeChip(
                    modifier = Modifier,
                    value = value.total,
                    style = VolumeChipStyle.SHORT,
                    size = ChipSize.Medium
                )

                Spacer(Modifier.weight(1f))

                Button(
                    content = ButtonContent.Text(
                        AppTokens.strings.res(Res.string.view_stats_btn)
                    ),
                    size = ButtonSize.Small,
                    onClick = onViewStatsClick
                )
            }

        }
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
