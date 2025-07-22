package com.grippo.design.components.drag.handler

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
public fun DragHandle(backProvider: (() -> Unit)?) {
    AnimatedContent(
        targetState = backProvider,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppTokens.dp.screen.horizontalPadding),
        transitionSpec = {
            fadeIn() + slideInHorizontally { -it / 2 } togetherWith
                    fadeOut() + slideOutHorizontally { -it / 2 }
        },
        label = "DragHandle"
    ) { back ->
        Box(modifier = Modifier.fillMaxWidth()) {
            back?.let {
                Button(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(top = AppTokens.dp.contentPadding.content),
                    text = AppTokens.strings.res(Res.string.back),
                    startIcon = AppTokens.icons.NavArrowLeft,
                    style = ButtonStyle.Transparent,
                    onClick = it
                )
            } ?: Spacer(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .height(AppTokens.dp.screen.verticalPadding)
            )
        }
    }
}

@AppPreview
@Composable
private fun DragHandlePreview() {
    PreviewContainer {
        DragHandle {}
    }
}