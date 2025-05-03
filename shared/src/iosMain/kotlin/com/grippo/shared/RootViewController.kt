package com.grippo.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.grippo.shared.root.RootComponent
import platform.UIKit.UIViewController

public fun rootViewController(root: RootComponent): UIViewController =
    ComposeUIViewController {
        root.Render()
    }