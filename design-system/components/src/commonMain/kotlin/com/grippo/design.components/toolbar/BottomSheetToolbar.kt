package com.grippo.design.components.toolbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonColorTokens
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.icons.Cancel
import com.grippo.design.resources.provider.icons.NavArrowLeft

@Immutable
public data class BottomSheetToolbarActionButton(
    public val style: ButtonStyle,
    public val size: ButtonSize,
    public val icon: ImageVector,
    public val onClick: () -> Unit,
)

@Composable
public fun BottomSheetToolbar(
    modifier: Modifier = Modifier,
    allowBack: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = modifier
            .padding(horizontal = AppTokens.dp.contentPadding.content)
            .padding(vertical = AppTokens.dp.contentPadding.content)
            .height(AppTokens.dp.bottomSheet.toolbar.height),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = allowBack,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Button(
                content = ButtonContent.Icon(
                    icon = AppTokens.icons.NavArrowLeft,
                ),
                style = ButtonStyle.Custom(
                    enabled = ButtonColorTokens(
                        background1 = Color.Transparent,
                        background2 = Color.Transparent,
                        content = AppTokens.colors.text.primary,
                        border = Color.Transparent,
                        icon = AppTokens.colors.icon.primary,
                    ),
                    disabled = ButtonColorTokens(
                        background1 = Color.Transparent,
                        background2 = Color.Transparent,
                        content = AppTokens.colors.text.disabled,
                        border = Color.Transparent,
                        icon = AppTokens.colors.icon.disabled
                    ),
                ),
                size = ButtonSize.Small,
                onClick = onBack
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            content = ButtonContent.Icon(
                icon = AppTokens.icons.Cancel,
            ),
            style = ButtonStyle.Custom(
                enabled = ButtonColorTokens(
                    background1 = Color.Transparent,
                    background2 = Color.Transparent,
                    content = AppTokens.colors.text.primary,
                    border = Color.Transparent,
                    icon = AppTokens.colors.icon.primary,
                ),
                disabled = ButtonColorTokens(
                    background1 = Color.Transparent,
                    background2 = Color.Transparent,
                    content = AppTokens.colors.text.disabled,
                    border = Color.Transparent,
                    icon = AppTokens.colors.icon.disabled
                ),
            ),
            size = ButtonSize.Small,
            onClick = onClose
        )
    }
}

@AppPreview
@Composable
private fun BottomSheetToolbarPreview() {
    PreviewContainer {
        BottomSheetToolbar(
            onBack = {},
            onClose = {},
            allowBack = true
        )
        BottomSheetToolbar(
            onBack = {},
            onClose = {},
            allowBack = false
        )
    }
}