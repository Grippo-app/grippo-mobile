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

/*
* issue: Uncaught Kotlin exception: kotlin.IllegalStateException: Error: Info.plist doesn't have a valid CADisableMinimumFrameDurationOnPhone entry, or has it set to false.
* solving: https://youtrack.jetbrains.com/issue/CMP-6849/Crash-upon-launch-of-iOS-app-with-version-1.7.0-rc01
*
* issue: Odd space for keyboard
* solving: https://youtrack.jetbrains.com/issue/CMP-3621
* */
@OptIn(ExperimentalComposeApi::class)
public fun rootViewController(
    root: RootComponent,
    backDispatcher: BackDispatcher
): UIViewController {
    return ComposeUIViewController(
        configure = {
            onFocusBehavior = OnFocusBehavior.DoNothing
            // Encode Metal/GPU commands on a separate render thread instead of main.
            // Experimental flag introduced in Compose Multiplatform 1.8 — releases the main
            // thread from rendering work, noticeably reduces input lag and scroll jank on iOS.
            // Safe here because the project doesn't use UIKitView interop inside Compose tree
            // (only the outer SwiftUI -> ComposeUIViewController bridge).
            // If the symbol is renamed across versions, alternative is `useSeparateRenderThreadWhenPossible = true`.
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