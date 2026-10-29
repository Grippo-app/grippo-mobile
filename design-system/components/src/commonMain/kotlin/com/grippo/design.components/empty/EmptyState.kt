package com.grippo.design.components.empty

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.design.components.illustration.Illustration
import com.grippo.design.components.illustration.IllustrationStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.empty_achievements
import com.grippo.design.resources.provider.empty_daily_trainings
import com.grippo.design.resources.provider.empty_exercises
import com.grippo.design.resources.provider.empty_sets
import com.grippo.design.resources.provider.empty_training_exercises
import com.grippo.design.resources.provider.empty_workouts
import com.grippo.design.resources.provider.exercise_example_achievements_empty_description
import com.grippo.design.resources.provider.exercise_example_achievements_empty_title
import com.grippo.design.resources.provider.exercise_history_no_previous
import com.grippo.design.resources.provider.icons.LineUp
import com.grippo.design.resources.provider.training_first_set_hint
import com.grippo.design.resources.provider.training_no_sets

@Stable
public sealed interface EmptyStateStyle {
    @Stable
    public data object Text : EmptyStateStyle

    @Stable
    public data class Illustrated(
        val painter: Painter,
        val illustrationStyle: IllustrationStyle = IllustrationStyle.Large,
    ) : EmptyStateStyle

    @Stable
    public data class Compact(val icon: ImageVector) : EmptyStateStyle
}

@Composable
public fun EmptyState(
    modifier: Modifier = Modifier,
    text: String,
    style: EmptyStateStyle,
    title: String? = null,
) {
    when (style) {
        EmptyStateStyle.Text -> EmptyStateText(
            modifier = modifier,
            text = text,
            title = title,
            textAlign = TextAlign.Start,
        )

        is EmptyStateStyle.Illustrated -> {
            val illustrationColors = AppTokens.colors.illustration

            Column(
                modifier = modifier,
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
                    painter = style.painter,
                    style = style.illustrationStyle,
                )
                EmptyStateText(
                    modifier = Modifier.fillMaxWidth(),
                    text = text,
                    title = title,
                    textAlign = TextAlign.Center,
                )
            }
        }

        is EmptyStateStyle.Compact -> {
            val dp = AppTokens.dp.emptyState.compact
            val shape = RoundedCornerShape(dp.radius)

            Row(
                modifier = modifier
                    .background(color = AppTokens.colors.background.dialog, shape = shape)
                    .padding(dp.padding),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(dp.spacing),
            ) {
                Box(
                    modifier = Modifier
                        .size(dp.leadingIcon.size)
                        .background(
                            color = AppTokens.colors.background.card,
                            shape = RoundedCornerShape(dp.leadingIcon.radius),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        modifier = Modifier.size(dp.leadingIcon.iconSize),
                        imageVector = style.icon,
                        tint = AppTokens.colors.icon.secondary,
                        contentDescription = null,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = dp.leadingIcon.size),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    EmptyStateText(
                        text = text,
                        title = title,
                        textAlign = TextAlign.Start,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateText(
    modifier: Modifier = Modifier,
    text: String,
    title: String?,
    textAlign: TextAlign,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        if (title != null) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = title,
                style = AppTokens.typography.h4(),
                color = AppTokens.colors.text.primary,
                textAlign = textAlign,
            )
        }
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = text,
            style = AppTokens.typography.b14Reg(),
            color = AppTokens.colors.text.secondary,
            textAlign = textAlign,
        )
    }
}

@AppPreview
@Composable
private fun EmptyStateTextPreview() {
    PreviewContainer {
        EmptyState(
            text = AppTokens.strings.res(Res.string.empty_daily_trainings),
            style = EmptyStateStyle.Text,
        )
    }
}

@AppPreview
@Composable
private fun EmptyStateTextWithTitlePreview() {
    PreviewContainer {
        EmptyState(
            title = AppTokens.strings.res(Res.string.training_no_sets),
            text = AppTokens.strings.res(Res.string.training_first_set_hint),
            style = EmptyStateStyle.Text,
        )
    }
}

@AppPreview
@Composable
private fun EmptyStateIllustratedNoSetsPreview() {
    PreviewContainer {
        EmptyState(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(AppTokens.dp.screen.horizontalPadding),
            title = AppTokens.strings.res(Res.string.training_no_sets),
            text = AppTokens.strings.res(Res.string.training_first_set_hint),
            style = EmptyStateStyle.Illustrated(
                painter = AppTokens.drawables.res(Res.drawable.empty_sets),
                illustrationStyle = IllustrationStyle.Medium,
            ),
        )
    }
}

@AppPreview
@Composable
private fun EmptyStateIllustratedNoWorkoutsPreview() {
    PreviewContainer {
        EmptyState(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(AppTokens.dp.screen.horizontalPadding),
            text = AppTokens.strings.res(Res.string.empty_daily_trainings),
            style = EmptyStateStyle.Illustrated(
                painter = AppTokens.drawables.res(Res.drawable.empty_workouts),
            ),
        )
    }
}

@AppPreview
@Composable
private fun EmptyStateIllustratedCompactPreview() {
    PreviewContainer {
        EmptyState(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .verticalScroll(rememberScrollState())
                .padding(AppTokens.dp.dialog.horizontalPadding),
            title = AppTokens.strings.res(Res.string.exercise_example_achievements_empty_title),
            text = AppTokens.strings.res(Res.string.exercise_example_achievements_empty_description),
            style = EmptyStateStyle.Illustrated(
                painter = AppTokens.drawables.res(Res.drawable.empty_achievements),
                illustrationStyle = IllustrationStyle.Compact,
            ),
        )
    }
}

@AppPreview
@Composable
private fun EmptyStateIllustratedExercisesPreview() {
    PreviewContainer {
        EmptyState(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(AppTokens.dp.screen.horizontalPadding),
            text = AppTokens.strings.res(Res.string.empty_training_exercises),
            style = EmptyStateStyle.Illustrated(
                painter = AppTokens.drawables.res(Res.drawable.empty_exercises),
            ),
        )
    }
}

@AppPreview
@Composable
private fun EmptyStateCompactPreview() {
    PreviewContainer {
        EmptyState(
            modifier = Modifier.fillMaxWidth(),
            text = AppTokens.strings.res(Res.string.exercise_history_no_previous),
            style = EmptyStateStyle.Compact(icon = AppTokens.icons.LineUp),
        )
    }
}
