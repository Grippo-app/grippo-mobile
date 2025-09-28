package com.grippo.design.components.toolbar

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
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
    start: BottomSheetToolbarActionButton?,
    end: BottomSheetToolbarActionButton?,
) {
    Row(
        modifier = modifier
            .padding(horizontal = AppTokens.dp.contentPadding.content)
            .padding(vertical = AppTokens.dp.contentPadding.content)
            .height(AppTokens.dp.bottomSheet.toolbar.height),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        start?.let { btn ->
            Button(
                content = ButtonContent.Icon(
                    icon = btn.icon,
                ),
                style = btn.style,
                size = btn.size,
                onClick = btn.onClick
            )
        }

        Spacer(Modifier.weight(1f))

        end?.let { btn ->
            Button(
                content = ButtonContent.Icon(
                    icon = btn.icon
                ),
                style = btn.style,
                size = btn.size,
                onClick = btn.onClick
            )
        }
    }
}

@AppPreview
@Composable
private fun BottomSheetToolbarPreview() {
    PreviewContainer {
        BottomSheetToolbar(
            start = BottomSheetToolbarActionButton(
                icon = AppTokens.icons.NavArrowLeft,
                style = ButtonStyle.Transparent,
                size = ButtonSize.Small,
                onClick = {}
            ),
            end = BottomSheetToolbarActionButton(
                size = ButtonSize.Small,
                style = ButtonStyle.Transparent,
                icon = AppTokens.icons.NavArrowLeft,
                onClick = {}
            ),
        )

        BottomSheetToolbar(
            start = BottomSheetToolbarActionButton(
                icon = AppTokens.icons.NavArrowLeft,
                style = ButtonStyle.Transparent,
                size = ButtonSize.Small,
                onClick = {}
            ),
            end = null
        )

        BottomSheetToolbar(
            start = null,
            end = null
        )
    }
}