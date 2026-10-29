package com.grippo.profile.body.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
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
import com.grippo.design.components.skeleton.SkeletonBone
import com.grippo.design.components.utils.rememberTextHeight
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.loading

@Composable
internal fun ProfileBodyInputsSkeleton(modifier: Modifier = Modifier) {
    val loadingDescription = AppTokens.strings.res(Res.string.loading)

    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = loadingDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        repeat(2) {
            SkeletonBone(
                modifier = Modifier.fillMaxWidth().height(AppTokens.dp.input.height),
                shape = RoundedCornerShape(AppTokens.dp.input.radius),
                animated = false,
            )
        }
    }
}

@Composable
internal fun ProfileBodyHistorySkeleton(modifier: Modifier = Modifier) {
    val loadingDescription = AppTokens.strings.res(Res.string.loading)
    val textMeasurer = rememberTextMeasurer()
    val dateHeight = rememberTextHeight(style = AppTokens.typography.b12Med(), textMeasurer = textMeasurer)
    val weightHeight = rememberTextHeight(style = AppTokens.typography.h6(), textMeasurer = textMeasurer)
    val cardHeight = dateHeight + weightHeight + AppTokens.dp.contentPadding.text +
        AppTokens.dp.weightHistoryCard.verticalPadding * 2

    Column(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = loadingDescription
            progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
        },
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        SkeletonBone(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(AppTokens.dp.weightHistoryCard.chartAspectRatio),
        )

        repeat(3) {
            SkeletonBone(
                modifier = Modifier.fillMaxWidth().height(cardHeight),
                shape = RoundedCornerShape(AppTokens.dp.weightHistoryCard.radius),
            )
        }
    }
}

@AppPreview
@Composable
private fun ProfileBodyInputsSkeletonPreview() {
    PreviewContainer {
        ProfileBodyInputsSkeleton(modifier = Modifier.fillMaxWidth())
    }
}

@AppPreview
@Composable
private fun ProfileBodyHistorySkeletonPreview() {
    PreviewContainer {
        ProfileBodyHistorySkeleton(modifier = Modifier.fillMaxWidth())
    }
}
