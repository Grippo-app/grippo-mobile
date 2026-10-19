package com.grippo.design.components.muscle

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.core.state.muscles.MuscleEnumState
import com.grippo.core.state.muscles.MuscleGroupEnumState
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Immutable
public enum class MuscleMarkStyle {
    Standard,
    Inline,
    Summary,
}

/** Anatomical zone beside its localized label; measured views may supply their load tint. */
@Composable
public fun MuscleMark(
    modifier: Modifier = Modifier,
    group: MuscleGroupEnumState,
    style: MuscleMarkStyle = MuscleMarkStyle.Standard,
    color: Color? = null,
) {
    MuscleMarkContent(modifier = modifier, icon = group.icon(color ?: group.color()), style = style)
}

@Composable
public fun MuscleMark(
    modifier: Modifier = Modifier,
    muscle: MuscleEnumState,
    style: MuscleMarkStyle = MuscleMarkStyle.Standard,
    color: Color? = null,
) {
    MuscleMarkContent(modifier = modifier, icon = muscle.icon(color ?: muscle.color()), style = style)
}

@Composable
private fun MuscleMarkContent(
    modifier: Modifier,
    icon: ImageVector,
    style: MuscleMarkStyle,
) {
    val markModifier = when (style) {
        MuscleMarkStyle.Standard -> {
            val shape = RoundedCornerShape(AppTokens.dp.muscle.markRadius)
            Modifier
                .size(AppTokens.dp.muscle.markSize)
                .clip(shape)
                .background(AppTokens.colors.background.card)
                .padding(AppTokens.dp.muscle.markPadding)
        }
        MuscleMarkStyle.Inline -> Modifier.size(AppTokens.dp.muscle.inlineMarkSize)
        MuscleMarkStyle.Summary -> Modifier.size(AppTokens.dp.muscle.summaryMarkSize)
    }
    Image(
        modifier = modifier.then(markModifier),
        imageVector = icon,
        contentDescription = null,
    )
}

@AppPreview
@Composable
private fun MuscleMarkGroupsPreview() {
    PreviewContainer {
        Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)) {
            MuscleGroupEnumState.entries.forEach { group ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                ) {
                    MuscleMark(group = group)
                    Text(
                        text = group.title().text(),
                        color = AppTokens.colors.text.primary,
                        style = AppTokens.typography.b14Med(),
                    )
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun MuscleMarkMusclesPreview() {
    PreviewContainer {
        Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content)) {
            listOf(
                MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR,
                MuscleEnumState.TRAPEZIUS,
                MuscleEnumState.RECTUS_ABDOMINIS,
                MuscleEnumState.QUADRICEPS,
                MuscleEnumState.FOREARM,
                MuscleEnumState.POSTERIOR_DELTOID,
            ).forEach { muscle ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
                ) {
                    MuscleMark(muscle = muscle)
                    Text(
                        text = muscle.title().text(),
                        color = AppTokens.colors.text.primary,
                        style = AppTokens.typography.b14Med(),
                    )
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun MuscleMarkInlinePreview() {
    PreviewContainer {
        MuscleGroupEnumState.entries.forEach { group ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            ) {
                MuscleMark(group = group, style = MuscleMarkStyle.Inline)
                Text(
                    text = group.title().text(),
                    color = AppTokens.colors.text.secondary,
                    style = AppTokens.typography.b12Med(),
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun MuscleMarkSummaryPreview() {
    PreviewContainer {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
            MuscleGroupEnumState.entries.forEach { group ->
                MuscleMark(
                    group = group,
                    style = MuscleMarkStyle.Summary,
                    color = AppTokens.colors.icon.secondary,
                )
            }
        }
    }
}
