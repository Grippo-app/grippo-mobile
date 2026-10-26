package com.grippo.start.training.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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

@Composable
internal fun StartTrainingSkeleton(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
) {
    val loadingDescription = AppTokens.strings.res(Res.string.loading)
    val descriptionStyle = AppTokens.typography.b14Med()
    val textMeasurer = rememberTextMeasurer()
    val segmentLine = rememberTextHeight(style = AppTokens.typography.b14Bold(), textMeasurer = textMeasurer)
    val descriptionHeight = rememberTextHeight(style = descriptionStyle, lines = 2, textMeasurer = textMeasurer)
    val segmentHeight = segmentLine + AppTokens.dp.contentPadding.subContent * 2 + AppTokens.dp.segment.fill.space * 2

    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = loadingDescription
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            }
            .padding(contentPadding),
    ) {
        SkeletonBone(
            modifier = Modifier
                .fillMaxWidth()
                .height(segmentHeight),
            shape = RoundedCornerShape(AppTokens.dp.segment.fill.outRadius),
            animated = false,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = AppTokens.dp.contentPadding.block),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                space = AppTokens.dp.contentPadding.block,
                alignment = Alignment.CenterVertically,
            ),
        ) {
            SkeletonBone(
                modifier = Modifier.size(AppTokens.dp.illustration.large),
                shape = CircleShape,
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                SkeletonText(
                    modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.medium),
                    style = AppTokens.typography.h4(),
                    textMeasurer = textMeasurer,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(descriptionHeight),
                    horizontalAlignment = Alignment.CenterHorizontally,
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
        }
    }
}

@AppPreview
@Composable
private fun StartTrainingSkeletonPreview() {
    PreviewContainer {
        StartTrainingSkeleton(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = AppTokens.dp.dialog.horizontalPadding,
                end = AppTokens.dp.dialog.horizontalPadding,
                top = AppTokens.dp.dialog.top,
            ),
        )
    }
}
