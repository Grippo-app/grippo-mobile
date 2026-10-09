package com.grippo.design.components.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.constrainHeight
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.ArrowLeft
import com.grippo.design.resources.provider.icons.User

@Immutable
public enum class ToolbarStyle {
    Transparent,
    Default,
}

@Stable
public sealed class Leading {
    @Stable
    public data class Back(val onClick: (() -> Unit)) : Leading()

    @Stable
    public data class Profile(val onClick: (() -> Unit)) : Leading()

    @Immutable
    public data object Nothing : Leading()
}

@Composable
public fun Toolbar(
    modifier: Modifier = Modifier,
    title: String? = null,
    style: ToolbarStyle = ToolbarStyle.Default,
    leading: Leading = Leading.Nothing,
    trailing: (@Composable BoxScope.() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val largeFont = LocalDensity.current.fontScale > AppTokens.dp.screen.largeFontScaleThreshold
    val titleGap = AppTokens.dp.contentPadding.text

    Column(
        modifier = modifier
            .background(
                when (style) {
                    ToolbarStyle.Transparent -> Color.Transparent
                    ToolbarStyle.Default -> AppTokens.colors.background.dialog
                }
            )
            .statusBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        Layout(
            modifier = Modifier
                .padding(horizontal = AppTokens.dp.contentPadding.content)
                .padding(vertical = AppTokens.dp.contentPadding.content)
                .heightIn(min = AppTokens.dp.screen.toolbar.height)
                .fillMaxWidth(),
            content = {
                Box(contentAlignment = Alignment.Center) {
                    when (leading) {
                        is Leading.Back -> Button(
                            content = ButtonContent.Icon(
                                icon = ButtonIcon.Icon(AppTokens.icons.ArrowLeft)
                            ),
                            style = ButtonStyle.Transparent,
                            size = ButtonSize.Small,
                            onClick = leading.onClick,
                        )
                        is Leading.Profile -> Button(
                            content = ButtonContent.Icon(
                                icon = ButtonIcon.Icon(AppTokens.icons.User)
                            ),
                            style = ButtonStyle.Transparent,
                            size = ButtonSize.Small,
                            onClick = leading.onClick,
                        )
                        Leading.Nothing -> Unit
                    }
                }
                Box(contentAlignment = Alignment.Center) {
                    title?.let {
                        Text(
                            text = it,
                            style = AppTokens.typography.h3(),
                            color = AppTokens.colors.text.primary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                Box(contentAlignment = Alignment.Center) {
                    trailing?.invoke(this)
                }
            },
        ) { measurables, constraints ->
            val loose = constraints.copy(minWidth = 0, minHeight = 0)
            val leadingAction = measurables[0].measure(loose)
            val trailingAction = measurables[2].measure(loose)
            val actionWidth = maxOf(leadingAction.width, trailingAction.width)
            val gap = if (actionWidth > 0) titleGap.roundToPx() else 0
            val centeredWidth = (constraints.maxWidth - 2 * (actionWidth + gap)).coerceAtLeast(0)
            val stackTitle = largeFont && actionWidth > 0 &&
                    measurables[1].maxIntrinsicWidth(Constraints.Infinity) > centeredWidth
            val titlePlaceable = measurables[1].measure(
                loose.copy(maxWidth = if (stackTitle) constraints.maxWidth else centeredWidth)
            )
            val actionsHeight = maxOf(leadingAction.height, trailingAction.height)
            val contentHeight = if (stackTitle) {
                actionsHeight + gap + titlePlaceable.height
            } else {
                maxOf(actionsHeight, titlePlaceable.height)
            }
            val height = constraints.constrainHeight(contentHeight)
            layout(constraints.maxWidth, height) {
                val actionsTop = if (stackTitle) 0 else (height - actionsHeight) / 2
                leadingAction.placeRelative(0, actionsTop + (actionsHeight - leadingAction.height) / 2)
                trailingAction.placeRelative(
                    constraints.maxWidth - trailingAction.width,
                    actionsTop + (actionsHeight - trailingAction.height) / 2,
                )
                titlePlaceable.placeRelative(
                    (constraints.maxWidth - titlePlaceable.width) / 2,
                    if (stackTitle) actionsHeight + gap else (height - titlePlaceable.height) / 2,
                )
            }
        }

        content?.invoke(this)
    }
}

@AppPreview
@Composable
private fun ToolbarPreview() {
    PreviewContainer {
        Toolbar(
            title = "Secondary Secondary Secondary Secondary",
            leading = Leading.Nothing,
            style = ToolbarStyle.Transparent
        )
    }
}

@AppPreview
@Composable
private fun ToolbarPreviewContent() {
    PreviewContainer {
        Toolbar(
            title = "Profile",
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppTokens.dp.button.medium.height)
                        .background(AppTokens.colors.background.card)
                )
            }
        )
    }
}
