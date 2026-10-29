package com.grippo.exercise.example.exerciseexample.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.grippo.design.components.empty.IllustratedEmptyState
import com.grippo.design.components.illustration.IllustrationStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.empty_achievements
import com.grippo.design.resources.provider.exercise_example_achievements_empty_description
import com.grippo.design.resources.provider.exercise_example_achievements_empty_title

@Composable
internal fun ExerciseExampleDetailsAchievementsEmpty(modifier: Modifier = Modifier) {
    IllustratedEmptyState(
        modifier = modifier,
        painter = AppTokens.drawables.res(Res.drawable.empty_achievements),
        title = AppTokens.strings.res(Res.string.exercise_example_achievements_empty_title),
        text = AppTokens.strings.res(Res.string.exercise_example_achievements_empty_description),
        contentPadding = PaddingValues(horizontal = AppTokens.dp.dialog.horizontalPadding),
        illustrationStyle = IllustrationStyle.Medium,
    )
}

@AppPreview
@Composable
private fun ExerciseExampleDetailsAchievementsEmptyPreview() {
    PreviewContainer {
        ExerciseExampleDetailsAchievementsEmpty(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .background(AppTokens.colors.background.dialog),
        )
    }
}
