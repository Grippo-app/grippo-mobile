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
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.back
import com.grippo.design.resources.provider.close
import com.grippo.design.resources.provider.icons.NavArrowLeft

@Immutable
public data class BottomSheetToolbarActionButton(
    public val text: String,
    public val style: ButtonStyle,
    public val startIcon: ImageVector?,
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
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding)
            .padding(top = AppTokens.dp.screen.verticalPadding)
            .padding(bottom = AppTokens.dp.contentPadding.subContent)
            .height(AppTokens.dp.bottomSheet.toolbar.height),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        start?.let { btn ->
            Button(
                content = ButtonContent.Text(
                    text = btn.text,
                    startIcon = btn.startIcon,
                ),
                style = btn.style,
                onClick = btn.onClick
            )
        }

        Spacer(Modifier.weight(1f))

        end?.let { btn ->
            Button(
                content = ButtonContent.Text(
                    text = btn.text,
                    startIcon = btn.startIcon,
                ),
                style = btn.style,
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
                text = AppTokens.strings.res(Res.string.back),
                startIcon = AppTokens.icons.NavArrowLeft,
                style = ButtonStyle.Transparent,
                onClick = {}
            ),
            end = BottomSheetToolbarActionButton(
                text = AppTokens.strings.res(Res.string.close),
                startIcon = null,
                style = ButtonStyle.Transparent,
                onClick = {}
            ),
        )

        BottomSheetToolbar(
            start = BottomSheetToolbarActionButton(
                text = AppTokens.strings.res(Res.string.back),
                startIcon = AppTokens.icons.NavArrowLeft,
                style = ButtonStyle.Transparent,
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