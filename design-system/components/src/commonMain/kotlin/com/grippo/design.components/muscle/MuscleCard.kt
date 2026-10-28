package com.grippo.design.components.muscle

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import com.grippo.core.state.muscles.MuscleEnumState
import com.grippo.core.state.muscles.MuscleGroupEnumState
import com.grippo.design.components.cards.selectable.ToggleSelectableCard
import com.grippo.design.components.cards.selectable.ToggleSelectableCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.muscles.MuscleColorPreset
import com.grippo.design.resources.provider.muscles.bodySplit
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList

@Stable
public sealed interface MuscleCardStyle {
    @Immutable
    public data object Compact : MuscleCardStyle

    @Immutable
    public data object Inline : MuscleCardStyle

    @Immutable
    public data class Load(
        public val color: Color,
    ) : MuscleCardStyle

    @Stable
    public data class Selection(
        public val isSelected: Boolean,
        public val onSelect: () -> Unit,
    ) : MuscleCardStyle
}

@Composable
public fun MuscleCard(
    modifier: Modifier = Modifier,
    value: MuscleEnumState,
    style: MuscleCardStyle,
) {
    when (style) {
        MuscleCardStyle.Compact -> MuscleCardCompact(
            modifier = modifier,
            title = value.title().text(),
            icon = value.image(
                rememberCompactPreset(
                    muscles = setOf(value),
                    highlight = value.color(),
                )
            ),
        )

        MuscleCardStyle.Inline -> MuscleCardInline(
            modifier = modifier,
            icon = value.image(
                rememberCompactPreset(
                    muscles = setOf(value),
                    highlight = value.color(),
                )
            ),
        )

        is MuscleCardStyle.Load -> MuscleCardLoad(
            modifier = modifier,
            icon = value.icon(style.color),
        )

        is MuscleCardStyle.Selection -> MuscleCardSelection(
            modifier = modifier,
            title = value.title().text(),
            icon = value.icon(),
            style = style,
        )
    }
}

@Composable
public fun MuscleCard(
    modifier: Modifier = Modifier,
    value: MuscleGroupEnumState,
    style: MuscleCardStyle,
) {
    when (style) {
        MuscleCardStyle.Compact -> MuscleCardCompact(
            modifier = modifier,
            title = value.title().text(),
            icon = value.image(
                rememberCompactPreset(
                    muscles = rememberGroupMuscles(groups = listOf(value)),
                    highlight = value.color(),
                )
            ),
        )

        MuscleCardStyle.Inline -> MuscleCardInline(
            modifier = modifier,
            icon = value.image(
                rememberCompactPreset(
                    muscles = rememberGroupMuscles(groups = listOf(value)),
                    highlight = value.color(),
                )
            ),
        )

        is MuscleCardStyle.Load -> MuscleCardLoad(
            modifier = modifier,
            icon = value.icon(style.color),
        )

        is MuscleCardStyle.Selection -> MuscleCardSelection(
            modifier = modifier,
            title = value.title().text(),
            icon = value.icon(),
            style = style,
        )
    }
}

@Composable
public fun MuscleCard(
    modifier: Modifier = Modifier,
    value: ImmutableList<MuscleGroupEnumState>,
    style: MuscleCardStyle.Compact,
) {
    if (value.isEmpty()) return

    val upperGroups = remember(value) {
        value.distinct().filterNot { it == MuscleGroupEnumState.LEGS }.toPersistentList()
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        when (upperGroups.size) {
            0 -> Unit
            1 -> MuscleCard(value = upperGroups.single(), style = style)
            else -> {
                val preset = rememberCompactPreset(
                    muscles = rememberGroupMuscles(groups = upperGroups),
                    highlight = AppTokens.colors.muscle.highlight,
                )

                MuscleCardCompact(
                    modifier = Modifier,
                    title = upperGroups.map { it.title().text() }.joinToString(separator = ", "),
                    icon = remember(preset) { bodySplit(preset) },
                )
            }
        }

        if (MuscleGroupEnumState.LEGS in value) {
            MuscleCard(value = MuscleGroupEnumState.LEGS, style = style)
        }
    }
}

@Composable
private fun rememberCompactPreset(
    muscles: Set<MuscleEnumState>,
    highlight: Color,
): MuscleColorPreset {
    val colors = AppTokens.colors.muscle
    return remember(muscles, colors, highlight) {
        MuscleEnumState.colorPreset(
            fallback = colors.compact.inactive,
            outline = colors.outline,
            background = colors.compact.background,
            colorFor = { muscle -> if (muscle in muscles) highlight else colors.compact.inactive },
        )
    }
}

@Composable
private fun rememberGroupMuscles(groups: List<MuscleGroupEnumState>): Set<MuscleEnumState> {
    return remember(groups) {
        MuscleEnumState.entries.filterTo(mutableSetOf()) { it.group() in groups }
    }
}

@Composable
private fun MuscleCardCompact(
    modifier: Modifier,
    title: String,
    icon: ImageVector,
) {
    Image(
        modifier = modifier
            .size(AppTokens.dp.muscleCard.compact.icon)
            .semantics {
                text = AnnotatedString(title)
            },
        imageVector = icon,
        contentDescription = null,
    )
}

@Composable
private fun MuscleCardInline(
    modifier: Modifier,
    icon: ImageVector,
) {
    Image(
        modifier = modifier.size(AppTokens.dp.muscleCard.inline.icon),
        imageVector = icon,
        contentDescription = null,
    )
}

@Composable
private fun MuscleCardLoad(
    modifier: Modifier,
    icon: ImageVector,
) {
    val shape = RoundedCornerShape(AppTokens.dp.muscleCard.load.radius)

    Image(
        modifier = modifier
            .size(AppTokens.dp.muscleCard.load.size)
            .clip(shape)
            .background(AppTokens.colors.background.card)
            .padding(AppTokens.dp.muscleCard.load.padding),
        imageVector = icon,
        contentDescription = null,
    )
}

@Composable
private fun MuscleCardSelection(
    modifier: Modifier,
    title: String,
    icon: ImageVector,
    style: MuscleCardStyle.Selection,
) {
    ToggleSelectableCard(
        modifier = modifier.fillMaxWidth(),
        style = ToggleSelectableCardStyle.Medium(
            title = title,
            icon = icon,
        ),
        isSelected = style.isSelected,
        onSelect = style.onSelect,
    )
}

@AppPreview
@Composable
private fun MuscleCardCompactPreview() {
    PreviewContainer {
        Row(horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
            MuscleGroupEnumState.entries.forEach { group ->
                MuscleCard(value = group, style = MuscleCardStyle.Compact)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
            listOf(
                MuscleEnumState.BICEPS,
                MuscleEnumState.TRICEPS,
                MuscleEnumState.FOREARM,
            ).forEach { muscle ->
                MuscleCard(value = muscle, style = MuscleCardStyle.Compact)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
            listOf(
                persistentListOf(MuscleGroupEnumState.CHEST_MUSCLES),
                persistentListOf(MuscleGroupEnumState.LEGS),
                persistentListOf(MuscleGroupEnumState.CHEST_MUSCLES, MuscleGroupEnumState.LEGS),
                persistentListOf(
                    MuscleGroupEnumState.BACK_MUSCLES,
                    MuscleGroupEnumState.ARMS_AND_FOREARMS,
                    MuscleGroupEnumState.SHOULDER_MUSCLES,
                ),
                persistentListOf(
                    MuscleGroupEnumState.CHEST_MUSCLES,
                    MuscleGroupEnumState.ABDOMINAL_MUSCLES,
                    MuscleGroupEnumState.LEGS,
                ),
            ).forEach { groups ->
                MuscleCard(value = groups, style = MuscleCardStyle.Compact)
            }
        }
    }
}

@AppPreview
@Composable
private fun MuscleCardInlinePreview() {
    PreviewContainer {
        MuscleGroupEnumState.entries.forEach { group ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.subContent),
            ) {
                MuscleCard(value = group, style = MuscleCardStyle.Inline)
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
private fun MuscleCardLoadPreview() {
    PreviewContainer {
        val palette = AppTokens.colors.muscle.palette6MuscleCalm
        Row(horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
            MuscleGroupEnumState.entries.forEachIndexed { index, group ->
                MuscleCard(
                    value = group,
                    style = MuscleCardStyle.Load(color = palette[index % palette.size]),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text)) {
            listOf(
                MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR,
                MuscleEnumState.TRAPEZIUS,
                MuscleEnumState.LATERAL_DELTOID,
                MuscleEnumState.FOREARM,
                MuscleEnumState.QUADRICEPS,
            ).forEachIndexed { index, muscle ->
                MuscleCard(
                    value = muscle,
                    style = MuscleCardStyle.Load(color = palette[index % palette.size]),
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun MuscleCardSelectedPreview() {
    PreviewContainer {
        listOf(
            MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR,
            MuscleEnumState.TRAPEZIUS,
            MuscleEnumState.LATERAL_DELTOID,
            MuscleEnumState.ADDUCTORS,
        ).forEach { muscle ->
            MuscleCard(
                value = muscle,
                style = MuscleCardStyle.Selection(isSelected = true, onSelect = {}),
            )
        }
    }
}

@AppPreview
@Composable
private fun MuscleCardUnselectedPreview() {
    PreviewContainer {
        listOf(
            MuscleEnumState.PECTORALIS_MAJOR_CLAVICULAR,
            MuscleEnumState.TRAPEZIUS,
            MuscleEnumState.LATERAL_DELTOID,
            MuscleEnumState.ADDUCTORS,
        ).forEach { muscle ->
            MuscleCard(
                value = muscle,
                style = MuscleCardStyle.Selection(isSelected = false, onSelect = {}),
            )
        }
    }
}
