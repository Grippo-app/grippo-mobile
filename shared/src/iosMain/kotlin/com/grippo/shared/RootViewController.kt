package com.grippo.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.grippo.shared.root.RootComponent
import platform.UIKit.UIViewController

/*
* issue: Uncaught Kotlin exception: kotlin.IllegalStateException: Error: Info.plist doesn't have a valid CADisableMinimumFrameDurationOnPhone entry, or has it set to false.
* solving: https://youtrack.jetbrains.com/issue/CMP-6849/Crash-upon-launch-of-iOS-app-with-version-1.7.0-rc01
* */
public fun rootViewController(root: RootComponent): UIViewController {
    return ComposeUIViewController { root.Render() }
}