package com.grippo.design.components.bottom.sheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.grippo.design.components.button.Button
import com.grippo.design.components.button.ButtonStyle
import com.grippo.design.core.AppTokens
import com.grippo.design.preview.AppPreview
import com.grippo.design.preview.PreviewContainer
import com.grippo.design.resources.Res
import com.grippo.design.resources.back
import com.grippo.design.resources.icons.NavArrowLeft

@Composable
public fun BottomSheetBackButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            color = AppTokens.colors.divider.default
        )

        Spacer(Modifier.height(AppTokens.dp.screen.verticalPadding))

        Button(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
            text = AppTokens.strings.res(Res.string.back),
            startIcon = AppTokens.icons.NavArrowLeft,
            style = ButtonStyle.Transparent,
            onClick = onClick
        )

        Spacer(Modifier.height(AppTokens.dp.screen.verticalPadding))
    }
}

@AppPreview
@Composable
private fun BottomSheetBackButtonPreview() {
    PreviewContainer {
        BottomSheetBackButton(onClick = {})
    }
}