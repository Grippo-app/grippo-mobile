package com.grippo.training.streak.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import com.grippo.design.components.metrics.engagement.streak.TrainingStreakCardSkeleton
import com.grippo.design.components.metrics.engagement.streak.TrainingStreakCardStyle
import com.grippo.design.components.skeleton.SkeletonBone
import com.grippo.design.components.skeleton.SkeletonText
import com.grippo.design.components.utils.rememberTextHeight
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.loading

private const val TIMELINE_PERIOD_COUNT = 4

@Composable
internal fun TrainingStreakDetailsLoadingContent(modifier: Modifier = Modifier) {
    val loadingDescription = AppTokens.strings.res(Res.string.loading)
    val textMeasurer = rememberTextMeasurer()
    val periodLine = rememberTextHeight(style = AppTokens.typography.b13Semi(), textMeasurer = textMeasurer)
    val valueLine = rememberTextHeight(style = AppTokens.typography.b12Med(), textMeasurer = textMeasurer)
    val textGap = AppTokens.dp.contentPadding.text
    val periodHeight = periodLine + textGap + valueLine + textGap + AppTokens.dp.metrics.charts.indicator.height

    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = loadingDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block),
    ) {
        TrainingStreakCardSkeleton(
            modifier = Modifier.fillMaxWidth(),
            style = TrainingStreakCardStyle.Detailed,
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            SkeletonText(
                modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.short),
                style = AppTokens.typography.h4(),
                textMeasurer = textMeasurer,
            )
            SkeletonText(
                modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.long),
                style = AppTokens.typography.b12Med(),
                textMeasurer = textMeasurer,
            )
            repeat(TIMELINE_PERIOD_COUNT) {
                SkeletonBone(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(periodHeight),
                )
            }
        }

        SkeletonBone(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppTokens.dp.button.medium.height),
            shape = RoundedCornerShape(AppTokens.dp.button.medium.radius),
            animated = false,
        )
    }
}

@AppPreview
@Composable
private fun TrainingStreakDetailsLoadingContentPreview() {
    PreviewContainer {
        TrainingStreakDetailsLoadingContent(modifier = Modifier.fillMaxWidth())
    }
}
