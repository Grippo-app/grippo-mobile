package com.grippo.statistics.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import com.grippo.design.components.metrics.distribution.muscle.loading.MuscleLoadingSkeleton
import com.grippo.design.components.metrics.volume.VolumeMetricChartSkeleton
import com.grippo.design.components.skeleton.SkeletonBone
import com.grippo.design.components.skeleton.SkeletonText
import com.grippo.design.components.utils.rememberTextHeight
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.loading

private const val TOTAL_CHIP_COUNT = 2

@Composable
internal fun StatisticsLoadingContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val loadingDescription = AppTokens.strings.res(Res.string.loading)

    LazyColumn(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = loadingDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.block),
    ) {
        item(key = "total_chips_loading") {
            StatisticsTotalSkeleton(modifier = Modifier.fillMaxWidth())
        }

        item(key = "exercise_volume_loading") {
            VolumeMetricChartSkeleton(modifier = Modifier.fillMaxWidth())
        }

        item(key = "muscle_load_loading") {
            StatisticsMuscleLoadSkeleton(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StatisticsTotalSkeleton(modifier: Modifier = Modifier) {
    val valueLine = rememberTextHeight(style = AppTokens.typography.b14Semi())
    val chipHeight = maxOf(AppTokens.dp.chip.medium.height, valueLine + AppTokens.dp.chip.verticalPadding * 2)
    val chipShape = RoundedCornerShape(AppTokens.dp.chip.medium.radius)

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
    ) {
        repeat(TOTAL_CHIP_COUNT) {
            SkeletonBone(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chipHeight),
                shape = chipShape,
            )
        }
    }
}

@Composable
private fun StatisticsMuscleLoadSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        SkeletonText(
            modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.short),
            style = AppTokens.typography.h4(),
        )
        MuscleLoadingSkeleton(modifier = Modifier.fillMaxWidth())
    }
}

@AppPreview
@Composable
private fun StatisticsLoadingContentPreview() {
    PreviewContainer {
        StatisticsLoadingContent(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding),
        )
    }
}
