package com.grippo.design.components.toolbar

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonContent
import com.grippo.design.components.button.ButtonIcon
import com.grippo.design.components.button.ButtonSize
import com.grippo.design.components.button.ButtonState
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.provider.Res
import com.grippo.design.resources.provider.dialog_back
import com.grippo.design.resources.provider.dialog_close
import com.grippo.design.resources.provider.icons.ArrowLeft
import com.grippo.design.resources.provider.icons.Cancel

@Composable
public fun BottomSheetToolbar(
    modifier: Modifier = Modifier,
    title: String,
    allowBack: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    val backLabel = AppTokens.strings.res(Res.string.dialog_back)
    val closeLabel = AppTokens.strings.res(Res.string.dialog_close)

    Row(
        modifier = modifier
            .padding(horizontal = AppTokens.dp.contentPadding.content)
            .padding(vertical = AppTokens.dp.contentPadding.subContent)
            .heightIn(min = AppTokens.dp.bottomSheet.toolbar.height),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(AppTokens.dp.bottomSheet.toolbar.actionSize), contentAlignment = Alignment.Center) {
            androidx.compose.animation.AnimatedVisibility(
                visible = allowBack,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Button(
                    modifier = Modifier.size(AppTokens.dp.bottomSheet.toolbar.actionSize).semantics { contentDescription = backLabel },
                    content = ButtonContent.Icon(
                        icon = ButtonIcon.Icon(AppTokens.icons.ArrowLeft),
                    ),
                    state = if (allowBack) ButtonState.Enabled else ButtonState.Disabled,
                    style = ButtonStyle.Transparent,
                    size = ButtonSize.Small,
                    onClick = onBack
                )
            }
        }

        Spacer(Modifier.width(AppTokens.dp.contentPadding.text))

        Text(
            modifier = Modifier.weight(1f).semantics { heading() },
            text = title,
            style = AppTokens.typography.h3(),
            color = AppTokens.colors.text.primary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(Modifier.width(AppTokens.dp.contentPadding.text))

        Button(
            modifier = Modifier.size(AppTokens.dp.bottomSheet.toolbar.actionSize).semantics { contentDescription = closeLabel },
            content = ButtonContent.Icon(
                icon = ButtonIcon.Icon(AppTokens.icons.Cancel),
            ),
            style = ButtonStyle.Tertiary,
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
            title = "Select exercise",
            onBack = {},
            onClose = {},
            allowBack = true
        )
        BottomSheetToolbar(
            title = "Choose your primary training goal",
            onBack = {},
            onClose = {},
            allowBack = false
        )
    }
}
