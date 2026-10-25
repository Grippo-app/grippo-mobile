package com.grippo.exercise.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.loading

@Composable
internal fun ExerciseLoadingContent(modifier: Modifier = Modifier) {
    val loadingDescription = AppTokens.strings.res(Res.string.loading)
    val textMeasurer = rememberTextMeasurer()
    val image = AppTokens.dp.exerciseExampleImage.medium

    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = loadingDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonBone(
                modifier = Modifier.size(image.size),
                shape = RoundedCornerShape(image.radius),
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            ) {
                SkeletonText(
                    modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.long),
                    style = AppTokens.typography.h4(),
                    textMeasurer = textMeasurer,
                )
                SkeletonText(
                    modifier = Modifier.fillMaxWidth(AppTokens.dp.skeleton.line.short),
                    style = AppTokens.typography.b12Med(),
                    textMeasurer = textMeasurer,
                )
            }
        }

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        ExerciseSetsSkeleton(modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

        ExerciseProgressCardSkeleton(modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.size(AppTokens.dp.contentPadding.block))

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
private fun ExerciseLoadingContentPreview() {
    PreviewContainer {
        ExerciseLoadingContent(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppTokens.colors.background.dialog)
                .padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
        )
    }
}
