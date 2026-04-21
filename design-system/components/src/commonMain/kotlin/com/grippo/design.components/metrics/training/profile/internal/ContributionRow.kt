package com.grippo.design.components.metrics.training.profile.internal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.components.indicators.LineIndicator
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.AppColor
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.training_profile_artifact_share

@Composable
internal fun ContributionRow(
    modifier: Modifier = Modifier,
    label: String,
    caption: String? = null,
    sharePercent: Int,
    colors: AppColor.Charts.IndicatorColors.IndicatorColors = AppTokens.colors.charts.indicator.primary,
) {
    val clamped = sharePercent.coerceIn(0, 100)
    val progress = clamped / 100f

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = AppTokens.typography.b14Semi(),
                    color = AppTokens.colors.text.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!caption.isNullOrBlank()) {

                    Spacer(Modifier.size(AppTokens.dp.contentPadding.text))

                    Text(
                        text = caption,
                        style = AppTokens.typography.b12Med(),
                        color = AppTokens.colors.text.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.size(AppTokens.dp.contentPadding.subContent))

            Text(
                text = AppTokens.strings.res(Res.string.training_profile_artifact_share, clamped),
                style = AppTokens.typography.b14Semi(),
                color = AppTokens.colors.text.primary,
                maxLines = 1,
            )
        }

        Spacer(Modifier.size(AppTokens.dp.contentPadding.text))

        LineIndicator(
            modifier = Modifier.fillMaxWidth(),
            progress = progress,
            colors = colors,
        )
    }
}

@AppPreview
@Composable
private fun ContributionRowPreview() {
    PreviewContainer {
        ContributionRow(
            label = "Compound",
            caption = "12 exercises",
            sharePercent = 72,
        )

        ContributionRow(
            label = "Isolation",
            caption = "5 exercises",
            sharePercent = 28,
            colors = AppTokens.colors.charts.indicator.info,
        )

        ContributionRow(
            label = "Push",
            sharePercent = 55,
            colors = AppTokens.colors.charts.indicator.success,
        )

        ContributionRow(
            label = "Pull",
            caption = "3 exercises",
            sharePercent = 35,
            colors = AppTokens.colors.charts.indicator.warning,
        )

        ContributionRow(
            label = "Hinge",
            caption = "1 exercise",
            sharePercent = 10,
            colors = AppTokens.colors.charts.indicator.muted,
        )

        ContributionRow(
            label = "Very long label that should ellipsize when it does not fit",
            caption = "Also a pretty long caption that should ellipsize gracefully",
            sharePercent = 100,
        )

        ContributionRow(
            label = "Empty",
            sharePercent = 0,
        )
    }
}
