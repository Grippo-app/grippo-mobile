package com.grippo.design.components.tooltip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.grippo.design.components.tooltip.internal.TooltipBubble
import com.grippo.design.components.tooltip.internal.resolveTooltipColors
import com.grippo.design.components.tooltip.internal.tooltipPositionProvider
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Warning

@Immutable
public sealed interface TooltipVariant {
    @Immutable
    public data object Default : TooltipVariant

    @Immutable
    public data object Success : TooltipVariant

    @Immutable
    public data object Error : TooltipVariant

    @Immutable
    public data object Warning : TooltipVariant

    @Immutable
    public data object Info : TooltipVariant
}

@Immutable
public sealed interface TooltipPlacement {
    @Immutable
    public data object Top : TooltipPlacement

    @Immutable
    public data object Bottom : TooltipPlacement

    @Immutable
    public data object Start : TooltipPlacement

    @Immutable
    public data object End : TooltipPlacement
}

@Immutable
public sealed interface TooltipContent {
    @Immutable
    public data class Simple(
        val text: String,
        val icon: ImageVector? = null,
    ) : TooltipContent

    @Immutable
    public data class Rich(
        val title: String,
        val subtitle: String,
        val icon: ImageVector? = null,
    ) : TooltipContent
}

@Composable
public fun Tooltip(
    modifier: Modifier = Modifier,
    state: TooltipState = rememberTooltipState(),
    tooltipContent: TooltipContent,
    placement: TooltipPlacement = TooltipPlacement.Top,
    variant: TooltipVariant = TooltipVariant.Default,
    content: @Composable () -> Unit,
) {
    val colors = resolveTooltipColors(variant)
    val positionProvider = remember(placement) { tooltipPositionProvider(placement) }

    TooltipBox(
        modifier = modifier,
        positionProvider = positionProvider,
        tooltip = {
            TooltipBubble(
                content = tooltipContent,
                colors = colors,
                placement = placement,
            )
        },
        state = state,
        focusable = true,
        enableUserInput = false,
        content = content,
    )
}

@AppPreview
@Composable
private fun TooltipSimplePreview() {
    PreviewContainer {
        val state = rememberTooltipState(isPersistent = true)

        LaunchedEffect(Unit) {
            state.show()
        }

        Tooltip(
            state = state,
            tooltipContent = TooltipContent.Simple(
                text = "This is a hint",
                icon = AppTokens.icons.Warning,
            ),
            placement = TooltipPlacement.Top,
            variant = TooltipVariant.Default,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        AppTokens.colors.background.card,
                        RoundedCornerShape(AppTokens.dp.tooltip.radius),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppTokens.icons.Warning,
                    contentDescription = null,
                    tint = AppTokens.colors.icon.primary,
                )
            }
        }
    }
}

@AppPreview
@Composable
private fun TooltipVariantsPreview() {
    PreviewContainer {
        val variants = listOf(
            TooltipVariant.Default to "Default",
            TooltipVariant.Success to "Success",
            TooltipVariant.Error to "Error",
            TooltipVariant.Warning to "Warning",
            TooltipVariant.Info to "Info",
        )
        variants.forEach { (variant, label) ->
            val state = rememberTooltipState(isPersistent = true)
            LaunchedEffect(Unit) { state.show() }
            Tooltip(
                state = state,
                tooltipContent = TooltipContent.Simple(
                    text = label,
                    icon = AppTokens.icons.Warning,
                ),
                placement = TooltipPlacement.Top,
                variant = variant,
            ) {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@AppPreview
@Composable
private fun TooltipRichPreview() {
    PreviewContainer {
        val state = rememberTooltipState(isPersistent = true)
        LaunchedEffect(Unit) { state.show() }
        Tooltip(
            state = state,
            tooltipContent = TooltipContent.Rich(
                title = "Personal record!",
                subtitle = "You lifted 120 kg — your best ever.",
                icon = AppTokens.icons.Warning,
            ),
            placement = TooltipPlacement.Bottom,
            variant = TooltipVariant.Success,
        ) {
            Spacer(Modifier.height(8.dp))
        }
    }
}

@AppPreview
@Composable
private fun TooltipPlacementsPreview() {
    PreviewContainer {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppTokens.dp.contentPadding.block),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            listOf(
                TooltipPlacement.Top to "Top placement",
                TooltipPlacement.Bottom to "Bottom placement",
                TooltipPlacement.Start to "Start",
                TooltipPlacement.End to "End placement",
            ).forEach { (placement, label) ->
                val state = rememberTooltipState(isPersistent = true)
                LaunchedEffect(Unit) { state.show() }
                Tooltip(
                    state = state,
                    tooltipContent = TooltipContent.Simple(label),
                    placement = placement,
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(
                                AppTokens.colors.background.card,
                                RoundedCornerShape(AppTokens.dp.tooltip.radius),
                            )
                    )
                }
            }
        }
    }
}
