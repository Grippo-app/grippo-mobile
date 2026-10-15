package com.grippo.design.components.empty

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.design.components.illustration.Illustration
import com.grippo.design.components.illustration.IllustrationStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.empty_daily_trainings
import com.grippo.design.resources.provider.empty_exercises
import com.grippo.design.resources.provider.empty_sets
import com.grippo.design.resources.provider.empty_training_exercises
import com.grippo.design.resources.provider.empty_workouts
import com.grippo.design.resources.provider.training_first_set_hint
import com.grippo.design.resources.provider.training_no_sets

@Composable
public fun IllustratedEmptyState(
    modifier: Modifier = Modifier,
    painter: Painter,
    text: String,
    contentPadding: PaddingValues,
    title: String? = null,
    illustrationStyle: IllustrationStyle = IllustrationStyle.Large,
) {
    val illustrationColors = AppTokens.colors.illustration

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            space = AppTokens.dp.contentPadding.block,
            alignment = Alignment.CenterVertically,
        ),
    ) {
        Illustration(
            modifier = Modifier
                .fillMaxWidth()
                .drawWithCache {
                    val radius = size.minDimension / 2f
                    if (radius <= 0f) {
                        onDrawBehind {}
                    } else {
                        val halo = Brush.radialGradient(
                            colors = listOf(
                                illustrationColors.backgroundStart,
                                illustrationColors.backgroundEnd,
                                Color.Transparent,
                            ),
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = radius,
                        )
                        onDrawBehind {
                            drawCircle(brush = halo, radius = radius)
                        }
                    }
                },
            painter = painter,
            style = illustrationStyle,
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        ) {
            if (title != null) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = title,
                    style = AppTokens.typography.h4(),
                    color = AppTokens.colors.text.primary,
                    textAlign = TextAlign.Center,
                )
            }
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = text,
                style = if (title == null) AppTokens.typography.h5() else AppTokens.typography.b14Med(),
                color = if (title == null) AppTokens.colors.text.primary else AppTokens.colors.text.secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@AppPreview
@Composable
private fun IllustratedEmptyStateNoSetsPreview() {
    PreviewContainer {
        IllustratedEmptyState(
            modifier = Modifier.fillMaxSize(),
            painter = AppTokens.drawables.res(Res.drawable.empty_sets),
            title = AppTokens.strings.res(Res.string.training_no_sets),
            text = AppTokens.strings.res(Res.string.training_first_set_hint),
            illustrationStyle = IllustrationStyle.Medium,
            contentPadding = PaddingValues(AppTokens.dp.screen.horizontalPadding),
        )
    }
}

@AppPreview
@Composable
private fun IllustratedEmptyStateNoWorkoutsPreview() {
    PreviewContainer {
        IllustratedEmptyState(
            modifier = Modifier.fillMaxSize(),
            painter = AppTokens.drawables.res(Res.drawable.empty_workouts),
            text = AppTokens.strings.res(Res.string.empty_daily_trainings),
            contentPadding = PaddingValues(AppTokens.dp.screen.horizontalPadding),
        )
    }
}

@AppPreview
@Composable
private fun IllustratedEmptyStateCompactPreview() {
    PreviewContainer {
        IllustratedEmptyState(
            modifier = Modifier.fillMaxWidth().height(400.dp),
            painter = AppTokens.drawables.res(Res.drawable.empty_exercises),
            title = "Пока нет упражнений для этой тренировки",
            text = "Добавьте первое упражнение, чтобы начать отслеживать подходы и прогресс тренировки.",
            illustrationStyle = IllustrationStyle.Compact,
            contentPadding = PaddingValues(AppTokens.dp.dialog.horizontalPadding),
        )
    }
}

@AppPreview
@Composable
private fun IllustratedEmptyStatePreview() {
    PreviewContainer {
        IllustratedEmptyState(
            modifier = Modifier.fillMaxSize(),
            painter = AppTokens.drawables.res(Res.drawable.empty_exercises),
            text = AppTokens.strings.res(Res.string.empty_training_exercises),
            contentPadding = PaddingValues(AppTokens.dp.screen.horizontalPadding),
        )
    }
}
