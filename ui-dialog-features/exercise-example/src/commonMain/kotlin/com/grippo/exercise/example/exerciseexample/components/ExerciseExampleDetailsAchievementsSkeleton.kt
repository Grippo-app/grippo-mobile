package com.grippo.exercise.example.exerciseexample.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import com.grippo.design.components.metrics.volume.VolumeMetricChartSkeleton
import com.grippo.design.components.skeleton.SkeletonBone
import com.grippo.design.components.skeleton.SkeletonText
import com.grippo.design.components.utils.rememberTextHeight
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.loading

private const val ACHIEVEMENT_COUNT = 3
private const val HISTORY_ROW_COUNT = 3

@Composable
internal fun ExerciseExampleDetailsAchievementsSkeleton(modifier: Modifier = Modifier) {
    val loadingDescription = AppTokens.strings.res(Res.string.loading)
    val textMeasurer = rememberTextMeasurer()
    val captionLine = rememberTextHeight(style = AppTokens.typography.b12Med(), textMeasurer = textMeasurer)
    val achievementValueLine = rememberTextHeight(style = AppTokens.typography.h4(), textMeasurer = textMeasurer)
    val estimateValueLine = rememberTextHeight(style = AppTokens.typography.h5(), textMeasurer = textMeasurer)
    val dateLine = rememberTextHeight(style = AppTokens.typography.h6(), textMeasurer = textMeasurer)
    val deltaLine = rememberTextHeight(style = AppTokens.typography.b12Semi(), textMeasurer = textMeasurer)
    val setsLine = rememberTextHeight(style = AppTokens.typography.b14Semi(), textMeasurer = textMeasurer)
    val horizontalPadding = AppTokens.dp.dialog.horizontalPadding
    val achievementCard = AppTokens.dp.achievementCard
    val exerciseCard = AppTokens.dp.exerciseCard.small
    val achievementHeight = achievementCard.verticalPadding * 2 + maxOf(achievementCard.emblem.size, captionLine) +
        AppTokens.dp.contentPadding.content + achievementValueLine
    val estimateHeight = captionLine * 3 + estimateValueLine + AppTokens.dp.metrics.performance.strength.height +
        AppTokens.dp.contentPadding.subContent * 4
    val exerciseCardHeight = exerciseCard.verticalPadding * 2 + maxOf(dateLine, deltaLine) +
        AppTokens.dp.contentPadding.subContent + setsLine

    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = loadingDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds()
                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                .padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
        ) {
            repeat(ACHIEVEMENT_COUNT) {
                SkeletonBone(
                    modifier = Modifier
                        .width(achievementCard.width)
                        .height(achievementHeight),
                    shape = RoundedCornerShape(achievementCard.radius),
                )
            }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        SkeletonBone(
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .fillMaxWidth()
                .height(estimateHeight),
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        SkeletonText(
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .fillMaxWidth(AppTokens.dp.skeleton.line.short),
            style = AppTokens.typography.h4(),
            textMeasurer = textMeasurer,
        )

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

        VolumeMetricChartSkeleton(
            modifier = Modifier
                .padding(horizontal = horizontalPadding)
                .fillMaxWidth(),
        )

        repeat(HISTORY_ROW_COUNT) {
            Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.content))

            SkeletonBone(
                modifier = Modifier
                    .padding(horizontal = horizontalPadding)
                    .fillMaxWidth()
                    .height(exerciseCardHeight),
                shape = RoundedCornerShape(exerciseCard.radius),
            )
        }
    }
}

@AppPreview
@Composable
private fun ExerciseExampleDetailsAchievementsSkeletonPreview() {
    PreviewContainer {
        ExerciseExampleDetailsAchievementsSkeleton(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTokens.colors.background.dialog),
        )
    }
}
