package com.grippo.design.components.equipment

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorGroup
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.grippo.core.state.equipments.EquipmentEnumState
import com.grippo.core.state.equipments.EquipmentState
import com.grippo.design.components.cards.selectable.ToggleSelectableCard
import com.grippo.design.components.cards.selectable.ToggleSelectableCardStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer

@Stable
public sealed interface EquipmentCardStyle {
    @Immutable
    public data object Compact : EquipmentCardStyle

    @Immutable
    public data object Inline : EquipmentCardStyle

    @Immutable
    public data object Detail : EquipmentCardStyle

    @Stable
    public data class Selection(
        public val isSelected: Boolean,
        public val onSelect: () -> Unit,
    ) : EquipmentCardStyle
}

@Composable
public fun EquipmentCard(
    modifier: Modifier = Modifier,
    value: EquipmentState,
    style: EquipmentCardStyle,
) {
    when (style) {
        EquipmentCardStyle.Compact -> EquipmentCardCompact(modifier = modifier, value = value)
        EquipmentCardStyle.Inline -> EquipmentCardInline(modifier = modifier, value = value)
        EquipmentCardStyle.Detail -> EquipmentCardDetail(modifier = modifier, value = value)
        is EquipmentCardStyle.Selection -> EquipmentCardSelection(
            modifier = modifier,
            value = value,
            style = style,
        )
    }
}

@Composable
private fun EquipmentCardCompact(
    modifier: Modifier,
    value: EquipmentState,
) {
    val title = value.type.title().text()
    val sourceIcon = value.type.icon()
    val highlight = AppTokens.colors.example.equipmentCompactHighlight
    val icon = remember(sourceIcon, highlight) {
        compactEquipmentImage(source = sourceIcon, highlight = highlight)
    }
    val opticalPadding = when (value.type) {
        EquipmentEnumState.DUMBBELLS,
        EquipmentEnumState.KETTLEBELLS,
        EquipmentEnumState.BARBELL,
        EquipmentEnumState.EZ_BAR,
        EquipmentEnumState.TRAP_BAR -> AppTokens.dp.equipmentCard.compactFreeWeightPadding

        else -> 0.dp
    }

    Image(
        modifier = modifier
            .size(AppTokens.dp.equipmentCard.compactIcon)
            .semantics {
                text = AnnotatedString(title)
            }
            .padding(opticalPadding),
        imageVector = icon,
        contentDescription = null,
        contentScale = ContentScale.Fit,
    )
}

private fun compactEquipmentImage(source: ImageVector, highlight: Color): ImageVector {
    if (highlight == Color.White) {
        return source
    }

    val builder = ImageVector.Builder(
        name = source.name,
        defaultWidth = source.defaultWidth,
        defaultHeight = source.defaultHeight,
        viewportWidth = source.viewportWidth,
        viewportHeight = source.viewportHeight,
        tintColor = source.tintColor,
        tintBlendMode = source.tintBlendMode,
        autoMirror = source.autoMirror,
    )
    appendEquipmentGroup(builder = builder, group = source.root, highlight = highlight)
    return builder.build()
}

private fun appendEquipmentGroup(
    builder: ImageVector.Builder,
    group: VectorGroup,
    highlight: Color,
) {
    builder.addGroup(
        name = group.name,
        rotate = group.rotation,
        pivotX = group.pivotX,
        pivotY = group.pivotY,
        scaleX = group.scaleX,
        scaleY = group.scaleY,
        translationX = group.translationX,
        translationY = group.translationY,
        clipPathData = group.clipPathData,
    )
    group.forEach { node ->
        when (node) {
            is VectorGroup -> appendEquipmentGroup(builder = builder, group = node, highlight = highlight)
            is VectorPath -> builder.addPath(
                pathData = node.pathData,
                pathFillType = node.pathFillType,
                name = node.name,
                fill = equipmentHighlightBrush(brush = node.fill, highlight = highlight),
                fillAlpha = node.fillAlpha,
                stroke = equipmentHighlightBrush(brush = node.stroke, highlight = highlight),
                strokeAlpha = node.strokeAlpha,
                strokeLineWidth = node.strokeLineWidth,
                strokeLineCap = node.strokeLineCap,
                strokeLineJoin = node.strokeLineJoin,
                strokeLineMiter = node.strokeLineMiter,
                trimPathStart = node.trimPathStart,
                trimPathEnd = node.trimPathEnd,
                trimPathOffset = node.trimPathOffset,
            )
        }
    }
    builder.clearGroup()
}

private fun equipmentHighlightBrush(brush: Brush?, highlight: Color): Brush? {
    if (brush !is SolidColor) {
        return brush
    }
    val color = brush.value
    val softened = color.copy(
        red = minOf(color.red, highlight.red),
        green = minOf(color.green, highlight.green),
        blue = minOf(color.blue, highlight.blue),
    )
    return if (softened == color) {
        brush
    } else {
        SolidColor(softened)
    }
}

@Composable
private fun EquipmentCardInline(
    modifier: Modifier,
    value: EquipmentState,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
    ) {
        Image(
            modifier = Modifier.size(AppTokens.dp.equipmentCard.inlineIcon),
            imageVector = value.type.icon(),
            contentDescription = null,
        )

        Text(
            text = value.type.title().text(),
            style = AppTokens.typography.b13Med(),
            color = AppTokens.colors.text.secondary,
        )
    }
}

@Composable
private fun EquipmentCardDetail(
    modifier: Modifier,
    value: EquipmentState,
) {
    Column(
        modifier = modifier.width(AppTokens.dp.equipmentCard.width),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.content),
    ) {
        Image(
            modifier = Modifier.size(AppTokens.dp.equipmentCard.icon),
            imageVector = value.type.icon(),
            contentDescription = null,
        )

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = value.type.title().text(),
            textAlign = TextAlign.Center,
            style = AppTokens.typography.b14Semi(),
            color = AppTokens.colors.text.primary,
        )
    }
}

@Composable
private fun EquipmentCardSelection(
    modifier: Modifier,
    value: EquipmentState,
    style: EquipmentCardStyle.Selection,
) {
    val icon = value.type.icon()

    ToggleSelectableCard(
        modifier = modifier.fillMaxWidth(),
        style = ToggleSelectableCardStyle.Medium(
            title = value.type.title().text(),
            icon = icon,
        ),
        isSelected = style.isSelected,
        onSelect = style.onSelect,
        leading = {
            Image(
                modifier = Modifier.size(AppTokens.dp.toggleSelectableCard.medium.icon),
                imageVector = icon,
                contentDescription = null,
            )
        },
    )
}

@AppPreview
@Composable
private fun EquipmentCardCompactPreview() {
    PreviewContainer {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
            verticalArrangement = Arrangement.spacedBy(AppTokens.dp.contentPadding.text),
        ) {
            val featured = listOf(
                EquipmentEnumState.LANDMINE,
                EquipmentEnumState.VERTICAL_LEG_PRESS,
                EquipmentEnumState.LEG_PRESS_MACHINE,
                EquipmentEnumState.PLATE_LOADED_DECLINE_CHEST_PRESS,
                EquipmentEnumState.PLATE_LOADED_INCLINE_CHEST_PRESS,
                EquipmentEnumState.CHEST_PRESS_MACHINES,
                EquipmentEnumState.PLATE_LOADED_SEATED_DIP,
            )
            val types = featured + EquipmentEnumState.entries.filterNot { it in featured }
            types.forEach { type ->
                EquipmentCard(
                    value = EquipmentState(id = type.name, type = type),
                    style = EquipmentCardStyle.Compact,
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun EquipmentCardInlinePreview() {
    PreviewContainer {
        listOf(
            EquipmentEnumState.LANDMINE,
            EquipmentEnumState.VERTICAL_LEG_PRESS,
            EquipmentEnumState.LEG_PRESS_MACHINE,
            EquipmentEnumState.PLATE_LOADED_DECLINE_CHEST_PRESS,
            EquipmentEnumState.PLATE_LOADED_INCLINE_CHEST_PRESS,
            EquipmentEnumState.CHEST_PRESS_MACHINES,
            EquipmentEnumState.ANKLE_CUFF,
            EquipmentEnumState.CORD_HANDLES,
            EquipmentEnumState.PLATE_LOADED_SEATED_DIP,
            EquipmentEnumState.TRICEPS_MACHINES,
            EquipmentEnumState.BELT_SQUAT,
            EquipmentEnumState.PLATE_LOADED_ROW,
            EquipmentEnumState.ASSISTED_PULL_UP_DIP,
            EquipmentEnumState.KETTLEBELLS,
            EquipmentEnumState.BUTTERFLY_FOREARM,
            EquipmentEnumState.BARBELL,
            EquipmentEnumState.SMITH_MACHINES,
            EquipmentEnumState.ROPE,
            EquipmentEnumState.DECLINE_BENCH_WITH_RACK,
        ).forEach { type ->
            EquipmentCard(
                value = EquipmentState(id = type.name, type = type),
                style = EquipmentCardStyle.Inline,
            )
        }
    }
}

@AppPreview
@Composable
private fun EquipmentCardDetailPreview() {
    PreviewContainer {
        listOf(
            EquipmentEnumState.LANDMINE,
            EquipmentEnumState.VERTICAL_LEG_PRESS,
            EquipmentEnumState.LEG_PRESS_MACHINE,
            EquipmentEnumState.PLATE_LOADED_DECLINE_CHEST_PRESS,
            EquipmentEnumState.PLATE_LOADED_INCLINE_CHEST_PRESS,
            EquipmentEnumState.CHEST_PRESS_MACHINES,
            EquipmentEnumState.ANKLE_CUFF,
            EquipmentEnumState.CORD_HANDLES,
            EquipmentEnumState.PLATE_LOADED_SEATED_DIP,
            EquipmentEnumState.TRICEPS_MACHINES,
            EquipmentEnumState.BELT_SQUAT,
            EquipmentEnumState.PLATE_LOADED_ROW,
            EquipmentEnumState.ASSISTED_PULL_UP_DIP,
            EquipmentEnumState.KETTLEBELLS,
            EquipmentEnumState.BUTTERFLY_FOREARM,
            EquipmentEnumState.BARBELL,
            EquipmentEnumState.SMITH_MACHINES,
            EquipmentEnumState.ROPE,
            EquipmentEnumState.DECLINE_BENCH_WITH_RACK,
        ).forEach { type ->
            EquipmentCard(
                value = EquipmentState(id = type.name, type = type),
                style = EquipmentCardStyle.Detail,
            )
        }
    }
}

@AppPreview
@Composable
private fun EquipmentCardSelectedPreview() {
    PreviewContainer {
        listOf(
            EquipmentEnumState.LANDMINE,
            EquipmentEnumState.VERTICAL_LEG_PRESS,
            EquipmentEnumState.LEG_PRESS_MACHINE,
            EquipmentEnumState.PLATE_LOADED_DECLINE_CHEST_PRESS,
            EquipmentEnumState.PLATE_LOADED_INCLINE_CHEST_PRESS,
            EquipmentEnumState.CHEST_PRESS_MACHINES,
            EquipmentEnumState.ANKLE_CUFF,
            EquipmentEnumState.CORD_HANDLES,
            EquipmentEnumState.PLATE_LOADED_SEATED_DIP,
            EquipmentEnumState.TRICEPS_MACHINES,
            EquipmentEnumState.BELT_SQUAT,
            EquipmentEnumState.PLATE_LOADED_ROW,
            EquipmentEnumState.ASSISTED_PULL_UP_DIP,
            EquipmentEnumState.KETTLEBELLS,
            EquipmentEnumState.BUTTERFLY_FOREARM,
            EquipmentEnumState.ROPE,
            EquipmentEnumState.SMITH_MACHINES,
            EquipmentEnumState.DECLINE_BENCH_WITH_RACK,
        ).forEach { type ->
            EquipmentCard(
                value = EquipmentState(id = type.name, type = type),
                style = EquipmentCardStyle.Selection(isSelected = true, onSelect = {}),
            )
        }
    }
}

@AppPreview
@Composable
private fun EquipmentCardUnselectedPreview() {
    PreviewContainer {
        listOf(
            EquipmentEnumState.LANDMINE,
            EquipmentEnumState.VERTICAL_LEG_PRESS,
            EquipmentEnumState.LEG_PRESS_MACHINE,
            EquipmentEnumState.PLATE_LOADED_DECLINE_CHEST_PRESS,
            EquipmentEnumState.PLATE_LOADED_INCLINE_CHEST_PRESS,
            EquipmentEnumState.CHEST_PRESS_MACHINES,
            EquipmentEnumState.ANKLE_CUFF,
            EquipmentEnumState.CORD_HANDLES,
            EquipmentEnumState.PLATE_LOADED_SEATED_DIP,
            EquipmentEnumState.TRICEPS_MACHINES,
            EquipmentEnumState.BELT_SQUAT,
            EquipmentEnumState.PLATE_LOADED_ROW,
            EquipmentEnumState.ASSISTED_PULL_UP_DIP,
            EquipmentEnumState.KETTLEBELLS,
            EquipmentEnumState.BUTTERFLY_FOREARM,
            EquipmentEnumState.ROPE,
            EquipmentEnumState.SMITH_MACHINES,
            EquipmentEnumState.DECLINE_BENCH_WITH_RACK,
        ).forEach { type ->
            EquipmentCard(
                value = EquipmentState(id = type.name, type = type),
                style = EquipmentCardStyle.Selection(isSelected = false, onSelect = {}),
            )
        }
    }
}
