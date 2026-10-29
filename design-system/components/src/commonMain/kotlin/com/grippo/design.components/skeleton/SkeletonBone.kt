package com.grippo.design.components.skeleton

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
public fun SkeletonBone(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(AppTokens.dp.skeleton.radius),
    animated: Boolean = true,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(AppTokens.colors.skeleton.bone)
            .skeletonWave(enabled = animated),
    )
}

@AppPreview
@Composable
private fun SkeletonBonePreview() {
    PreviewContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SkeletonBone(
                modifier = Modifier.size(AppTokens.dp.userCard.avatar.small),
                shape = CircleShape,
            )
            SkeletonBone(
                modifier = Modifier
                    .weight(1f)
                    .height(AppTokens.dp.button.small.height),
                shape = RoundedCornerShape(AppTokens.dp.button.small.radius),
            )
            SkeletonBone(
                modifier = Modifier
                    .weight(1f)
                    .height(AppTokens.dp.button.small.height),
                animated = false,
            )
        }
    }
}
