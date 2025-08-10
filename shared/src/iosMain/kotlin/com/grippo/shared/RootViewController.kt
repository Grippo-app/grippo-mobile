package com.grippo.shared

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackGestureIcon
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.PredictiveBackGestureOverlay
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.grippo.date.utils.DateFormatting
import com.grippo.shared.root.RootComponent
import platform.Foundation.NSLocale
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNotificationName
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification
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
        val languageTag = rememberIosLanguageTag()

        LaunchedEffect(languageTag) {
            DateFormatting.install(languageTag)
        }

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

// Locale-tag receiver from device
@Composable
public fun rememberIosLanguageTag(): String? {
    var tag by remember { mutableStateOf(currentIosLanguageTag()) }

    // Recompute when app comes to foreground (covers locale changes from Settings)
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val observers = mutableListOf<Any>()

        fun observe(name: NSNotificationName) {
            observers += center.addObserverForName(name, `object` = null, queue = null) { _ ->
                tag = currentIosLanguageTag()
            }
        }

        observe(UIApplicationWillEnterForegroundNotification)
        observe(UIApplicationDidBecomeActiveNotification)

        onDispose { observers.forEach { center.removeObserver(it) } }
    }

    return tag
}

private fun currentIosLanguageTag(): String? {
    return NSLocale.currentLocale.localeIdentifier.replace('_', '-')
}