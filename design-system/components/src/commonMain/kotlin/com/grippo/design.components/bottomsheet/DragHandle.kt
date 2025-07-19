package com.grippo.design.components.bottomsheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.grippo.design.components.modifiers.scalableClick
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.icons.NavArrowLeft

@Composable
public fun DragHandle(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .height(intrinsicSize = IntrinsicSize.Min),
    ) {
        onBack?.let {
            Icon(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .scalableClick(onClick = onBack)
                    .fillMaxHeight()
                    .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
                imageVector = AppTokens.icons.NavArrowLeft,
                contentDescription = null,
                tint = AppTokens.colors.icon.primary,
            )
        }

        BottomSheetDefaults.DragHandle(
            modifier = Modifier.align(Alignment.Center),
            color = AppTokens.colors.dialog.handle
        )
    }
}

@AppPreview
@Composable
private fun DragHandlePreview() {
    PreviewContainer {
        DragHandle(
            modifier = Modifier.fillMaxWidth(),
            onBack = {}
        )

        DragHandle(
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
