package com.grippo.design.components.cards.selectable

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.components.cards.selectable.internal.CheckSelectableCardLarge
import com.grippo.design.components.cards.selectable.internal.CheckSelectableCardMedium
import com.grippo.design.components.cards.selectable.internal.CheckSelectableCardSmall
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.ExperienceBeginner

@Immutable
public sealed class CheckSelectableCardStyle(
    public open val title: String
) {
    @Immutable
    public data class Small(
        public override val title: String,
    ) : CheckSelectableCardStyle(title)

    @Immutable
    public data class Medium(
        public override val title: String,
        public val description: String?,
    ) : CheckSelectableCardStyle(title)

    @Immutable
    public data class Large(
        public override val title: String,
        public val description: String,
        public val icon: ImageVector,
    ) : CheckSelectableCardStyle(title)
}

@Composable
public fun CheckSelectableCard(
    modifier: Modifier = Modifier,
    style: CheckSelectableCardStyle,
    isSelected: Boolean,
    onSelect: () -> Unit,
    leading: (@Composable () -> Unit)? = null,
) {
    when (style) {
        is CheckSelectableCardStyle.Large -> CheckSelectableCardLarge(
            modifier = modifier,
            style = style,
            isSelected = isSelected,
            onClick = onSelect,
            leading = leading,
        )

        is CheckSelectableCardStyle.Medium -> CheckSelectableCardMedium(
            modifier = modifier,
            style = style,
            isSelected = isSelected,
            onClick = onSelect,
            leading = leading,
        )

        is CheckSelectableCardStyle.Small -> CheckSelectableCardSmall(
            modifier = modifier,
            style = style,
            isSelected = isSelected,
            onClick = onSelect,
            leading = leading,
        )
    }
}

@AppPreview
@Composable
private fun CheckSelectableCardLargePreview() {
    PreviewContainer {
        CheckSelectableCardVariants(
            CheckSelectableCardStyle.Large(
                title = "Початковий рівень",
                description = "Почніть із базових вправ і поступово сформуйте звичку до регулярних тренувань.",
                icon = AppTokens.icons.ExperienceBeginner,
            )
        )
    }
}

@AppPreview
@Composable
private fun CheckSelectableCardMediumPreview() {
    PreviewContainer {
        CheckSelectableCardVariants(
            CheckSelectableCardStyle.Medium(
                title = "Збільшення м’язової маси",
                description = "Зосередьтеся на силі та поступовому прогресі."
            )
        )
    }
}

@AppPreview
@Composable
private fun CheckSelectableCardSmallPreview() {
    PreviewContainer {
        CheckSelectableCardVariants(
            CheckSelectableCardStyle.Small(
                title = "Тренування всього тіла",
            )
        )
    }
}

@Composable
internal fun CheckSelectableCardVariants(style: CheckSelectableCardStyle) {
    val modifier = when (style) {
        is CheckSelectableCardStyle.Small -> Modifier
        else -> Modifier.fillMaxWidth()
    }
    CheckSelectableCard(
        modifier = modifier,
        style = style,
        isSelected = true,
        onSelect = {}
    )
    CheckSelectableCard(
        modifier = modifier,
        style = style,
        isSelected = false,
        onSelect = {}
    )
}
