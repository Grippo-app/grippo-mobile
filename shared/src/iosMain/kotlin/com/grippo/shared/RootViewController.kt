package com.grippo.shared

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
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
public fun rootViewController(
    root: RootComponent,
    backDispatcher: BackDispatcher
): UIViewController {
    return ComposeUIViewController(
        configure = { this.onFocusBehavior = OnFocusBehavior.DoNothing }
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
        ) {
            root.Render()
        }
    }
}