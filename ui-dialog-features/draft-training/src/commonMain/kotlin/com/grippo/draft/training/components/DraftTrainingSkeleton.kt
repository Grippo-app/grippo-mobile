package com.grippo.draft.training.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import com.grippo.design.components.skeleton.SkeletonBone
import com.grippo.design.components.skeleton.SkeletonText
import com.grippo.design.components.utils.rememberTextHeight
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.loading

private const val EXERCISE_ROW_COUNT = 3

@Composable
internal fun DraftTrainingSkeleton(
    modifier: Modifier = Modifier,
) {
    val loadingDescription = AppTokens.strings.res(Res.string.loading)
    val descriptionStyle = AppTokens.typography.b14Med()
    val textMeasurer = rememberTextMeasurer()
    val descriptionHeight = rememberTextHeight(style = descriptionStyle, lines = 2, textMeasurer = textMeasurer)
    val statValueLine = rememberTextHeight(style = AppTokens.typography.h5(), textMeasurer = textMeasurer)
    val statLabelLine = rememberTextHeight(style = AppTokens.typography.b12Med(), textMeasurer = textMeasurer)
    val exerciseNameLine = rememberTextHeight(style = AppTokens.typography.b14Semi(), textMeasurer = textMeasurer)
    val exerciseSummaryLine = rememberTextHeight(style = AppTokens.typography.b13Med(), textMeasurer = textMeasurer)
    val textGap = AppTokens.dp.contentPadding.text
    val exercisePadding = AppTokens.dp.contentPadding.subContent
    val summaryHeight = statValueLine + textGap + statLabelLine + AppTokens.dp.metrics.panel.large.verticalPadding * 2
    val exerciseHeight = maxOf(
        AppTokens.dp.exerciseExampleImage.compact.size,
        exerciseNameLine + textGap + exerciseSummaryLine,
    )

    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = loadingDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AppTokens.dp.contentPadding.block),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
        ) {
            SkeletonText(
                modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.medium),
                style = AppTokens.typography.h3(),
                textMeasurer = textMeasurer,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(descriptionHeight),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                SkeletonText(
                    modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.long),
                    style = descriptionStyle,
                    textMeasurer = textMeasurer,
                )
                SkeletonText(
                    modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.medium),
                    style = descriptionStyle,
                    textMeasurer = textMeasurer,
                )
            }
        }
        SkeletonBone(
            modifier = Modifier
                .fillMaxWidth()
                .height(summaryHeight),
            shape = RoundedCornerShape(AppTokens.dp.metrics.panel.large.radius),
        )
        SkeletonText(
            modifier = Modifier
                .padding(
                    top = AppTokens.dp.contentPadding.block,
                    bottom = AppTokens.dp.contentPadding.text,
                )
                .fillMaxWidth(AppTokens.dp.skeleton.line.short),
            style = AppTokens.typography.h4(),
            textMeasurer = textMeasurer,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = exercisePadding),
            verticalArrangement = Arrangement.spacedBy(exercisePadding * 2 + AppTokens.dp.divider.thickness),
        ) {
            repeat(EXERCISE_ROW_COUNT) {
                SkeletonBone(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(exerciseHeight),
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun DraftTrainingSkeletonPreview() {
    PreviewContainer {
        DraftTrainingSkeleton(modifier = Modifier.fillMaxWidth())
    }
}
