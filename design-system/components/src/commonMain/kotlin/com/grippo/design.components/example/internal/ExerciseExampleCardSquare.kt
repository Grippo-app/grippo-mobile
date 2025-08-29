package com.grippo.design.components.example.internal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.state.exercise.examples.ExerciseExampleState

@Composable
internal fun ExerciseExampleCardSquare(
    modifier: Modifier,
    value: ExerciseExampleState,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(AppTokens.dp.exerciseExampleCard.square.radius)

    Box(
        modifier = modifier
            .scalableClick(onClick = onClick)
            .background(AppTokens.colors.background.card, shape)
            .aspectRatio(1f)
            .padding(
                horizontal = AppTokens.dp.exerciseExampleCard.square.horizontalPadding,
                vertical = AppTokens.dp.exerciseExampleCard.square.verticalPadding
            )
    ) {
        // No icon/image. Use text-only with gradient footer and pills.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to AppTokens.colors.background.card
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTokens.dp.contentPadding.subContent)
            ) {
                Text(
                    text = value.value.name,
                    style = AppTokens.typography.b14Bold(),
                    color = AppTokens.colors.text.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(AppTokens.dp.contentPadding.text))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Pill(text = value.value.category.title().text())
                    Pill(text = value.value.forceType.title().text())
                }
            }
        }

        // Quick badge (top right)
        Pill(
            modifier = Modifier.align(Alignment.TopEnd),
            text = value.value.weightType.title().text()
        )
    }
}

@Composable
private fun Pill(
    modifier: Modifier = Modifier,
    text: String
) {
    Row(
        modifier = modifier
            .background(AppTokens.colors.background.screen, RoundedCornerShape(999.dp))
            .padding(
                horizontal = AppTokens.dp.contentPadding.subContent,
                vertical = AppTokens.dp.contentPadding.text
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = AppTokens.typography.b11Med(),
            color = AppTokens.colors.text.secondary
        )
    }
}


