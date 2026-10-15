package com.grippo.exercise.example.exerciseexample.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.grippo.design.components.modifiers.shimmer
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Composable
internal fun ExerciseExampleDetailsAchievementsShimmer(
    modifier: Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = AppTokens.dp.dialog.horizontalPadding),
    ) {
        repeat(3) { index ->
            if (index > 0) Spacer(Modifier.size(AppTokens.dp.contentPadding.content))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AppTokens.dp.button.medium.height * 2)
                    .clip(RoundedCornerShape(AppTokens.dp.achievementCard.radius))
                    .background(AppTokens.colors.background.card)
                    .shimmer(),
            )
        }
    }
}

@AppPreview
@Composable
private fun ExerciseExampleDetailsAchievementsShimmerPreview() {
    PreviewContainer {
        ExerciseExampleDetailsAchievementsShimmer(
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
