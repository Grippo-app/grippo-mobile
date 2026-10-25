package com.grippo.shared

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackGestureIcon
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackGestureOverlay
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.grippo.shared.root.RootComponent
import platform.UIKit.UIViewController

// OnFocusBehavior.DoNothing avoids the extra keyboard space of CMP-3621.
@OptIn(ExperimentalComposeApi::class)
public fun rootViewController(
    root: RootComponent,
    backDispatcher: BackDispatcher
): UIViewController {
    return ComposeUIViewController(
        configure = {
            onFocusBehavior = OnFocusBehavior.DoNothing
            // Keep this enabled: false selects the legacy renderer involved in CMP-7598.
            parallelRendering = true
        }
    ) {
        PredictiveBackGestureOverlay(
            backDispatcher = backDispatcher,
            backIcon = { progress, _ ->
                PredictiveBackGestureIcon(
                    imageVector = Icons.Default.ChevronLeft,
                    progress = progress,
                )
            },
            modifier = Modifier.fillMaxSize(),
            content = { root.Render() }
        )
    }
}
