package com.grippo.toolkit.localization.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import platform.Foundation.NSLocale
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationWillEnterForegroundNotification

internal actual object SystemLocale {

    private const val LANGUAGES_KEY: String = "AppleLanguages"

    actual val current: String
        @Composable get() {
            val tag = rememberTag()
            return normalize(tag)
        }

    actual fun current(): String {
        return normalize(preferredTag())
    }

    actual fun apply(tag: String?) {
        if (tag == null) {
            return
        }

        NSUserDefaults.standardUserDefaults.setObject(listOf(tag), forKey = LANGUAGES_KEY)
    }

    @Composable
    private fun rememberTag(): String {
        var tag by remember { mutableStateOf(preferredTag()) }

        DisposableEffect(Unit) {
            val center = NSNotificationCenter.defaultCenter
            val observers = listOfNotNull(
                UIApplicationWillEnterForegroundNotification,
                UIApplicationDidBecomeActiveNotification,
            ).map { name ->
                center.addObserverForName(name, `object` = null, queue = null) { _ ->
                    tag = preferredTag()
                }
            }
            onDispose { observers.forEach { center.removeObserver(it) } }
        }
        return tag
    }

    private fun preferredTag(): String {
        val first = NSLocale.preferredLanguages.firstOrNull() as? String
        return first ?: "en"
    }

    private fun normalize(tag: String): String {
        return tag.replace('_', '-')
    }
}
